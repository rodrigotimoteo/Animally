import SwiftUI
import Shared

/// Presentation state for one record section.
///
/// The Kotlin list view model owns filtering, ordering, and the collapsed
/// projection. SwiftUI only keeps the full collection available for derived
/// tab-level content such as the active gestation card.
struct RecordListState<Item> {
    var allItems: [Item] = []
    var visibleItems: [Item] = []
    var matchingCount: Int = 0
    var searchQuery: String?
    var isExpanded: Bool = false

    var totalCount: Int { allItems.count }
    var isSearching: Bool { searchQuery != nil }

    func sectionDisplay(
        onSearchClick: @escaping () -> Void,
        onSearchQueryChange: @escaping (String) -> Void,
        onCloseSearch: @escaping () -> Void,
        onToggleExpanded: @escaping () -> Void
    ) -> RecordSectionDisplayState {
        RecordSectionDisplayState(
            totalCount: totalCount,
            matchingCount: matchingCount,
            searchQuery: searchQuery,
            isExpanded: isExpanded,
            onSearchClick: onSearchClick,
            onSearchQueryChange: onSearchQueryChange,
            onCloseSearch: onCloseSearch,
            onToggleExpanded: onToggleExpanded
        )
    }
}

/// Search and expansion actions supplied by the shared Kotlin list state.
struct RecordSectionDisplayState {
    let totalCount: Int
    let matchingCount: Int
    let searchQuery: String?
    let isExpanded: Bool
    let onSearchClick: () -> Void
    let onSearchQueryChange: (String) -> Void
    let onCloseSearch: () -> Void
    let onToggleExpanded: () -> Void

    var isSearching: Bool { searchQuery != nil }
    var hasBlankSearch: Bool { searchQuery?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true }
}

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
        HStack(alignment: .top, spacing: 12) {
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
                    .lineLimit(2)

                if let subtitle = subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                        .lineLimit(2)
                }
            }

            Spacer()

            if let date = date {
                Text(date)
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
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
    let display: RecordSectionDisplayState?
    @ViewBuilder let content: () -> Content

    init(
        title: String,
        icon: String,
        count: Int,
        display: RecordSectionDisplayState? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.icon = icon
        self.count = count
        self.display = display
        self.content = content
    }

    var body: some View {
        if count > 0 || (display?.totalCount ?? 0) > 0 {
            Section {
                if let display, display.isSearching, display.matchingCount == 0 {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("No matches")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Theme.textPrimary)
                        Button("Clear search", action: display.onCloseSearch)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Theme.forestGreen)
                    }
                    .padding(.vertical, 8)
                } else {
                    content()
                }
            } header: {
                RecordSectionHeader(title: title, icon: icon, count: count, display: display)
            }
        }
    }
}

private struct RecordSectionHeader: View {
    let title: String
    let icon: String
    let count: Int
    let display: RecordSectionDisplayState?
    @FocusState private var isSearchFocused: Bool

    var body: some View {
        HStack(spacing: 8) {
            if let display, display.isSearching {
                TextField("Search \(title)", text: Binding(
                    get: { display.searchQuery ?? "" },
                    set: display.onSearchQueryChange
                ))
                .textFieldStyle(.roundedBorder)
                .font(.subheadline)
                .focused($isSearchFocused)
                .accessibilityLabel("Search \(title)")

                Button {
                    isSearchFocused = false
                    display.onCloseSearch()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.textSecondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close search \(title)")
            } else {
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundStyle(Theme.forestGreen)
                Text("\(title) (\(count))")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.forestGreen)
                    .textCase(nil)

                Spacer(minLength: 4)

                if let display {
                    Button(action: display.onSearchClick) {
                        Image(systemName: "magnifyingglass")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Theme.forestGreen)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Search \(title)")
                }
            }
        }
        .onAppear {
            isSearchFocused = display?.isSearching == true
        }
        .onChange(of: display?.isSearching) { _, isSearching in
            isSearchFocused = isSearching ?? false
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
    let display: RecordSectionDisplayState?

    /// Title shown on the swipe-delete button; defaults to `title`.
    var deleteTitle: String? = nil

    /// Optional single-line extra under the row (rendered in the amber
    /// calendar style used by next-due lines). Nil line = no extra.
    var extraLine: ((Item) -> String?)? = nil

    /// Optional label for [extraLine]. `nil` renders the value without a prefix.
    var extraLineLabel: String? = "Next due"

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
        extraLine: ((Item) -> String?)? = nil,
        extraLineLabel: String? = "Next due",
        display: RecordSectionDisplayState? = nil
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
        self.extraLineLabel = extraLineLabel
        self.display = display
    }
}

/// Renders a `RecordSectionSpec`: section container, rows, optional extra
/// lines, tap-to-open-record wiring, and swipe-to-delete. The shared Kotlin
/// list state supplies the filtered and collapsed item projection.
@ViewBuilder
func recordSection<Item>(
    _ spec: RecordSectionSpec<Item>,
    onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)?
) -> some View {
    RecordSection(
        title: spec.title,
        icon: spec.icon,
        count: spec.display?.matchingCount ?? spec.items.count,
        display: spec.display
    ) {
        RecordSectionRows(spec: spec, onOpenRecord: onOpenRecord)
    }
    .id(sectionIdentity(for: spec))
}

private func sectionIdentity<Item>(for spec: RecordSectionSpec<Item>) -> String {
    let query = spec.display?.searchQuery ?? "<closed>"
    let expanded = spec.display?.isExpanded == true
    return "\(spec.title)|\(expanded)|\(query)"
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

    var body: some View {
        Group {
            ForEach(
                spec.items.map { (spec.recordId($0), $0) },
                id: \.0
            ) { _, item in
                row(item)
            }

            if let display = spec.display,
               display.matchingCount > RecordSectionRowsConfig.collapseLimit,
               display.hasBlankSearch {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        display.onToggleExpanded()
                    }
                } label: {
                    HStack(spacing: 4) {
                        Spacer()
                        Text(display.isExpanded ? "Show less" : "Show all \(display.matchingCount)")
                            .font(.subheadline.weight(.medium))
                        Image(systemName: display.isExpanded ? "chevron.up" : "chevron.down")
                            .font(.caption.weight(.semibold))
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .contentShape(Rectangle())
                    .foregroundStyle(Theme.forestGreen)
                    .padding(.vertical, 6)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    display.isExpanded
                        ? "Show fewer \(spec.title)"
                        : "Show all \(display.matchingCount) \(spec.title)"
                )
            }
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
                    Text("\(spec.extraLineLabel.map { "\($0): " } ?? "")\(extra)")
                        .font(.caption2)
                }
                .foregroundStyle(Theme.amber)
                .padding(.leading, 48)
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("record_row_\(spec.displayType)_\(spec.recordId(item))")
        .onTapGesture {
            onOpenRecord?(spec.displayType, spec.recordId(item), spec.fields(item).filter { !$0.value.isEmpty })
        }
        .confirmationSwipeDelete(title: spec.deleteTitle ?? spec.title) {
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
