import PhotosUI
import SwiftUI
import UIKit

/// Single-image attachment field backed by `PhotosPicker`.
///
/// Copies the picked image into app-local storage
/// (`Documents/attachments/<uuid>.jpg`) and reports the local file path so it
/// can be stored in the record's `imageUris` field. Shows a thumbnail of the
/// attached image with a remove affordance.
struct ImageAttachmentField: View {
    let title: String
    /// Current local file path of the attached image, or empty when unset.
    let imagePath: String?
    var onChange: (String) -> Void

    @State private var pickerItem: PhotosPickerItem?
    @State private var thumbnail: UIImage?

    var body: some View {
        HStack(spacing: 12) {
            if let thumbnail {
                Image(uiImage: thumbnail)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .accessibilityLabel("Attached image")
            } else {
                Image(systemName: "photo.on.rectangle.angled")
                    .font(.title2)
                    .foregroundStyle(Theme.textTertiary)
                    .frame(width: 56, height: 56)
                    .background(Theme.surfaceElevated)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }

            VStack(alignment: .leading, spacing: 4) {
                if thumbnail != nil {
                    Text("Image attached")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.textPrimary)
                } else {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.textSecondary)
                }

                HStack(spacing: 12) {
                    PhotosPicker(selection: $pickerItem, matching: .images) {
                        Text(thumbnail == nil ? "Choose Photo" : "Replace")
                            .font(.subheadline)
                            .foregroundStyle(Theme.forestGreen)
                    }
                    .buttonStyle(.plain)

                    if thumbnail != nil {
                        Button {
                            onChange("")
                            thumbnail = nil
                        } label: {
                            Text("Remove")
                                .font(.subheadline)
                                .foregroundStyle(.red)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Spacer()
        }
        .padding(.vertical, 2)
        .onChange(of: pickerItem) { _, newItem in
            guard let newItem else { return }
            Task { await attach(newItem) }
        }
        .onAppear {
            loadThumbnail()
        }
    }

    private func loadThumbnail() {
        guard let imagePath, !imagePath.isEmpty else {
            thumbnail = nil
            return
        }
        thumbnail = UIImage(contentsOfFile: imagePath)
    }

    private func attach(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data),
              let jpegData = image.jpegData(compressionQuality: 0.85) else {
            return
        }

        let attachmentsDirectory = attachmentsURL()
        try? FileManager.default.createDirectory(at: attachmentsDirectory, withIntermediateDirectories: true)

        let fileURL = attachmentsDirectory.appendingPathComponent("\(UUID().uuidString).jpg")
        do {
            try jpegData.write(to: fileURL, options: .atomic)
            onChange(fileURL.path)
            thumbnail = image
        } catch {
            // Leave the field unchanged if the copy failed.
        }
    }

    private func attachmentsURL() -> URL {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documents.appendingPathComponent("attachments", isDirectory: true)
    }
}
