import SwiftUI
import Shared

// MARK: - Internship overview summary cards
//
// Renders every value straight from `OverviewMetrics`; no calculation in Swift.
// Handles null averages explicitly ("—" + unavailable hint) and shows comparison
// deltas only when the Kotlin use case supplies them.

struct InsightsOverviewSection: View {
    let overview: OverviewMetrics
    let isComparisonEnabled: Bool
    var onOpenSourceRecords: (() -> Void)?

    // Adaptive grid: two columns on regular width, wraps gracefully on compact
    // and with large Dynamic Type because cards flex vertically and never clip.
    private let columns = [
        GridItem(.flexible(minimum: 148), spacing: 12),
        GridItem(.flexible(minimum: 148), spacing: 12),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "chart.bar.doc.horizontal")
                    .foregroundStyle(Theme.forestGreen)
                Text("Internship overview")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                Spacer()
            }
            .accessibilityAddTraits(.isHeader)

            LazyVGrid(columns: columns, spacing: 12) {
                overviewCard(
                    title: "Patients seen",
                    value: "\(overview.patientCount)",
                    subtitle: "Distinct horses with activity",
                    accessibilityLabel: "Patients seen, \(overview.patientCount)"
                )
                overviewCard(
                    title: "Recorded activities",
                    value: "\(overview.activityCount)",
                    subtitle: "Active dated records",
                    accessibilityLabel: "Recorded activities, \(overview.activityCount)"
                )
                overviewCard(
                    title: "Case-days",
                    value: "\(overview.caseDayCount)",
                    subtitle: "Patient · day pairs",
                    accessibilityLabel: "Case-days, \(overview.caseDayCount)"
                )
                overviewCard(
                    title: "Active days",
                    value: "\(overview.activeDayCount)",
                    subtitle: "Distinct dates with activity",
                    accessibilityLabel: "Active days, \(overview.activeDayCount)"
                )
            }

            // Averages row — null when denominator zero, never 0/Inf/NaN
            HStack(spacing: 12) {
                averageCard(
                    title: "Avg per active day",
                    value: overview.averagePerActiveDay,
                    accessibilityHint: overview.averagePerActiveDay == nil ? "Unavailable, no active days in period" : nil
                )
                averageCard(
                    title: "Avg per case-day",
                    value: overview.averagePerCaseDay,
                    accessibilityHint: overview.averagePerCaseDay == nil ? "Unavailable, no case-days in period" : nil
                )
            }

            // Comparison delta — only when Kotlin supplies it (disabled for all-time)
            comparisonRow

            if onOpenSourceRecords != nil {
                Button {
                    onOpenSourceRecords?()
                } label: {
                    Label("View source records", systemImage: "doc.text.magnifyingglass")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("insights_overview_source_records")
            }
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityIdentifier("insights_overview_section")
    }

    // MARK: - Cards

    private func overviewCard(title: String, value: String, subtitle: String, accessibilityLabel: String) -> some View {
        Button {
            onOpenSourceRecords?()
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Text(value)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Theme.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(onOpenSourceRecords == nil)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint(onOpenSourceRecords == nil ? "" : "Shows source records")
    }

    @ViewBuilder
    private func averageCard(title: String, value: KotlinDouble?, accessibilityHint: String?) -> some View {
        let isAvailable = value != nil
        let displayValue: String = {
            guard let v = value else { return "—" }
            return String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), v.doubleValue)
        }()
        if isAvailable, onOpenSourceRecords != nil {
            Button {
                onOpenSourceRecords?()
            } label: {
                averageCardContent(title: title, displayValue: displayValue, isAvailable: true)
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(title), \(displayValue)")
        } else {
            averageCardContent(title: title, displayValue: displayValue, isAvailable: isAvailable)
                .accessibilityElement(children: .combine)
                .accessibilityLabel(isAvailable ? "\(title), \(displayValue)" : "\(title), unavailable")
                .accessibilityHint(accessibilityHint ?? "")
        }
    }

    private func averageCardContent(
        title: String,
        displayValue: String,
        isAvailable: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
            Text(displayValue)
                .font(.title3.weight(.bold))
                .foregroundStyle(isAvailable ? Theme.textPrimary : Theme.textSecondary)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(isAvailable ? "Activities per day" : "Unavailable")
                .font(.caption2)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        .contentShape(Rectangle())
    }

    // MARK: - Comparison

    @ViewBuilder
    private var comparisonRow: some View {
        if !isComparisonEnabled {
            HStack(spacing: 6) {
                Image(systemName: "arrow.left.arrow.right")
                    .font(.caption2)
                Text("Comparison disabled for all time")
                    .font(.caption)
            }
            .foregroundStyle(Theme.textTertiary)
            .accessibilityLabel("Comparison disabled for all time range")
        } else if let comparison = overview.comparison {
            HStack(spacing: 10) {
                Image(systemName: deltaIcon(for: comparison.absoluteDelta))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(deltaColor(for: comparison.absoluteDelta))
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(deltaText(for: comparison))
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Theme.textPrimary)
                        if let pct = comparison.percentageDelta {
                            Text(String(format: "(%+.1f%%)", locale: Locale(identifier: "en_US_POSIX"), pct.doubleValue))
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(deltaColor(for: comparison.absoluteDelta))
                                .accessibilityLabel(percentageAccessibility(pct))
                        } else {
                            Text("—%")
                                .font(.caption2)
                                .foregroundStyle(Theme.textTertiary)
                                .accessibilityLabel("Percentage unavailable, previous period was empty")
                        }
                    }
                    Text("vs previous \(comparison.previous) in prior period")
                        .font(.caption2)
                        .foregroundStyle(Theme.textSecondary)
                }
                Spacer()
            }
            .padding(.top, 4)
            .accessibilityElement(children: .combine)
            .accessibilityLabel(comparisonAccessibility(comparison))
        }
    }

    private func deltaText(for c: MetricComparison) -> String {
        let sign = c.absoluteDelta >= 0 ? "+" : ""
        return "\(sign)\(c.absoluteDelta) vs prior"
    }

    private func deltaIcon(for delta: Int32) -> String {
        if delta > 0 { return "arrow.up.right" }
        if delta < 0 { return "arrow.down.right" }
        return "arrow.right"
    }

    private func deltaColor(for delta: Int32) -> Color {
        if delta > 0 { return Theme.forestGreen }
        if delta < 0 { return .red }
        return Theme.textTertiary
    }

    private func percentageAccessibility(_ pct: KotlinDouble) -> String {
        let v = pct.doubleValue
        return String(format: "%+.1f percent", locale: Locale(identifier: "en_US_POSIX"), v)
    }

    private func comparisonAccessibility(_ c: MetricComparison) -> String {
        let delta = c.absoluteDelta
        let pctText: String
        if let pct = c.percentageDelta {
            pctText = String(format: "%+.1f percent", locale: Locale(identifier: "en_US_POSIX"), pct.doubleValue)
        } else {
            pctText = "percentage unavailable"
        }
        return "Change \(delta), \(pctText), previous period had \(c.previous)"
    }
}
