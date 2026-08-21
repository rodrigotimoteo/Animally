import Foundation
import Shared
import SwiftUI

/// Generic plumbing shared by every record edit screen: subscribes to a
/// Kotlin-backed store's state flow and detects save completion the same
/// way `PatientEditViewModel` does (was saving → no longer saving → no
/// validation error → pop).
///
/// Each entity supplies its own `isSaving` / `hasError` readers because
/// every `*StoreState` carries its own validation-error properties.
@MainActor
class RecordFormViewModel<State: AnyObject>: ObservableObject {
    @Published var state: State

    /// Invoked after a successful save so the view can pop.
    var onSaved: (() -> Void)?

    private var cancellable: NativeCancellable?
    private let isSaving: (State) -> Bool
    private let hasError: (State) -> Bool

    init(
        initial: State,
        subscribe: (@escaping (State) -> Void) -> NativeCancellable,
        isSaving: @escaping (State) -> Bool,
        hasError: @escaping (State) -> Bool
    ) {
        self.state = initial
        self.isSaving = isSaving
        self.hasError = hasError

        cancellable = subscribe({ [weak self] newState in
            Task { @MainActor in
                guard let self else { return }
                let wasSaving = self.isSaving(self.state)
                self.state = newState
                if wasSaving,
                   !self.isSaving(newState),
                   !self.hasError(newState) {
                    self.onSaved?()
                }
            }
        })
    }

    deinit {
        cancellable?.cancel()
    }
}

// MARK: - Shared field styling helpers

/// Reusable pieces matching PatientEditView's design language exactly.
enum RecordFormStyle {
    static func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
    }

    static func errorText(_ message: String) -> some View {
        Text(message)
            .font(.caption)
            .foregroundStyle(.red)
    }

    static func dateField(_ placeholder: String, value: String, onChange: @escaping (String) -> Void) -> some View {
        TextField(placeholder, text: Binding(
            get: { value },
            set: { onChange($0) }
        ))
        .keyboardType(.numbersAndPunctuation)
        .textCase(nil)
    }

    static func textField(_ placeholder: String, value: String?, onChange: @escaping (String) -> Void) -> some View {
        TextField(placeholder, text: Binding(
            get: { value ?? "" },
            set: { onChange($0) }
        ))
        .textCase(nil)
    }

    static func notesField(value: String?, onChange: @escaping (String) -> Void) -> some View {
        TextField("Notes", text: Binding(
            get: { value ?? "" },
            set: { onChange($0) }
        ), axis: .vertical)
        .lineLimit(3...6)
        .textCase(nil)
    }

    static func loadingView(subject: String) -> some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading \(subject)…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    static func notFoundView(subject: String) -> some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.amber)
            Text("\(subject) not found")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    static func errorBanner(message: String, onDismiss: @escaping () -> Void) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                onDismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}
