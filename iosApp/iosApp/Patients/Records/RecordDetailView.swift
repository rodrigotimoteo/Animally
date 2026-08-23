import SwiftUI

/// Navigation payload for the read-only record detail screen.
struct RecordDetailNav: Identifiable, Hashable {
    struct FieldRow: Identifiable, Hashable {
        let label: String
        let value: String
        var id: String { label }

        init(label: String, value: String) {
            self.label = label
            self.value = value
        }
    }

    let id = UUID()
    let title: String
    let displayType: String
    let patientId: Int64
    let recordId: Int64
    let fields: [FieldRow]
}

/// Read-only view of everything inside one record. "Edit" opens the
/// prefilled form; back returns to the patient page.
struct RecordDetailView: View {
    let nav: RecordDetailNav
    let onEdit: () -> Void

    var body: some View {
        List {
            Section {
                ForEach(nav.fields) { field in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(field.label)
                            .font(.caption)
                            .foregroundStyle(Theme.textSecondary)
                        Text(field.value)
                            .font(.subheadline)
                            .foregroundStyle(Theme.textPrimary)
                    }
                    .padding(.vertical, 3)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(nav.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Edit", action: onEdit)
                    .font(.subheadline.weight(.semibold))
            }
        }
    }
}
