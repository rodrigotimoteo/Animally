import SwiftUI
import Shared

// MARK: - Date Formatting Helper

extension Kotlinx_datetimeLocalDate {
    var displayString: String {
        "\(year)-\(String(format: "%02d", monthNumber))-\(String(format: "%02d", dayOfMonth))"
    }

    /// Human-friendly date, e.g. "22 Aug 2026".
    var friendlyString: String {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let index = Int(monthNumber) - 1
        let month = index >= 0 && index < months.count ? months[index] : ""
        return "\(dayOfMonth) \(month) \(year)"
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
/// lines, tap-to-open-record wiring, and swipe-to-delete. When more than
/// `RecordSectionRowsConfig.collapseLimit` records exist, only the 5 most recent
/// (by row date, newest first) are shown until the user expands inline.
@ViewBuilder
func recordSection<Item>(
    _ spec: RecordSectionSpec<Item>,
    onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)?
) -> some View {
    RecordSection(title: spec.title, icon: spec.icon, count: spec.items.count) {
        RecordSectionRows(spec: spec, onOpenRecord: onOpenRecord)
    }
}

/// Collapse threshold shared by every section: sections with more rows show
/// only the most recent N until expanded.
private enum RecordSectionRowsConfig {
    static let collapseLimit = 5
}

/// Row list for one record section with the >5 collapse/expand behavior.
/// Rows are always shown newest-first by row date; expanding reveals the rest.
private struct RecordSectionRows<Item>: View {
    let spec: RecordSectionSpec<Item>
    let onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)?

    @State private var isExpanded = false

    private var sortedItems: [Item] {
        spec.items.sorted { a, b in
            let da = spec.rowDate(a) ?? ""
            let db = spec.rowDate(b) ?? ""
            if da != db { return da > db }
            // Same-date ties (bulk-created records): newer auto-increment id wins
            // so freshly created rows always surface in the recent window.
            return spec.recordId(a) > spec.recordId(b)
        }
    }

    var body: some View {
        let items = sortedItems
        let visibleCount = isExpanded ? items.count : min(items.count, RecordSectionRowsConfig.collapseLimit)

        Group {
            ForEach(
                items.prefix(visibleCount).map { (spec.recordId($0), $0) },
                id: \.0
            ) { _, item in
                row(item)
            }

            if items.count > RecordSectionRowsConfig.collapseLimit {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    isExpanded.toggle()
                }
            } label: {
                HStack(spacing: 4) {
                    Spacer()
                    Text(isExpanded ? "Show less" : "Show all \(items.count)")
                        .font(.subheadline.weight(.medium))
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption.weight(.semibold))
                }
                    .foregroundStyle(Theme.forestGreen)
                    .padding(.vertical, 6)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isExpanded ? "Show fewer \(spec.title)" : "Show all \(items.count) \(spec.title)")
            }
        }
        .onChange(of: items.count) { _, _ in
            // Fresh data (reload/delete): collapse back to the recent top 5.
            isExpanded = false
        }
    }

    @ViewBuilder
    private func row(_ item: Item) -> some View {
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
