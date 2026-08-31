import SwiftUI
import Charts
import Shared

// MARK: - Case mix: activity trend + record-type distribution
//
// No calculation in Swift. Both charts read straight from
// `activitySeries` and `recordMix` supplied by the use case. Each chart
// shows textual values alongside colour, carries full VoiceOver labels, and
// degrades to a textual list when the series is empty.

struct InsightsCaseMixSection: View {
    let activitySeries: [ActivityPoint]
    let recordMix: [RecordTypeCount]
    let totalActivityCount: Int32
    var isDrillDownEnabled: Bool = true
    var onSelectRecordType: ((RecordType) -> Void)? = nil
    @State private var selectedType: RecordType?

    // Stable expanded palette — 18 distinct hues, colour never sole communicator.
    private static let palette: [Color] = {
        var base: [Color] = [
            Color(red: 0.11, green: 0.35, blue: 0.29),
            Color(red: 0.09, green: 0.39, blue: 0.65),
            Color(red: 0.61, green: 0.30, blue: 0.20),
            Color(red: 0.48, green: 0.24, blue: 0.45),
            Color(red: 0.85, green: 0.65, blue: 0.13),
            Color(red: 0.26, green: 0.35, blue: 0.47),
            Color(red: 0.20, green: 0.55, blue: 0.45),
            Color(red: 0.75, green: 0.45, blue: 0.20),
        ]
        // Generate remaining via hue for distinctness
        for i in base.count..<18 {
            let hue = Double(i) / 18.0
            base.append(Color(hue: hue, saturation: 0.62, brightness: 0.60))
        }
        return base
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header
            activityChart
            Divider().opacity(0.6)
            recordMixBlock
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityIdentifier("insights_case_mix_section")
    }

    private var header: some View {
        HStack(spacing: 8) {
            Image(systemName: "chart.pie.fill")
                .foregroundStyle(Theme.forestGreen)
            Text("Case mix")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Spacer()
        }
        .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Activity trend chart

    @ViewBuilder
    private var activityChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Activity trend")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .accessibilityAddTraits(.isHeader)

            if activitySeries.isEmpty {
                Text("No activity in this period")
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 24)
                    .accessibilityLabel("Activity trend empty")
            } else {
                Chart {
                    ForEach(activitySeries, id: \.periodStart) { point in
                        BarMark(
                            x: .value("Period", point.periodStart.swiftDate),
                            y: .value("Activities", point.count)
                        )
                        .foregroundStyle(Theme.forestGreen.gradient)
                        .cornerRadius(4)
                        .accessibilityLabel("\(point.periodStart.friendlyString), \(point.count) activities")
                        .accessibilityValue("\(point.count)")
                    }
                }
                .frame(height: 160)
                .chartXAxis {
                    AxisMarks(values: .automatic(desiredCount: 4)) { value in
                        AxisValueLabel {
                            if let date = value.as(Date.self) {
                                Text(shortLabel(for: date))
                                    .font(.caption2)
                                    .foregroundStyle(Theme.textTertiary)
                            }
                        }
                        AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                            .foregroundStyle(Color(.separator).opacity(0.4))
                    }
                }
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisValueLabel {
                            if let v = value.as(Int.self) {
                                Text("\(v)")
                                    .font(.caption2)
                                    .foregroundStyle(Theme.textTertiary)
                            }
                        }
                        AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                            .foregroundStyle(Color(.separator).opacity(0.4))
                    }
                }
                .accessibilityLabel("Activity trend, \(activitySeries.count) buckets")

                // Textual fallback directly beneath chart — colour not sole channel
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(activitySeries, id: \.periodStart) { p in
                            VStack(spacing: 2) {
                                Text(p.periodStart.displayString)
                                    .font(.caption2)
                                    .foregroundStyle(Theme.textSecondary)
                                Text("\(p.count)")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(Theme.textPrimary)
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .background(Color(.systemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .accessibilityElement(children: .combine)
                            .accessibilityLabel("\(p.periodStart.friendlyString): \(p.count)")
                        }
                    }
                }
            }
        }
    }

    private static let shortLabelFormatter: DateFormatter = {
        let fmt = DateFormatter()
        fmt.dateFormat = "dd MMM"
        fmt.locale = Locale(identifier: "en_US_POSIX")
        return fmt
    }()

    private func shortLabel(for date: Date) -> String {
        Self.shortLabelFormatter.string(from: date)
    }

    // MARK: - Record mix

    @ViewBuilder
    private var recordMixBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Record mix")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .accessibilityAddTraits(.isHeader)

            if recordMix.isEmpty {
                Text("No records in this period")
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 16)
                    .accessibilityLabel("Record mix empty")
            } else {
                // Donut + legend: textual count+share always visible, not colour-only
                Chart {
                    ForEach(Array(recordMix.enumerated()), id: \.element.type) { index, item in
                        SectorMark(
                            angle: .value("Count", item.count),
                            innerRadius: .ratio(0.62),
                            angularInset: 1.5
                        )
                        .foregroundStyle(Self.palette[index % Self.palette.count])
                        .opacity(selectedType == item.type ? 1.0 : 0.92)
                        .cornerRadius(3)
                        .accessibilityLabel("\(item.type.displayName), \(item.count), \(shareText(for: item.share))")
                    }
                }
                .frame(height: 180)
                .animation(.easeInOut(duration: 0.2), value: selectedType)
                .chartBackground { _ in
                    VStack(spacing: 2) {
                        Text("\(totalActivityCount)")
                            .font(.title3.weight(.bold))
                            .foregroundStyle(Theme.textPrimary)
                        Text("activities")
                            .font(.caption2)
                            .foregroundStyle(Theme.textSecondary)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("Total \(totalActivityCount) activities")
                }
                .accessibilityLabel("Record mix, \(recordMix.count) types, \(totalActivityCount) total activities")

                // Legend list — tap to drill down (Task 9 wires to records)
                VStack(spacing: 0) {
                    ForEach(Array(recordMix.enumerated()), id: \.element.type) { index, item in
                        Button {
                            selectedType = item.type
                            onSelectRecordType?(item.type)
                        } label: {
                            HStack(spacing: 10) {
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Self.palette[index % Self.palette.count])
                                    .frame(width: 12, height: 12)
                                    .accessibilityHidden(true)
                                Text(item.type.displayName)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(Theme.textPrimary)
                                    .lineLimit(1)
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text("\(item.count)")
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(Theme.textPrimary)
                                    Text(shareText(for: item.share))
                                        .font(.caption2)
                                        .foregroundStyle(Theme.textSecondary)
                                }
                                Image(systemName: "chevron.right")
                                    .font(.caption2.weight(.semibold))
                                    .foregroundStyle(Theme.textTertiary)
                            }
                            .padding(.vertical, 8)
                            .padding(.horizontal, 4)
                            .contentShape(Rectangle())
                            .background(selectedType == item.type ? Theme.forestGreen.opacity(0.08) : Color.clear)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                        .disabled(!isDrillDownEnabled)
                        .opacity(isDrillDownEnabled ? 1.0 : 0.52)
                        .accessibilityLabel("\(item.type.displayName), \(item.count) activities, \(shareText(for: item.share))")
                        .accessibilityHint(isDrillDownEnabled ? "Shows records for \(item.type.displayName)" : "Unavailable for current range")
                        .accessibilityAddTraits(.isButton)
                        if index < recordMix.count - 1 {
                            Divider().opacity(0.5)
                        }
                    }
                }
            }
        }
    }

    private func shareText(for share: KotlinDouble?) -> String {
        guard let s = share else { return "—" }
        return String(format: "%.1f%%", locale: Locale(identifier: "en_US_POSIX"), s.doubleValue * 100)
    }
}
