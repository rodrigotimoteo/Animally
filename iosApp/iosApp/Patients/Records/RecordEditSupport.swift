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

// MARK: - Dictation prefill

/// Values dictated by voice and pushed into a record edit form when it
/// finishes loading. Only fields the transcript expressed are non-nil.
struct RecordPrefill {
    var date: String?
    var weightKg: String?
    var ovaryStatus: String?
    var uterineStatus: String?
    var follicleSizeMm: String?
    var drugName: String?
    var medicationName: String?
    var medicationDosage: String?
    var notes: String?
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

    /// ISO yyyy-MM-dd formatter — single source via DateFormatters.
    static var isoDateFormatter: DateFormatter { DateFormatters.iso }

    /// Native compact date picker bound through an ISO yyyy-MM-dd string so
    /// it can drive the Kotlin-backed form fields without any parsing glue
    /// at the call sites.
    static func dateField(_ placeholder: String, value: String, onChange: @escaping (String) -> Void) -> some View {
        DatePicker(
            placeholder,
            selection: Binding(
                get: { DateFormatters.iso.date(from: value) ?? Date() },
                set: { onChange(DateFormatters.iso.string(from: $0)) }
            ),
            displayedComponents: .date
        )
        .datePickerStyle(.compact)
        .textCase(nil)
    }

    /// Optional date control that does not invent today's date for an empty
    /// value. A missing date is explicit and can be set or cleared without
    /// writing a value back to the Kotlin form accidentally.
    static func optionalDateField(
        _ title: String,
        value: String?,
        onChange: @escaping (String?) -> Void
    ) -> some View {
        HStack {
            if let value, let parsedDate = DateFormatters.iso.date(from: value) {
                DatePicker(
                    title,
                    selection: Binding(
                        get: { parsedDate },
                        set: { onChange(DateFormatters.iso.string(from: $0)) }
                    ),
                    displayedComponents: .date
                )
                .datePickerStyle(.compact)

                Button {
                    onChange(nil)
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.textTertiary)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel("Clear \(title)")
            } else {
                Text(title)
                    .foregroundStyle(Theme.textPrimary)
                Spacer()
                Text("Not set")
                    .foregroundStyle(Theme.textSecondary)
                Button("Set") {
                    onChange(DateFormatters.iso.string(from: Date()))
                }
                .buttonStyle(.borderless)
            }
        }
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
        InlineErrorBanner(message: message, onDismiss: onDismiss)
    }
}
