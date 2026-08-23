import SwiftUI
import Shared

// MARK: - Date Formatting Helper

extension Kotlinx_datetimeLocalDate {
    var displayString: String {
        "\(year)-\(String(format: "%02d", monthNumber))-\(String(format: "%02d", dayOfMonth))"
    }
}

// MARK: - Generic Record Row

/// Standard row for displaying a record with date, title, subtitle, and icon.
struct RecordRowView: View {
    let icon: String
    let iconTint: Color
    let title: String
    let subtitle: String?
    let date: String?

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(iconTint)
                .frame(width: 36, height: 36)
                .background(iconTint.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                    .lineLimit(1)

                if let subtitle = subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            if let date = date {
                Text(date)
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Section Container

/// Collapsible section for a record type within a tab.
struct RecordSection<Content: View>: View {
    let title: String
    let icon: String
    let count: Int
    @ViewBuilder let content: () -> Content

    var body: some View {
        if count > 0 {
            Section {
                content()
            } header: {
                HStack(spacing: 6) {
                    Image(systemName: icon)
                        .font(.caption)
                        .foregroundStyle(Theme.forestGreen)
                    Text("\(title) (\(count))")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                        .textCase(nil)
                }
            }
        }
    }
}

// MARK: - Generic Record Section

/// Declarative description of one record section: row presentation, tap
/// fields, delete action, and an optional amber calendar line under each row.
struct RecordSectionSpec<Item> {
    let title: String
    let icon: String
    let items: [Item]
    /// Extracts the Kotlin record id for tap/swipe wiring.
    let recordId: (Item) -> Int64
    let rowTitle: (Item) -> String
    let rowSubtitle: (Item) -> String?
    let rowDate: (Item) -> String?
    let displayType: String
    let fields: (Item) -> [RecordDetailNav.FieldRow]
    let onDelete: (Item) -> Void

    /// Title shown on the swipe-delete button; defaults to `title`.
    var deleteTitle: String? = nil

    /// Optional single-line extra under the row (rendered in the amber
    /// calendar style used by next-due lines). Nil line = no extra.
    var extraLine: ((Item) -> String?)? = nil

    init(
        title: String,
        icon: String,
        items: [Item],
        recordId: @escaping (Item) -> Int64,
        rowTitle: @escaping (Item) -> String,
        rowSubtitle: @escaping (Item) -> String?,
        rowDate: @escaping (Item) -> String?,
        displayType: String,
        fields: @escaping (Item) -> [RecordDetailNav.FieldRow],
        onDelete: @escaping (Item) -> Void,
        deleteTitle: String? = nil,
        extraLine: ((Item) -> String?)? = nil
    ) {
        self.title = title
        self.icon = icon
        self.items = items
        self.recordId = recordId
        self.rowTitle = rowTitle
        self.rowSubtitle = rowSubtitle
        self.rowDate = rowDate
        self.displayType = displayType
        self.fields = fields
        self.onDelete = onDelete
        self.deleteTitle = deleteTitle
        self.extraLine = extraLine
    }
}

/// Renders a `RecordSectionSpec`: section container, rows, optional extra
/// lines, tap-to-open-record wiring, and swipe-to-delete.
@ViewBuilder
func recordSection<Item>(
    _ spec: RecordSectionSpec<Item>,
    onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)?
) -> some View {
    RecordSection(title: spec.title, icon: spec.icon, count: spec.items.count) {
        ForEach(spec.items.indices, id: \.self) { index in
            let item = spec.items[index]
            VStack(alignment: .leading, spacing: 6) {
                RecordRowView(
                    icon: spec.icon,
                    iconTint: Theme.forestGreen,
                    title: spec.rowTitle(item),
                    subtitle: spec.rowSubtitle(item),
                    date: spec.rowDate(item)
                )
                if let extra = spec.extraLine?(item), !extra.isEmpty {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                            .font(.caption2)
                        Text("Next due: \(extra)")
                            .font(.caption2)
                    }
                    .foregroundStyle(Theme.amber)
                    .padding(.leading, 48)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onOpenRecord?(spec.displayType, spec.recordId(item), spec.fields(item).filter { !$0.value.isEmpty })
            }
            .recordSwipeDelete(title: spec.deleteTitle ?? spec.title) {
                spec.onDelete(item)
            }
        }
    }
}

// MARK: - Empty State for Tab

struct TabEmptyStateView: View {
    let icon: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundStyle(Theme.forestGreen.opacity(0.4))
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
