import Shared
import SwiftUI

/// Swift-facing wrapper around the committed Kotlin `DictationStore`,
/// following the same observe pattern as `AssistantViewModel`.
@MainActor
final class DictationReviewViewModel: ObservableObject {
    @Published var state: DictationStoreState

    let store: DictationStore
    private var cancellable: NativeCancellable?

    init(store: DictationStore) {
        self.store = store
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    func setTranscript(_ value: String) {
        store.setTranscript(value: value)
    }

    /// Decodes + validates the session JSON and resolves patient names.
    func validate(sessionJson: String) {
        store.validate(sessionJson: sessionJson)
    }

    func accept(index: Int) {
        store.accept(index: Int64(index))
    }

    func reject(index: Int) {
        store.reject(index: Int64(index))
    }

    deinit {
        cancellable?.cancel()
    }
}

// MARK: - View model helpers over the Kotlin sealed types

extension DictationSuggestionUi {
    /// True when the user has not decided on this suggestion yet.
    var isPending: Bool { decision == nil }

    /// Human-readable flag reasons; empty when validation passed untouched.
    var flagReasons: [String] {
        guard let flagged = record.validation as? SuggestedValidationStateFlagged else {
            return []
        }
        return flagged.reasons.map(Self.flagLabel)
    }

    var isDropped: Bool {
        record.validation is SuggestedValidationStateDropped
    }

    var isResolved: Bool {
        resolution is PatientResolutionResolved
    }

    var resolvedPatientId: Int64? {
        (resolution as? PatientResolutionResolved)?.patient.id
    }

    var resolvedPatientName: String? {
        (resolution as? PatientResolutionResolved)?.patient.name
    }

    var ambiguousCandidates: [Patient] {
        ((resolution as? PatientResolutionAmbiguous)?.candidates as? [Patient]) ?? []
    }

    /// Quarantined suggestions must never reach a save path: structurally
    /// dropped records, or records whose patient could not be uniquely
    /// resolved.
    var isQuarantined: Bool {
        if isDropped { return true }
        if record.patientName == nil { return true }
        return !isResolved
    }

    private static func flagLabel(_ reason: String) -> String {
        switch reason {
        case "date_out_of_range": return "Date outside the last year"
        case "weight_implausible": return "Implausible weight — value cleared"
        case "weight_high": return "Weight above 1500 kg — verify"
        case "follicle_size_implausible": return "Implausible follicle size"
        case "drug_name_truncated": return "Drug name was truncated"
        default: return reason.replacingOccurrences(of: "_", with: " ")
        }
    }
}

// MARK: - Review screen

/// Per-suggestion review list shown after extraction completes.
struct SuggestionReviewView: View {
    @ObservedObject var viewModel: DictationReviewViewModel
    /// Chosen patient per suggestion index after disambiguation.
    @Binding var disambiguatedPatients: [Int: Patient]
    let onFinished: () -> Void

    private var suggestions: [DictationSuggestionUi] {
        viewModel.state.suggestions
    }

    private var validIndices: [Int] {
        suggestions.enumerated()
            .filter { !$0.element.isQuarantined && $0.element.decision != false }
            .map(\.offset)
    }

    private var quarantinedIndices: [Int] {
        suggestions.enumerated()
            .filter { $0.element.isQuarantined && $0.element.decision != false }
            .map(\.offset)
    }

    private var acceptedCount: Int {
        suggestions.enumerated().filter { $0.element.decision == true }.count
    }

    var body: some View {
        List {
            if viewModel.state.error != nil {
                Section {
                    Label(
                        viewModel.state.error ?? "Could not read the dictation.",
                        systemImage: "exclamationmark.triangle.fill"
                    )
                    .foregroundStyle(Theme.amber)
                }
            }

            if !validIndices.isEmpty {
                Section {
                    ForEach(validIndices, id: \.self) { index in
                        suggestionRow(index: index)
                    }
                } header: {
                    RecordFormStyle.sectionHeader("Suggestions")
                } footer: {
                    Text("Tap a card to check it in its full form before accepting.")
                        .font(.caption2)
                        .foregroundStyle(Theme.textSecondary)
                }
            }

            if !quarantinedIndices.isEmpty {
                Section {
                    ForEach(quarantinedIndices, id: \.self) { index in
                        suggestionRow(index: index)
                    }
                } header: {
                    RecordFormStyle.sectionHeader("Needs attention")
                } footer: {
                    Text("These records have an unresolved patient or invalid data and cannot be saved until fixed.")
                        .font(.caption2)
                        .foregroundStyle(Theme.textSecondary)
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            confirmBar
        }
    }

    // MARK: Rows

    @ViewBuilder
    private func suggestionRow(index: Int) -> some View {
        let suggestion = suggestions[index]
        let isRejected = suggestion.decision == false

        HStack(alignment: .top, spacing: 12) {
            navigationDestination(index: index, suggestion: suggestion)
                .frame(maxWidth: 44)

            VStack(alignment: .leading, spacing: 6) {
                SuggestionCardContent(suggestion: suggestion, overridePatient: disambiguatedPatients[index])
                if !suggestion.flagReasons.isEmpty {
                    ForEach(suggestion.flagReasons, id: \.self) { reason in
                        Label(reason, systemImage: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(Theme.amber)
                    }
                }
                if !suggestion.ambiguousCandidates.isEmpty {
                    Button {
                        disambiguationTarget = DisambiguationTarget(index: index)
                    } label: {
                        Label("Choose which horse", systemImage: "person.2")
                            .font(.footnote.weight(.medium))
                            .foregroundStyle(Theme.forestGreen)
                    }
                    .buttonStyle(.plain)
                }
            }

            Spacer()

            decisionControls(index: index, isRejected: isRejected)
        }
        .opacity(isRejected ? 0.45 : 1)
    }

    /// Pushes the matching prefilled edit form when the patient is resolved;
    /// quarantined cards are not tappable.
    @ViewBuilder
    private func navigationDestination(index: Int, suggestion: DictationSuggestionUi) -> some View {
        if let patientId = suggestion.resolvedPatientId ?? disambiguatedPatients[index]?.id {
            NavigationLink {
                editDestination(patientId: patientId, suggestion: suggestion)
            } label: {
                typeIcon(for: suggestion.record.recordType.name)
            }
            .buttonStyle(.plain)
            .disabled(suggestion.isQuarantined && disambiguatedPatients[index] == nil)
        } else {
            typeIcon(for: suggestion.record.recordType.name)
        }
    }

    @ViewBuilder
    private func editDestination(patientId: Int64, suggestion: DictationSuggestionUi) -> some View {
        let prefill = Self.prefill(for: suggestion)
        switch suggestion.record.recordType.name {
        case "weight":
            WeightEditView(patientId: patientId, weightId: nil, prefill: prefill)
        case "deworming":
            DewormingEditView(patientId: patientId, dewormingId: nil, prefill: prefill)
        default:
            UltrasoundEditView(patientId: patientId, ultrasoundId: nil, prefill: prefill)
        }
    }

    private func typeIcon(for typeName: String) -> some View {
        let systemName: String =
            switch typeName {
            case "weight": "scalemass"
            case "deworming": "pills"
            default: "waveform.path.ecg"
            }
        return Image(systemName: systemName)
            .font(.title3)
            .foregroundStyle(Theme.forestGreen)
            .frame(width: 40, height: 40)
            .background(Theme.forestGreen.opacity(0.10))
            .clipShape(Circle())
    }

    private func decisionControls(index: Int, isRejected: Bool) -> some View {
        HStack(spacing: 8) {
            Button {
                viewModel.reject(index: index)
            } label: {
                Image(systemName: isRejected ? "xmark.circle.fill" : "xmark.circle")
                    .font(.title3)
                    .foregroundStyle(isRejected ? Color.red : Theme.textTertiary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Reject suggestion")

            Button {
                viewModel.accept(index: index)
            } label: {
                Image(systemName: suggestions[index].decision == true ? "checkmark.circle.fill" : "checkmark.circle")
                    .font(.title3)
                    .foregroundStyle(suggestions[index].decision == true ? Theme.forestGreen : Theme.textTertiary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Accept suggestion")
        }
    }

    // MARK: Disambiguation

    @State private var disambiguationTarget: DisambiguationTarget?

    // MARK: Confirm bar

    private var confirmBar: some View {
        VStack(spacing: 8) {
            if validIndices.count > 1 {
                Button {
                    for index in validIndices where suggestions[index].decision == nil {
                        viewModel.accept(index: index)
                    }
                } label: {
                    Text("Accept all valid (\(validIndices.count))")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            Button {
                for index in suggestions.indices where suggestions[index].decision == true {
                    viewModel.accept(index: index)
                }
                onFinished()
            } label: {
                Text(acceptedCount > 0 ? "Save \(acceptedCount) record\(acceptedCount == 1 ? "" : "s")" : "Done")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(acceptedCount > 0 ? Theme.forestGreen : Theme.textTertiary)
                    .clipShape(Capsule())
            }
            .disabled(acceptedCount == 0)
            .accessibilityIdentifier("dictation_confirm")
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(.bar)
        .sheet(item: $disambiguationTarget) { target in
            PatientDisambiguationSheet(
                spokenName: suggestions[target.index].record.patientName ?? "",
                candidates: suggestions[target.index].ambiguousCandidates
            ) { chosen in
                disambiguatedPatients[target.index] = chosen
            }
        }
    }

    /// Maps a validated suggestion onto the shared edit-form prefill.
    static func prefill(for suggestion: DictationSuggestionUi) -> RecordPrefill {
        let record = suggestion.record
        return RecordPrefill(
            date: record.date?.description,
            weightKg: record.weightKg.map { "\($0)" },
            ovaryStatus: record.ovaryStatus,
            uterineStatus: record.uterineStatus,
            follicleSizeMm: record.follicleSizeMm.map { "\($0)" },
            drugName: record.drugName,
            notes: record.notes
        )
    }
}

/// One suggestion's textual summary: type, date, key fields, patient line.
struct SuggestionCardContent: View {
    let suggestion: DictationSuggestionUi
    let overridePatient: Patient?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                Spacer()
                if let date = suggestion.record.date {
                    Text(date.description)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                }
            }

            if let detail = detailLine {
                Text(detail)
                    .font(.footnote)
                    .foregroundStyle(Theme.textSecondary)
            }

            patientLine
        }
    }

    private var title: String {
        switch suggestion.record.recordType.name {
        case "weight": return "Weight entry"
        case "deworming": return "Deworming"
        default: return "Ultrasound"
        }
    }

    private var detailLine: String? {
        let record = suggestion.record
        switch record.recordType.name {
        case "weight":
            return record.weightKg.map { "\($0) kg" }
        case "deworming":
            return record.drugName
        default:
            var parts: [String] = []
            if let ovary = record.ovaryStatus { parts.append(ovary) }
            if let uterus = record.uterineStatus { parts.append(uterus) }
            if let follicle = record.follicleSizeMm { parts.append("\(follicle) mm") }
            return parts.isEmpty ? nil : parts.joined(separator: " · ")
        }
    }

    @ViewBuilder
    private var patientLine: some View {
        if let name = overridePatient?.name ?? suggestion.resolvedPatientName {
            Label(name, systemImage: "horse")
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
        } else if let spoken = suggestion.record.patientName {
            Label("\"\(spoken)\" — no match", systemImage: "questionmark.circle")
                .font(.caption)
                .foregroundStyle(Theme.amber)
        } else {
            Label("No patient mentioned", systemImage: "questionmark.circle")
                .font(.caption)
                .foregroundStyle(Theme.amber)
        }
    }
}

/// Sheet presentation target for patient disambiguation.
struct DisambiguationTarget: Identifiable {
    let index: Int
    var id: Int { index }
}
