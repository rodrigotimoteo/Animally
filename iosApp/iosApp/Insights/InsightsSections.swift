import SwiftUI
import Charts
import Shared

// MARK: - Overview

struct InsightsOverviewSection: View {
    let overview: OverviewMetrics
    let isComparisonEnabled: Bool
    var onOpenSourceRecords: (() -> Void)?
    private let columns = [GridItem(.flexible(minimum: 148), spacing: 12), GridItem(.flexible(minimum: 148), spacing: 12)]
    var body: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 12) {
                InsightSectionHeader(systemImage: "chart.bar.doc.horizontal", title: "Internship overview")
                LazyVGrid(columns: columns, spacing: 12) {
                    InsightStatCard(title: "Patients seen", value: "\(overview.patientCount)", subtitle: "Distinct horses with activity", action: onOpenSourceRecords)
                    InsightStatCard(title: "Recorded activities", value: "\(overview.activityCount)", subtitle: "Active dated records", action: onOpenSourceRecords)
                    InsightStatCard(title: "Case-days", value: "\(overview.caseDayCount)", subtitle: "Patient · day pairs", action: onOpenSourceRecords)
                    InsightStatCard(title: "Active days", value: "\(overview.activeDayCount)", subtitle: "Distinct dates with activity", action: onOpenSourceRecords)
                }
                HStack(spacing: 12) {
                    avgCard(title: "Avg per active day", value: overview.averagePerActiveDay)
                    avgCard(title: "Avg per case-day", value: overview.averagePerCaseDay)
                }
                comparisonRow
                if onOpenSourceRecords != nil {
                    Button { onOpenSourceRecords?() } label: {
                        Label("View source records", systemImage: "doc.text.magnifyingglass").font(.caption.weight(.semibold)).foregroundStyle(Theme.forestGreen).frame(maxWidth: .infinity, alignment: .trailing)
                    }.buttonStyle(.plain).accessibilityIdentifier("insights_overview_source_records").accessibilityLabel("View source records").accessibilityHint("Shows records for this period")
                }
            }
        }.accessibilityIdentifier("insights_overview_section").accessibilityElement(children: .contain)
    }
    @ViewBuilder private func avgCard(title: String, value: KotlinDouble?) -> some View {
        let disp: String = value.map { String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), $0.doubleValue) } ?? "—"
        let avail = value != nil
        InsightStatCard(title: title, value: disp, subtitle: avail ? "Activities per day" : "Unavailable", isAvailable: avail, action: avail ? onOpenSourceRecords : nil)
    }
    @ViewBuilder private var comparisonRow: some View {
        if !isComparisonEnabled {
            HStack(spacing: 6) { Image(systemName: "arrow.left.arrow.right").font(.caption2).accessibilityHidden(true); Text("Comparison disabled for all time").font(.caption) }.foregroundStyle(Theme.textTertiary).accessibilityLabel("Comparison disabled for all time range")
        } else if let c = overview.comparison {
            HStack(spacing: 10) {
                Image(systemName: deltaIcon(c.absoluteDelta)).font(.caption.weight(.bold)).foregroundStyle(deltaColor(c.absoluteDelta)).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(deltaText(c)).font(.caption.weight(.semibold)).foregroundStyle(Theme.textPrimary)
                        if let pct = c.percentageDelta { Text(String(format: "(%+.1f%%)", locale: Locale(identifier: "en_US_POSIX"), pct.doubleValue)).font(.caption2.weight(.medium)).foregroundStyle(deltaColor(c.absoluteDelta)) }
                        else { Text("—%").font(.caption2).foregroundStyle(Theme.textTertiary) }
                    }
                    Text("vs previous \(c.previous) in prior period").font(.caption2).foregroundStyle(Theme.textSecondary)
                }
                Spacer()
            }.padding(.top, 4).accessibilityElement(children: .combine).accessibilityLabel(comparisonA11y(c))
        }
    }
    private func deltaText(_ c: MetricComparison) -> String { let s = c.absoluteDelta >= 0 ? "+" : ""; return "\(s)\(c.absoluteDelta) vs prior" }
    private func deltaIcon(_ d: Int32) -> String { d > 0 ? "arrow.up.right" : d < 0 ? "arrow.down.right" : "arrow.right" }
    private func deltaColor(_ d: Int32) -> Color { d > 0 ? Theme.forestGreen : d < 0 ? .red : Theme.textTertiary }
    private func comparisonA11y(_ c: MetricComparison) -> String {
        let pct: String = c.percentageDelta.map { String(format: "%+.1f percent", locale: Locale(identifier: "en_US_POSIX"), $0.doubleValue) } ?? "percentage unavailable"
        return "Change \(c.absoluteDelta), \(pct), previous period had \(c.previous)"
    }
}

// MARK: - CaseMix — single render path per data set

struct InsightsCaseMixSection: View {
    let activitySeries: [ActivityPoint]
    let recordMix: [RecordTypeCount]
    let totalActivityCount: Int32
    var isDrillDownEnabled: Bool = true
    var onSelectRecordType: ((RecordType) -> Void)? = nil
    @State private var selectedType: RecordType?
    var body: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 16) {
                InsightSectionHeader(systemImage: "chart.pie.fill", title: "Case mix")
                activityChart
                Divider().opacity(0.6)
                recordMixBlock
            }
        }.accessibilityIdentifier("insights_case_mix_section").accessibilityElement(children: .contain)
    }
    @ViewBuilder private var activityChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Activity trend").font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary).accessibilityAddTraits(.isHeader)
            if activitySeries.isEmpty {
                Text("No activity in this period").font(.caption).foregroundStyle(Theme.textTertiary).frame(maxWidth: .infinity, alignment: .center).padding(.vertical, 24).accessibilityLabel("Activity trend empty")
            } else {
                Chart { ForEach(activitySeries, id: \.periodStart) { p in BarMark(x: .value("Period", p.periodStart.swiftDate), y: .value("Activities", p.count)).foregroundStyle(Theme.forestGreen.gradient).cornerRadius(4) } }
                .frame(height: 160)
                .chartXAxis { AxisMarks(values: .automatic(desiredCount: 4)) { v in AxisValueLabel { if let d = v.as(Date.self) { Text(Self.shortLabel(d)).font(.caption2).foregroundStyle(Theme.textTertiary) } }; AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5)).foregroundStyle(Color(.separator).opacity(0.4)) } }
                .chartYAxis { AxisMarks(position: .leading) { v in AxisValueLabel { if let i = v.as(Int.self) { Text("\(i)").font(.caption2).foregroundStyle(Theme.textTertiary) } }; AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5)).foregroundStyle(Color(.separator).opacity(0.4)) } }
                .accessibilityLabel("Activity trend, \(activitySeries.count) buckets")
                .accessibilityHidden(false)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) { ForEach(activitySeries, id: \.periodStart) { p in VStack(spacing: 2) { Text(p.periodStart.displayString).font(.caption2).foregroundStyle(Theme.textSecondary); Text("\(p.count)").font(.caption.weight(.semibold)).foregroundStyle(Theme.textPrimary) }.padding(.horizontal, 8).padding(.vertical, 6).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 8)).accessibilityElement(children: .combine).accessibilityLabel("\(p.periodStart.friendlyString): \(p.count) activities") } }
                }.accessibilityLabel("Activity buckets")
            }
        }
    }
    private static func shortLabel(_ d: Date) -> String { DateFormatters.short.string(from: d) }
    @ViewBuilder private var recordMixBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Record mix").font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary).accessibilityAddTraits(.isHeader)
            if recordMix.isEmpty {
                Text("No records in this period").font(.caption).foregroundStyle(Theme.textTertiary).frame(maxWidth: .infinity, alignment: .center).padding(.vertical, 16).accessibilityLabel("Record mix empty")
            } else {
                // Single data source rendered once as chart + once as list — not double render of same view tree
                Chart { ForEach(Array(recordMix.enumerated()), id: \.element.type) { idx, item in SectorMark(angle: .value("Count", item.count), innerRadius: .ratio(0.62), angularInset: 1.5).foregroundStyle(Theme.chartPalette[idx % Theme.chartPalette.count]).opacity(selectedType == item.type ? 1 : 0.92).cornerRadius(3) } }
                .frame(height: 180).animation(.easeInOut(duration: 0.2), value: selectedType)
                .chartBackground { _ in VStack(spacing: 2) { Text("\(totalActivityCount)").font(.title3.weight(.bold)).foregroundStyle(Theme.textPrimary); Text("activities").font(.caption2).foregroundStyle(Theme.textSecondary) }.accessibilityElement(children: .combine).accessibilityLabel("Total \(totalActivityCount) activities") }
                .accessibilityLabel("Record mix, \(recordMix.count) types, \(totalActivityCount) total activities")
                VStack(spacing: 0) {
                    ForEach(Array(recordMix.enumerated()), id: \.element.type) { idx, item in
                        Button { selectedType = item.type; onSelectRecordType?(item.type) } label: {
                            HStack(spacing: 10) {
                                RoundedRectangle(cornerRadius: 3).fill(Theme.chartPalette[idx % Theme.chartPalette.count]).frame(width: 12, height: 12).accessibilityHidden(true)
                                Text(item.type.displayName).font(.subheadline.weight(.medium)).foregroundStyle(Theme.textPrimary).lineLimit(1)
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) { Text("\(item.count)").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textPrimary); Text(shareText(item.share)).font(.caption2).foregroundStyle(Theme.textSecondary) }
                                Image(systemName: "chevron.right").font(.caption2.weight(.semibold)).foregroundStyle(Theme.textTertiary).accessibilityHidden(true)
                            }.padding(.vertical, 8).padding(.horizontal, 4).contentShape(Rectangle()).background(selectedType == item.type ? Theme.forestGreen.opacity(0.08) : Color.clear).clipShape(RoundedRectangle(cornerRadius: 8))
                        }.buttonStyle(.plain).disabled(!isDrillDownEnabled).opacity(isDrillDownEnabled ? 1 : 0.52).accessibilityLabel("\(item.type.displayName), \(item.count) activities, \(shareText(item.share))").accessibilityHint(isDrillDownEnabled ? "Shows records for \(item.type.displayName)" : "Unavailable for current range").accessibilityAddTraits(.isButton)
                        if idx < recordMix.count - 1 { Divider().opacity(0.5) }
                    }
                }.accessibilityLabel("Record mix list")
            }
        }
    }
    private func shareText(_ s: KotlinDouble?) -> String { guard let v = s else { return "—" }; return String(format: "%.1f%%", locale: Locale(identifier: "en_US_POSIX"), v.doubleValue*100) }
}

// MARK: - Reproduction

struct InsightsReproductionSection: View {
    let metrics: ReproductionMetrics
    var isDrillDownEnabled = true
    var onOpenRecords: ((RecordType, ReproductionEventType?) -> Void)?
    private let cols = [GridItem(.flexible(minimum: 142), spacing: 10), GridItem(.flexible(minimum: 142), spacing: 10)]
    var body: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 14) {
                InsightSectionHeader(systemImage: "heart.text.clipboard.fill", title: "Reproduction activity")
                eventCounts
                Divider().opacity(0.6)
                procedureMetrics
                Text("Period facts only — no conception or outcome rates are inferred.").font(.caption2).foregroundStyle(Theme.textSecondary).fixedSize(horizontal: false, vertical: true).accessibilityLabel("No conception or outcome rates are inferred")
            }
        }.accessibilityIdentifier("insights_reproduction_section").accessibilityElement(children: .contain)
    }
    @ViewBuilder private var eventCounts: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Events").font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary).accessibilityAddTraits(.isHeader)
            if metrics.eventCounts.isEmpty {
                Text("No reproduction events in this period").font(.caption).foregroundStyle(Theme.textTertiary).frame(maxWidth: .infinity, alignment: .center).padding(.vertical, 10).accessibilityLabel("No reproduction events")
            } else {
                ForEach(metrics.eventCounts, id: \.type) { item in
                    Button { onOpenRecords?(.reproductionevent, item.type) } label: {
                        HStack(spacing: 10) { RecordBadgeIcon(systemName: "heart.circle.fill", size: 28, corner: 7); Text(item.type.displayLabel).font(.subheadline).foregroundStyle(Theme.textPrimary); Spacer(); Text("\(item.count)").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textPrimary); Image(systemName: "chevron.right").font(.caption2.weight(.semibold)).foregroundStyle(Theme.textTertiary).accessibilityHidden(true) }.padding(.vertical, 7).contentShape(Rectangle())
                    }.buttonStyle(.plain).disabled(!isDrillDownEnabled).opacity(isDrillDownEnabled ? 1 : 0.52).accessibilityLabel("\(item.type.displayLabel), \(item.count) events").accessibilityHint("Shows matching reproduction records").accessibilityIdentifier("insights_reproduction_event_\(item.type.storageLabel.replacingOccurrences(of: " ", with: "_"))")
                }
            }
        }
    }
    private var procedureMetrics: some View {
        LazyVGrid(columns: cols, spacing: 10) {
            metric(title: "Embryo collections", value: "\(metrics.embryoCollections)", detail: "\(metrics.embryosCollected) embryos recorded", type: .embryotransfer, avail: metrics.embryoCollections>0)
            metric(title: "Embryos / collection", value: fmt(metrics.averageEmbryosPerCollection), detail: metrics.averageEmbryosPerCollection == nil ? "No collections" : "Across \(metrics.embryoCollections) collections", type: .embryotransfer, avail: metrics.embryoCollections>0)
            metric(title: "ICSI sessions", value: "\(metrics.icsiSessions)", detail: "\(metrics.folliclesRecovered) follicles recovered", type: .icsi, avail: metrics.icsiSessions>0)
            metric(title: "Follicles / ICSI", value: fmt(metrics.averageFolliclesPerIcsi), detail: metrics.averageFolliclesPerIcsi == nil ? "No ICSI sessions" : "Across \(metrics.icsiSessions) sessions", type: .icsi, avail: metrics.icsiSessions>0)
            metric(title: "Ultrasounds", value: "\(metrics.ultrasoundCount)", detail: "Examinations in period", type: .ultrasound, avail: metrics.ultrasoundCount>0)
        }
    }
    @ViewBuilder private func metric(title: String, value: String, detail: String, type: RecordType, avail: Bool) -> some View {
        InsightStatCard(title: title, value: value, subtitle: detail, isAvailable: avail, action: (avail && isDrillDownEnabled) ? { onOpenRecords?(type, nil) } : nil)
    }
    private func fmt(_ v: KotlinDouble?) -> String { guard let x = v else { return "—" }; return String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), x.doubleValue) }
}

// MARK: - Gestation

struct InsightsGestationSection: View {
    let currentCare: CurrentCareSnapshot
    var onOpenGestation: ((Int64, Int64) -> Void)? = nil
    var body: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 12) {
                header
                summaryRow
                if currentCare.activeGestations.isEmpty { empty } else { list }
            }
        }.accessibilityElement(children: .contain).accessibilityIdentifier("insights_gestation_section")
    }
    private var header: some View {
        InsightSectionHeader(systemImage: "heart.circle.fill", title: "Current gestations", countText: "\(currentCare.activeGestations.count) active")
            .accessibilityLabel("Active gestations \(currentCare.activeGestations.count)")
    }
    private var summaryRow: some View {
        HStack(spacing: 12) {
            pill(title: "≤30d", count: Int(currentCare.dueSoon30.count), color: Theme.amber)
            pill(title: "≤60d", count: Int(currentCare.dueSoon60.count), color: Theme.forestGreen)
            pill(title: "≤90d", count: Int(currentCare.dueSoon90.count), color: Theme.textSecondary)
            Spacer()
        }.accessibilityElement(children: .combine).accessibilityLabel("Due in ≤30 days \(currentCare.dueSoon30.count), ≤60 days \(currentCare.dueSoon60.count), ≤90 days \(currentCare.dueSoon90.count)")
    }
    private func pill(title: String, count: Int, color: Color) -> some View {
        VStack(spacing: 2) { Text("\(count)").font(.title3.weight(.bold)).foregroundStyle(color).lineLimit(1).minimumScaleFactor(0.6); Text(title).font(.caption2.weight(.medium)).foregroundStyle(Theme.textSecondary).lineLimit(1) }
            .frame(minWidth: 52).padding(.vertical, 8).padding(.horizontal, 10).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 10)).shadow(color: .black.opacity(0.04), radius: 4, y: 1).accessibilityElement(children: .combine).accessibilityLabel("\(title), \(count)")
    }
    private var empty: some View {
        HStack(spacing: 10) { Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.forestGreen.opacity(0.35)).accessibilityHidden(true); Text("No active gestations").font(.subheadline).foregroundStyle(Theme.textSecondary); Spacer() }
            .padding(12).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 10)).accessibilityLabel("No active gestations")
    }
    private var list: some View { VStack(spacing: 8) { ForEach(currentCare.activeGestations, id: \.gestationId) { gestationRow($0) } } }
    private func gestationRow(_ item: CurrentGestationItem) -> some View {
        let days = Int(item.daysUntilDue); let overdue = days < 0; let dueSoon = days >= 0 && days <= 30; let accent: Color = overdue || dueSoon ? Theme.amber : Theme.forestGreen; let dueLabel = GestationDueText.daysLabel(daysUntilDue: days)
        return Button { onOpenGestation?(item.patientId, item.gestationId) } label: {
            HStack(alignment: .top, spacing: 12) {
                RecordBadgeIcon(systemName: "heart.fill", tint: accent)
                VStack(alignment: .leading, spacing: 3) {
                    Text(item.patientName).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textPrimary).lineLimit(1)
                    HStack(spacing: 6) { Text("Day \(item.gestationDay)").font(.caption.weight(.medium)).foregroundStyle(Theme.textSecondary); Text("·").foregroundStyle(Theme.textTertiary).accessibilityHidden(true); Text(item.status.isEmpty ? "—" : item.status).font(.caption).foregroundStyle(Theme.textSecondary).lineLimit(1) }
                    Text("Due \(item.dueDate.friendlyString) · \(dueLabel)").font(.caption2.weight(.medium)).foregroundStyle(accent).lineLimit(1)
                }
                Spacer(); Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(Theme.textTertiary).accessibilityHidden(true)
            }.padding(10).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 10)).shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        }.buttonStyle(.plain).accessibilityElement(children: .combine).accessibilityLabel("\(item.patientName), day \(item.gestationDay), \(GestationDueText.daysLabel(daysUntilDue: days)), \(item.dueDate.friendlyString)").accessibilityHint("Opens gestation record").accessibilityIdentifier("insights_gestation_\(item.gestationId)")
    }
}

// MARK: - Readiness

struct InsightsReadinessSection: View {
    let dataIssues: [InsightsDataIssueCount]
    var isDrillDownEnabled = true
    var onSelectIssue: ((InsightsDataIssueType) -> Void)?
    private var total: Int { dataIssues.reduce(0){$0+Int($1.count)} }
    var body: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 12) {
                header
                if dataIssues.isEmpty { empty } else { list }
                footer
            }
        }.accessibilityElement(children: .contain).accessibilityIdentifier("insights_readiness_section")
    }
    private var header: some View {
        InsightSectionHeader(systemImage: "exclamationmark.triangle.fill", title: "Research readiness", countText: "\(total) missing for analysis", tint: Theme.amber)
            .accessibilityLabel("Total missing for analysis \(total)").accessibilityElement(children: .combine)
    }
    private var list: some View {
        VStack(spacing: 0) {
            ForEach(dataIssues, id: \.type) { issue in
                Button { onSelectIssue?(issue.type) } label: {
                    HStack(spacing: 10) {
                        RecordBadgeIcon(systemName: iconFor(issue.type), tint: Theme.amber, size: 28, corner: 7)
                        VStack(alignment: .leading, spacing: 2) { Text(issue.type.displayName).font(.subheadline.weight(.medium)).foregroundStyle(Theme.textPrimary).lineLimit(1); Text(issue.type.missingForAnalysisLabel).font(.caption2).foregroundStyle(Theme.textSecondary).lineLimit(1) }
                        Spacer(); Text("\(issue.count)").font(.subheadline.weight(.bold)).foregroundStyle(Theme.textPrimary); Image(systemName: "chevron.right").font(.caption2.weight(.semibold)).foregroundStyle(Theme.textTertiary).accessibilityHidden(true)
                    }.padding(.vertical, 10).contentShape(Rectangle())
                }.buttonStyle(.plain).disabled(!isDrillDownEnabled).opacity(isDrillDownEnabled ? 1 : 0.52).accessibilityLabel("\(issue.type.displayName), \(issue.count) missing for analysis").accessibilityHint("Shows records missing for analysis").accessibilityIdentifier("insights_readiness_\(issue.type.name.lowercased())")
                if issue.type != dataIssues.last?.type { Divider().opacity(0.5) }
            }
        }.background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 10)).shadow(color: .black.opacity(0.04), radius: 4, y: 1)
    }
    private var empty: some View {
        HStack(spacing: 10) { Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.forestGreen).accessibilityHidden(true); Text("No research-readiness issues in this period").font(.subheadline).foregroundStyle(Theme.textSecondary); Spacer() }
            .padding(12).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 10)).accessibilityLabel("No research-readiness issues in this period")
    }
    private var footer: some View { Text("Records flagged as missing for analysis — not clinically wrong. Each row shows only the affected source records.").font(.caption2).foregroundStyle(Theme.textSecondary).fixedSize(horizontal: false, vertical: true).accessibilityLabel("Records flagged as missing for analysis, not clinically wrong") }
    private func iconFor(_ t: InsightsDataIssueType) -> String {
        switch t {
        case .unknownreproductioncategory: return "questionmark.circle.fill"
        case .missingvetname: return "stethoscope"
        case .unlinkedowner: return "person.crop.circle.badge.exclamationmark"
        case .freetextembryorecipient: return "arrow.triangle.branch"
        case .incompleteultrasounddata: return "waveform.path.ecg"
        default: return "exclamationmark.triangle.fill"
        }
    }
}
