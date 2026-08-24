import Shared
import SwiftUI

/// Lets the user pick which existing patient an ambiguous spoken name refers
/// to. Presented from the review screen's quarantine section.
struct PatientDisambiguationSheet: View {
    let spokenName: String
    let candidates: [Patient]
    let onChoose: (Patient) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(candidates, id: \.id) { patient in
                Button {
                    onChoose(patient)
                    dismiss()
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(patient.name)
                            .font(.body.weight(.medium))
                            .foregroundStyle(Theme.textPrimary)
                    }
                }
                .buttonStyle(.plain)
            }
            .navigationTitle("Which horse?")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
            .overlay {
                if candidates.isEmpty {
                    ContentUnavailableView(
                        "No matching horses",
                        systemImage: "questionmark.circle",
                        description: Text("\"\(spokenName)\" did not match any patient.")
                    )
                }
            }
        }
        .presentationDetents([.medium])
    }
}
