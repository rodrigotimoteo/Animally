import SwiftUI
import Shared

/// Presentation state for one record section.
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

// MARK: - Date helpers are centralized in DateFormatters.swift — single source
// (displayString / friendlyString / swiftDate / kotlinLocalDate / TodayProvider all delegate there)

// MARK: - Gestation helpers

enum GestationCalculator {
    static func gestationDay(for gestation: Gestation_, today: Kotlinx_datetimeLocalDate) -> Int {
        max(0, Int(today.epochDaysCompat() - gestation.breedingDate.epochDaysCompat()))
    }
    static func daysUntilDue(for gestation: Gestation_, today: Kotlinx_datetimeLocalDate) -> Int {
        Int(gestation.expectedDueDate.epochDaysCompat() - today.epochDaysCompat())
    }
    static func daysUntil(_ date: Kotlinx_datetimeLocalDate, today: Kotlinx_datetimeLocalDate) -> Int {
        Int(date.epochDaysCompat() - today.epochDaysCompat())
    }
}

enum GestationDueText {
    static func daysLabel(daysUntilDue: Int) -> String {
        if daysUntilDue < 0 { return "Overdue by \(-daysUntilDue) day\(-daysUntilDue == 1 ? "" : "s")" }
        if daysUntilDue == 0 { return "Due today" }
        return "Due in \(daysUntilDue) day\(daysUntilDue == 1 ? "" : "s")"
    }
}

// MARK: - RecordTypeIcon — single map for 15+ cases

enum RecordTypeIcon {
    static func systemName(for raw: String) -> String {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "VACCINATION": return "syringe.fill"
        case "DEWORMING": return "pills.fill"
        case "CONSULTATION": return "stethoscope"
        case "WEIGHT": return "scalemass.fill"
        case "REPRODUCTION", "REPRODUCTION_EVENT", "REPRODUCTIONEVENT": return "heart.fill"
        case "FARRIER", "FARRIER_VISIT", "FARRIERVISIT": return "figure.walk"
        case "DENTISTRY": return "mouth.fill"
        case "CUSTOM_REMINDER", "CUSTOMREMINDER", "CUSTOM REMINDER": return "bell.badge.fill"
        case "EMBRYO_TRANSFER", "EMBRYOTRANSFER", "EMBRYO TRANSFER": return "arrow.triangle.branch"
        case "ICSI": return "scope"
        case "ULTRASOUND": return "waveform.path.ecg"
        case "GESTATION": return "heart.circle.fill"
        case "REPRO_MEDICATION", "REPROMEDICATION", "REPRO MEDICATION": return "pills"
        case "LAB_RESULT", "LABRESULT", "LAB RESULT": return "testtube.2"
        case "IMAGING": return "photo.fill"
        case "LAMENESS": return "figure.walk.motion"
        case "SURGERY": return "cross.case.fill"
        case "MEDICATION": return "pill.fill"
        case "CONTROLLED_SUBSTANCE", "CONTROLLEDSUBSTANCE", "CONTROLLED SUBSTANCE", "SUBSTANCE": return "cross.vial.fill"
        case "ANAMNESE": return "doc.text.fill"
        case "PATIENT": return "pawprint.fill"
        case "OWNER": return "person.fill"
        default: return "doc.text.fill"
        }
    }

    static func systemName(for recordType: RecordType) -> String {
        systemName(for: recordType.wireName)
    }
}

// MARK: - Shared Gestation Card — replaces pregnancyCard vs ActiveGestationCard duplication

struct GestationCard: View {
    let gestation: Gestation_
    let gestationDay: Int
    let daysUntilDue: Int
    let onTap: () -> Void

    private static let dueSoonDays = 30

    private var isDueSoon: Bool { daysUntilDue <= Self.dueSoonDays }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "heart.circle.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Theme.forestGreen)

                    VStack(alignment: .leading, spacing: 4) {
                        Text("In Foal")
                            .font(.caption.weight(.bold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Theme.forestGreen)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())

                        Text("Day \(gestationDay)")
                            .font(.title.weight(.bold))
                            .foregroundStyle(Theme.textPrimary)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        Text(gestation.expectedDueDate.friendlyString)
                            .font(.headline)
                            .foregroundStyle(isDueSoon ? Theme.amber : Theme.textPrimary)
                        Text(GestationDueText.daysLabel(daysUntilDue: daysUntilDue))
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(isDueSoon ? Theme.amber : Theme.textSecondary)
                    }
                }

                Divider()

                HStack(spacing: 16) {
                    Label {
                        Text("Bred \(gestation.breedingDate.friendlyString)")
                    } icon: {
                        Image(systemName: "calendar")
                    }
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)

                    if let fetalCount = gestation.fetalCount {
                        Label {
                            Text("\(fetalCount) fetus\(fetalCount.intValue > 1 ? "es" : "")")
                        } icon: {
                            Image(systemName: "number")
                        }
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.textTertiary)
                }
            }
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("In foal, day \(gestationDay), due \(gestation.expectedDueDate.friendlyString)")
        .accessibilityHint("Opens gestation detail")
    }
}

/// Compact gestation card for overview — same visuals, smaller footprint
struct CompactGestationCard: View {
    let gestation: Gestation_
    let gestationDay: Int
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: "heart.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(Theme.forestGreen)

                VStack(alignment: .leading, spacing: 4) {
                    Text("In Foal")
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Theme.forestGreen)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())

                    Text("Day \(gestationDay) · Due \(gestation.expectedDueDate.friendlyString)")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.textPrimary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
            }
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("In foal, day \(gestationDay), due \(gestation.expectedDueDate.friendlyString). Opens reproduction tab")
    }
}

// MARK: - Generic Record Row — expanded reuse for Search / Timeline

struct RecordRowView: View {
    let icon: String
    let iconTint: Color
    let title: String
    let subtitle: String?
    let date: String?
    var showsDisclosure: Bool = false
    var badgeSize: CGFloat = 36
    var badgeCorner: CGFloat = 8

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RecordBadgeIcon(systemName: icon, tint: iconTint, size: badgeSize, corner: badgeCorner)

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

            if showsDisclosure {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title), \(subtitle ?? ""), \(date ?? "")")
    }
}

// MARK: - Section Container

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
                            .accessibilityLabel("Clear search for \(title)")
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
                    .accessibilityHidden(true)
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
                    .accessibilityHint("Opens search for \(title)")
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

// Simplified: String? directly, enum wrapper removed
typealias RecordExtraLineLabel = String?

extension String {
    static var nextDueLabel: String { "Next due" }
}

struct RecordSectionSpec<Item> {
    let title: String
    let icon: String
    let items: [Item]
    let recordId: (Item) -> Int64
    let rowTitle: (Item) -> String
    let rowSubtitle: (Item) -> String?
    let rowDate: (Item) -> String?
    let displayType: String
    let onDelete: (Item) -> Void
    let display: RecordSectionDisplayState?

    var deleteTitle: String? = nil
    var extraLine: ((Item) -> String?)? = nil
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
        self.onDelete = onDelete
        self.deleteTitle = deleteTitle
        self.extraLine = extraLine
        self.extraLineLabel = extraLineLabel
        self.display = display
    }
}

// MARK: - RecordBadgeIcon — moved from InsightShared to single shared location
struct RecordBadgeIcon: View {
    let systemName: String
    var tint: Color = Theme.forestGreen
    var size: CGFloat = 36
    var corner: CGFloat = 8
    var body: some View {
        Image(systemName: systemName)
            .font(.body)
            .foregroundStyle(tint)
            .frame(width: size, height: size)
            .background(tint.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: corner))
            .accessibilityHidden(true)
    }
}

@ViewBuilder
func recordSection<Item>(
    _ spec: RecordSectionSpec<Item>,
    onOpenRecord: ((String, Int64) -> Void)?
) -> some View {
    RecordSection(
        title: spec.title,
        icon: spec.icon,
        count: spec.display?.matchingCount ?? spec.items.count,
        display: spec.display
    ) {
        RecordSectionRows(spec: spec, onOpenRecord: onOpenRecord)
    }
}

private enum RecordSectionRowsConfig {
    static let collapseLimit = 5
}

private struct RecordSectionRows<Item>: View {
    let spec: RecordSectionSpec<Item>
    let onOpenRecord: ((String, Int64) -> Void)?

    var body: some View {
        Group {
            // No tuple allocation, stable index id avoids duplicate-id crash
            ForEach(spec.items.indices, id: \.self) { idx in
                row(spec.items[idx])
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
                        .accessibilityHidden(true)
                    Text(extraLineText(extra))
                        .font(.caption2)
                }
                .foregroundStyle(Theme.amber)
                .padding(.leading, 48)
                .accessibilityLabel(accessibilityExtraLabel(extra))
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("record_row_\(spec.displayType)_\(spec.recordId(item))")
        .onTapGesture {
            onOpenRecord?(spec.displayType, spec.recordId(item))
        }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint("Opens \(spec.displayType) detail")
        .confirmationSwipeDelete(title: spec.deleteTitle ?? spec.title) {
            spec.onDelete(item)
        }
    }

    private func extraLineText(_ extra: String) -> String {
        if let label = spec.extraLineLabel { return "\(label): \(extra)" }
        return extra
    }

    private func accessibilityExtraLabel(_ extra: String) -> String {
        if let label = spec.extraLineLabel { return "\(label) \(extra)" }
        return extra
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
                .accessibilityHidden(true)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .accessibilityElement(children: .combine)
        .accessibilityLabel(message)
    }
}
