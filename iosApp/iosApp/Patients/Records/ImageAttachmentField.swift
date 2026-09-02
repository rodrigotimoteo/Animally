import Foundation
import PhotosUI
import Shared
import SwiftUI
import UIKit

/// Single record-image reference used by the iOS presentation layer.
///
/// The persisted attachment list and its parsing remain in Kotlin. This
/// value only carries the already-decoded path and display name needed by the
/// image gallery.
private struct LocalImageReference: Identifiable, Hashable {
    let path: String
    let fileName: String

    var id: String { path }

    init(path: String, fileName: String? = nil) {
        self.path = path
        self.fileName = fileName ?? Self.fallbackFileName(for: path)
    }

    private static func fallbackFileName(for path: String) -> String {
        path
            .split(whereSeparator: { $0 == "/" || $0 == "\\" })
            .last
            .map(String.init)
            ?? "Image"
    }
}

/// Adapts already-loaded picker data to the suspendable Kotlin file contract.
/// The Kotlin view model remains responsible for storage and form state.
private final class KotlinByteArrayReader: NSObject, KotlinSuspendFunction0 {
    private let data: Data

    init(data: Data) {
        self.data = data
    }

    func invoke(completionHandler: @escaping (Any?, Error?) -> Void) {
        let bytes = KotlinByteArray(size: Int32(data.count))
        for (index, byte) in data.enumerated() {
            bytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        completionHandler(bytes, nil)
    }
}

/// Image attachment editor used by the imaging and ultrasound forms.
///
/// Paths are still persisted through the Kotlin form state. Swift owns only
/// the PhotosPicker bridge and the visual preview of those paths.
struct ImageAttachmentField: View {
    let title: String
    /// Current comma-separated local file paths, or `nil` when unset.
    let imagePath: String?
    let onFilesPicked: ([PickedFile]) -> Void
    let onRemove: (String) -> Void

    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var isProcessing = false
    @State private var errorMessage: String?

    private var attachedImages: [LocalImageReference] {
        imagePaths.map { LocalImageReference(path: $0) }
    }

    private var imagePaths: [String] {
        (imagePath ?? "")
            .split(separator: ",")
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Label(title, systemImage: "photo.on.rectangle.angled")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)

                Spacer()

                if !attachedImages.isEmpty {
                    Text(attachmentCountLabel)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                }
            }

            if attachedImages.isEmpty {
                emptyState
            } else {
                LocalImageGallery(
                    attachments: attachedImages,
                    onRemove: removeImage,
                )
            }

            HStack(spacing: 12) {
                PhotosPicker(
                    selection: $pickerItems,
                    maxSelectionCount: 10,
                    matching: .images,
                ) {
                    Label(
                        attachedImages.isEmpty ? "Add photos" : "Add more",
                        systemImage: "plus",
                    )
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.forestGreen)
                }
                .buttonStyle(.plain)
                .disabled(isProcessing)

                if isProcessing {
                    ProgressView()
                        .controlSize(.small)
                        .accessibilityLabel("Adding photos")
                }
            }

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .font(.caption)
                    .foregroundStyle(Theme.amber)
            }
        }
        .padding(.vertical, 2)
        .onChange(of: pickerItems) { _, newItems in
            guard !newItems.isEmpty else { return }
            Task { await importImages(newItems) }
        }
    }

    private var attachmentCountLabel: String {
        let count = attachedImages.count
        return "\(count) image\(count == 1 ? "" : "s")"
    }

    private var emptyState: some View {
        HStack(spacing: 10) {
            Image(systemName: "photo")
                .foregroundStyle(Theme.textTertiary)
            Text("No images attached yet")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func removeImage(_ attachment: LocalImageReference) {
        onRemove(attachment.path)
    }

    @MainActor
    private func importImages(_ items: [PhotosPickerItem]) async {
        isProcessing = true
        errorMessage = nil

        var pickedFiles: [PickedFile] = []
        var failedCount = 0

        for item in items {
            guard
                let data = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: data),
                let jpegData = image.jpegData(compressionQuality: 0.85)
            else {
                failedCount += 1
                continue
            }

            pickedFiles.append(
                PickedFile(
                    name: "image-\(UUID().uuidString).jpg",
                    readBytes: KotlinByteArrayReader(data: jpegData),
                ),
            )
        }

        if !pickedFiles.isEmpty {
            onFilesPicked(pickedFiles)
        }
        if failedCount > 0 {
            errorMessage = failedCount == 1
                ? "One photo could not be added."
                : "\(failedCount) photos could not be added."
        }

        pickerItems = []
        isProcessing = false
    }
}

/// Read-only gallery for images emitted by the Kotlin record-detail state.
struct RecordDetailAttachmentGallery: View {
    let attachments: [RecordDetailAttachment]

    private var imageReferences: [LocalImageReference] {
        attachments.map { LocalImageReference(path: $0.path, fileName: $0.fileName) }
    }

    var body: some View {
        LocalImageGallery(attachments: imageReferences)
    }
}

/// Horizontally scrolling thumbnail strip shared by edit and read-only views.
private struct LocalImageGallery: View {
    let attachments: [LocalImageReference]
    var onRemove: ((LocalImageReference) -> Void)? = nil

    @State private var selectedIndex: Int?

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 12) {
                ForEach(attachments.indices, id: \.self) { index in
                    let attachment = attachments[index]
                    VStack(spacing: 5) {
                        Button {
                            selectedIndex = index
                        } label: {
                            LocalImageThumbnail(attachment: attachment)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("View image \(index + 1) of \(attachments.count): \(attachment.fileName)")
                        .accessibilityHint("Opens the image full screen")
                        .accessibilityIdentifier("Record image \(index + 1)")

                        Text(attachment.fileName)
                            .font(.caption2)
                            .foregroundStyle(Theme.textSecondary)
                            .lineLimit(1)
                            .frame(width: 96)

                        if let onRemove {
                            Button {
                                onRemove(attachment)
                            } label: {
                                Label("Remove", systemImage: "trash")
                                    .font(.caption2.weight(.medium))
                                    .foregroundStyle(.red)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Remove image \(index + 1)")
                        }
                    }
                    .frame(width: 96)
                }
            }
            .padding(.vertical, 2)
        }
        .frame(height: onRemove == nil ? 122 : 154)
        .fullScreenCover(
            isPresented: Binding(
                get: { selectedIndex != nil },
                set: { isPresented in
                    if !isPresented { selectedIndex = nil }
                },
            ),
        ) {
            if let selectedIndex {
                LocalImageViewer(
                    attachments: attachments,
                    initialIndex: selectedIndex,
                )
            }
        }
    }
}

private struct LocalImageThumbnail: View {
    let attachment: LocalImageReference

    var body: some View {
        LocalImageContent(
            attachment: attachment,
            contentMode: .fill,
        )
        .frame(width: 96, height: 96)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay {
            RoundedRectangle(cornerRadius: 10)
                .stroke(Theme.textTertiary.opacity(0.22), lineWidth: 1)
        }
    }
}

private struct LocalImageViewer: View {
    let attachments: [LocalImageReference]
    let initialIndex: Int

    @Environment(\.dismiss) private var dismiss
    @State private var selectedIndex: Int

    init(attachments: [LocalImageReference], initialIndex: Int) {
        self.attachments = attachments
        self.initialIndex = initialIndex
        _selectedIndex = State(initialValue: initialIndex)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black
                    .ignoresSafeArea()

                TabView(selection: $selectedIndex) {
                    ForEach(attachments.indices, id: \.self) { index in
                        ZoomableLocalImageView(attachment: attachments[index])
                            .tag(index)
                            .padding(.horizontal, 8)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .navigationTitle(viewerTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("\(selectedIndex + 1) of \(attachments.count)")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.secondary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .accessibilityLabel("Close image viewer")
                }
            }
            .toolbarBackground(.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    private var viewerTitle: String {
        guard attachments.indices.contains(selectedIndex) else { return "Images" }
        return attachments[selectedIndex].fileName
    }
}

private struct ZoomableLocalImageView: View {
    let attachment: LocalImageReference

    @State private var scale: CGFloat = 1
    @State private var gestureScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var gestureOffset: CGSize = .zero

    var body: some View {
        LocalImageContent(
            attachment: attachment,
            contentMode: .fit,
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .scaleEffect(scale * gestureScale)
        .offset(
            x: offset.width + gestureOffset.width,
            y: offset.height + gestureOffset.height,
        )
        .contentShape(Rectangle())
        .gesture(magnificationGesture)
        .simultaneousGesture(dragGesture)
        .onTapGesture(count: 2, perform: toggleZoom)
        .accessibilityHint("Pinch to zoom and drag to pan. Double-tap to reset or zoom.")
    }

    private var magnificationGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                gestureScale = value
            }
            .onEnded { value in
                scale = min(max(scale * value, 1), 4)
                gestureScale = 1
                if scale == 1 { offset = .zero }
            }
    }

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                guard scale > 1 else { return }
                gestureOffset = value.translation
            }
            .onEnded { value in
                guard scale > 1 else {
                    gestureOffset = .zero
                    return
                }
                offset.width += value.translation.width
                offset.height += value.translation.height
                gestureOffset = .zero
            }
    }

    private func toggleZoom() {
        withAnimation(.easeInOut(duration: 0.2)) {
            if scale > 1 {
                scale = 1
                offset = .zero
            } else {
                scale = 2
            }
        }
    }
}

private struct LocalImageContent: View {
    let attachment: LocalImageReference
    let contentMode: ContentMode

    @State private var image: UIImage?
    @State private var isLoading = true
    @State private var failed = false

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
                    .accessibilityHidden(true)
            } else if isLoading {
                ProgressView()
                    .tint(.secondary)
                    .accessibilityLabel("Loading image")
            } else if failed {
                VStack(spacing: 5) {
                    Image(systemName: "photo")
                        .font(.title3)
                    Text("Unavailable")
                        .font(.caption2)
                }
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .accessibilityLabel("Image unavailable: \(attachment.fileName)")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task(id: attachment.path) {
            await loadImage()
        }
    }

    private func loadImage() async {
        isLoading = true
        failed = false
        image = nil

        guard let url = LocalAttachmentURL.resolvedURL(for: attachment.path) else {
            isLoading = false
            failed = true
            return
        }

        let data = await Task.detached(priority: .userInitiated) {
            try? Data(contentsOf: url, options: [.mappedIfSafe])
        }.value

        guard !Task.isCancelled else { return }

        guard let data, let decodedImage = UIImage(data: data) else {
            isLoading = false
            failed = true
            return
        }

        image = decodedImage
        isLoading = false
    }
}

/// Resolves both current absolute paths and legacy `file://` paths. If an
/// app reinstall changed the sandbox container, the attachment's file name is
/// used as a safe fallback in the current app-owned attachments directory.
/// Paths outside that directory are never read.
private enum LocalAttachmentURL {
    static func resolvedURL(for storedPath: String) -> URL? {
        let trimmedPath = storedPath.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPath.isEmpty else { return nil }

        guard let storedURL = storedURL(for: trimmedPath) else { return nil }
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let attachmentsURL = documentsURL.appendingPathComponent("attachments", isDirectory: true)

        if let currentURL = containedExistingFile(storedURL, under: attachmentsURL) {
            return currentURL
        }

        guard !storedURL.lastPathComponent.isEmpty else { return nil }
        let fallbackURL = attachmentsURL.appendingPathComponent(storedURL.lastPathComponent, isDirectory: false)
        return containedExistingFile(fallbackURL, under: attachmentsURL)
    }

    private static func storedURL(for path: String) -> URL? {
        if let url = URL(string: path), url.scheme != nil {
            return url.isFileURL ? url : nil
        }
        return URL(fileURLWithPath: path)
    }

    private static func containedExistingFile(_ candidate: URL, under root: URL) -> URL? {
        guard candidate.isFileURL else { return nil }

        let canonicalRoot = root.resolvingSymlinksInPath().standardizedFileURL.path
        let canonicalCandidate = candidate.resolvingSymlinksInPath().standardizedFileURL
        let candidatePath = canonicalCandidate.path
        guard candidatePath == canonicalRoot || candidatePath.hasPrefix(canonicalRoot + "/") else {
            return nil
        }
        var isDirectory = ObjCBool(false)
        guard
            FileManager.default.fileExists(atPath: candidatePath, isDirectory: &isDirectory),
            !isDirectory.boolValue
        else {
            return nil
        }
        return canonicalCandidate
    }
}
