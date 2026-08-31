import SwiftUI
import Shared

/// Current gestation snapshot: active pregnancies + due-soon groups.
/// Deterministic Kotlin calculation: gestationDay from breedingDate, persisted expectedDueDate,
/// overdue explicit (daysUntilDue negative), due-soon 0..30/60/90 inclusive, sorted by dueDate.
struct InsightsGestationSection: View {
    let currentCare: CurrentCareSnapshot
    var onOpenGestation: ((Int64, Int64) -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header

            // Summary counts
            summaryRow

            // Active gestations list or empty
            if currentCare.activeGestations.isEmpty {
                emptyState
            } else {
                gestationList
            }
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("insights_gestation_section")
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 8) {
            Image(systemName: "heart.circle.fill")
                .foregroundStyle(Theme.forestGreen)
            Text("Current gestations")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Spacer()
            Text("\(currentCare.activeGestations.count) active")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .accessibilityLabel("Active gestations \(currentCare.activeGestations.count)")
        }
        .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Summary row (due-soon 30/60/90)
    // Inclusive cumulative sets 0..N — labels use ≤ to avoid misreading as exclusive slices 0-30,31-60,61-90
    private var summaryRow: some View {
        HStack(spacing: 12) {
            summaryPill(title: "≤30d", count: Int(currentCare.dueSoon30.count), color: Theme.amber)
            summaryPill(title: "≤60d", count: Int(currentCare.dueSoon60.count), color: Theme.forestGreen)
            summaryPill(title: "≤90d", count: Int(currentCare.dueSoon90.count), color: Theme.textSecondary)
            Spacer()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Due in ≤30 days \(currentCare.dueSoon30.count), ≤60 days \(currentCare.dueSoon60.count), ≤90 days \(currentCare.dueSoon90.count)")
    }

    private func summaryPill(title: String, count: Int, color: Color) -> some View {
        VStack(spacing: 2) {
            Text("\(count)")
                .font(.title3.weight(.bold))
                .foregroundStyle(color)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(title)
                .font(.caption2.weight(.medium))
                .foregroundStyle(Theme.textSecondary)
                .lineLimit(1)
        }
        .frame(minWidth: 52)
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title), \(count)")
    }

    // MARK: - Empty

    private var emptyState: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.forestGreen.opacity(0.35))
            Text("No active gestations")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
        }
        .padding(12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityLabel("No active gestations")
    }

    // MARK: - List

    private var gestationList: some View {
        VStack(spacing: 8) {
            ForEach(currentCare.activeGestations, id: \.gestationId) { item in
                gestationRow(item)
            }
        }
    }

    private func gestationRow(_ item: CurrentGestationItem) -> some View {
        let days = Int(item.daysUntilDue)
        let isOverdue = days < 0
        let isDueSoon = days >= 0 && days <= 30
        let accent: Color = isOverdue || isDueSoon ? Theme.amber : Theme.forestGreen
        let dueLabel = dueText(days: days)
        // Gestation rows always enabled — current-state snapshot, not period-limited (vs reproduction drill-down which gates on effectiveRange)
        return Button {
            onOpenGestation?(item.patientId, item.gestationId)
        } label: {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "heart.fill")
                    .font(.body)
                    .foregroundStyle(accent)
                    .frame(width: 36, height: 36)
                    .background(accent.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                VStack(alignment: .leading, spacing: 3) {
                    Text(item.patientName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.textPrimary)
                        .lineLimit(1)
                    HStack(spacing: 6) {
                        Text("Day \(item.gestationDay)")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Theme.textSecondary)
                        Text("·")
                            .foregroundStyle(Theme.textTertiary)
                        Text(item.status.isEmpty ? "—" : item.status)
                            .font(.caption)
                            .foregroundStyle(Theme.textSecondary)
                            .lineLimit(1)
                    }
                    Text("Due \(item.dueDate.friendlyString) · \(dueLabel)")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(accent)
                        .lineLimit(1)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
            }
            .padding(10)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel(for: item, days: days))
        .accessibilityHint("Opens gestation record")
        .accessibilityIdentifier("insights_gestation_\(item.gestationId)")
    }

    private func dueText(days: Int) -> String {
        if days < 0 { return "Overdue by \(-days) day\(-days == 1 ? "" : "s")" }
        if days == 0 { return "Due today" }
        return "Due in \(days) day\(days == 1 ? "" : "s")"
    }

    private func accessibilityLabel(for item: CurrentGestationItem, days: Int) -> String {
        let due = dueText(days: days)
        return "\(item.patientName), day \(item.gestationDay), \(due), \(item.dueDate.friendlyString)"
    }
}
