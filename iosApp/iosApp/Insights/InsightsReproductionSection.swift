import Shared
import SwiftUI

/// Auditable reproduction-period facts calculated by shared Kotlin.
///
/// Counts and averages are rendered exactly as supplied by `ReproductionMetrics`;
/// Swift owns only formatting and navigation. Every non-empty metric can open the
/// persisted records behind it, and no unsupported outcome/success rate is shown.
/// RecordType literals use SKIE lower-case names (.reproductionevent/.embryotransfer/.icsi) verified against Shared.h swift_name.
struct InsightsReproductionSection: View {
    let metrics: ReproductionMetrics
    var isDrillDownEnabled = true
    var onOpenRecords: ((RecordType, ReproductionEventType?) -> Void)?

    private let columns = [
        GridItem(.flexible(minimum: 142), spacing: 10),
        GridItem(.flexible(minimum: 142), spacing: 10),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            eventCounts
            Divider().opacity(0.6)
            procedureMetrics
            Text("Period facts only — no conception or outcome rates are inferred.")
                .font(.caption2)
                .foregroundStyle(Theme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityLabel("No conception or outcome rates are inferred")
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityIdentifier("insights_reproduction_section")
    }

    private var header: some View {
        HStack(spacing: 8) {
            Image(systemName: "heart.text.clipboard.fill")
                .foregroundStyle(Theme.forestGreen)
            Text("Reproduction activity")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Spacer()
        }
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder
    private var eventCounts: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Events")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)

            if metrics.eventCounts.isEmpty {
                Text("No reproduction events in this period")
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 10)
            } else {
                ForEach(metrics.eventCounts, id: \.type) { item in
                    Button {
                        onOpenRecords?(.reproductionevent, item.type)
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "heart.circle.fill")
                                .foregroundStyle(Theme.forestGreen)
                                .accessibilityHidden(true)
                            Text(item.type.displayLabel)
                                .font(.subheadline)
                                .foregroundStyle(Theme.textPrimary)
                            Spacer()
                            Text("\(item.count)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Theme.textPrimary)
                            Image(systemName: "chevron.right")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(Theme.textTertiary)
                        }
                        .padding(.vertical, 7)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(!isDrillDownEnabled)
                    .opacity(isDrillDownEnabled ? 1 : 0.52)
                    .accessibilityLabel("\(item.type.displayLabel), \(item.count) events")
                    .accessibilityHint("Shows matching reproduction records")
                    .accessibilityIdentifier("insights_reproduction_event_\(item.type.storageLabel.replacingOccurrences(of: " ", with: "_"))")
                }
            }
        }
    }

    private var procedureMetrics: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            metricCard(
                title: "Embryo collections",
                value: "\(metrics.embryoCollections)",
                detail: "\(metrics.embryosCollected) embryos recorded",
                recordType: .embryotransfer,
                isAvailable: metrics.embryoCollections > 0
            )
            metricCard(
                title: "Embryos / collection",
                value: formatted(metrics.averageEmbryosPerCollection),
                detail: metrics.averageEmbryosPerCollection == nil ? "No collections" : "Across \(metrics.embryoCollections) collections",
                recordType: .embryotransfer,
                isAvailable: metrics.embryoCollections > 0
            )
            metricCard(
                title: "ICSI sessions",
                value: "\(metrics.icsiSessions)",
                detail: "\(metrics.folliclesRecovered) follicles recovered",
                recordType: .icsi,
                isAvailable: metrics.icsiSessions > 0
            )
            metricCard(
                title: "Follicles / ICSI",
                value: formatted(metrics.averageFolliclesPerIcsi),
                detail: metrics.averageFolliclesPerIcsi == nil ? "No ICSI sessions" : "Across \(metrics.icsiSessions) sessions",
                recordType: .icsi,
                isAvailable: metrics.icsiSessions > 0
            )
            metricCard(
                title: "Ultrasounds",
                value: "\(metrics.ultrasoundCount)",
                detail: "Examinations in period",
                recordType: .ultrasound,
                isAvailable: metrics.ultrasoundCount > 0
            )
        }
    }

    @ViewBuilder
    private func metricCard(
        title: String,
        value: String,
        detail: String,
        recordType: RecordType,
        isAvailable: Bool
    ) -> some View {
        if isAvailable && isDrillDownEnabled {
            Button {
                onOpenRecords?(recordType, nil)
            } label: {
                metricCardContent(title: title, value: value, detail: detail, isAvailable: true)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("\(title), \(value), \(detail)")
            .accessibilityHint("Shows source records")
        } else {
            metricCardContent(title: title, value: value, detail: detail, isAvailable: isAvailable)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("\(title), \(value), \(detail)")
                .accessibilityHint(isAvailable ? "Source records unavailable for this range" : "No source records in this period")
        }
    }

    private func metricCardContent(
        title: String,
        value: String,
        detail: String,
        isAvailable: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Text(value)
                .font(.title3.weight(.bold))
                .foregroundStyle(isAvailable ? Theme.textPrimary : Theme.textSecondary)
                .lineLimit(1)
                .minimumScaleFactor(0.65)
            Text(detail)
                .font(.caption2)
                .foregroundStyle(Theme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, minHeight: 90, alignment: .leading)
        .padding(11)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        .contentShape(Rectangle())
    }

    private func formatted(_ value: KotlinDouble?) -> String {
        guard let value else { return "—" }
        return String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), value.doubleValue)
    }
}
