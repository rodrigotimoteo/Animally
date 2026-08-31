import SwiftUI
import Shared

struct InsightsView: View {
    @StateObject private var viewModel: InsightsViewModel
    @State private var drillDownKey: InsightsDrillDownKey?
    @State private var gestationKey: RecordDetailKey?
    private let patientName: String?

    init(patientId: Int64? = nil, patientName: String? = nil) {
        self.patientName = patientName
        _viewModel = StateObject(wrappedValue: InsightsViewModel(patientId: patientId))
    }

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.snapshot == nil { loadingView }
            else if let snapshot = viewModel.state.snapshot { dashboardScroll(snapshot: snapshot) }
            else if let error = viewModel.state.errorMessage { errorEmptyView(message: error) }
            else if let validation = viewModel.state.validationError { validationEmptyView(message: validation) }
            else { loadingView }
        }
        .navigationTitle("Insights").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .topBarTrailing) { Button { viewModel.reload() } label: { Image(systemName: "arrow.clockwise").accessibilityLabel("Reload insights").accessibilityHint("Reloads dashboard") }.disabled(viewModel.state.isLoading) } }
        .overlay(alignment: .top) {
            VStack(spacing: 8) {
                if let e = viewModel.state.errorMessage { InlineErrorBanner(message: e, onRetry: { viewModel.retry() }, onDismiss: { viewModel.dismissError() }) }
                if let v = viewModel.state.validationError { InlineErrorBanner(message: v, onDismiss: { viewModel.dismissValidationError() }) }
                if let a = viewModel.awaitError { InlineErrorBanner(message: a, onRetry: { Task { _ = await viewModel.reloadAsync() } }, onDismiss: { viewModel.dismissAwaitError() }) }
            }.animation(.easeInOut(duration: 0.2), value: viewModel.state.errorMessage).animation(.easeInOut(duration: 0.2), value: viewModel.state.validationError)
        }
        .navigationDestination(item: $drillDownKey) { key in InsightsRecordsView(drillDown: key.drillDown, patientName: patientName) }
        .navigationDestination(item: $gestationKey) { key in RecordDetailView(displayType: key.displayType, patientId: key.patientId, recordId: key.recordId) }
    }

    private func dashboardScroll(snapshot: InsightsSnapshot) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                controlsSection
                InsightsOverviewSection(overview: snapshot.overview, isComparisonEnabled: viewModel.state.isComparisonEnabled, onOpenSourceRecords: { openDrillDown() }).accessibilityElement(children: .contain)
                if snapshot.overview.activityCount == 0 {
                    emptyView
                } else {
                    InsightsCaseMixSection(activitySeries: snapshot.activitySeries, recordMix: snapshot.recordMix, totalActivityCount: snapshot.overview.activityCount, isDrillDownEnabled: effectiveRange() != nil, onSelectRecordType: { openDrillDown(recordType: $0) }).accessibilityElement(children: .contain)
                }
                InsightsReproductionSection(metrics: snapshot.reproduction, isDrillDownEnabled: effectiveRange() != nil) { t, e in openDrillDown(recordType: t, reproductionEventType: e) }.accessibilityElement(children: .contain)
                InsightsGestationSection(currentCare: snapshot.currentCare) { pid, gid in gestationKey = RecordDetailKey(displayType: "GESTATION", patientId: pid, recordId: gid) }.accessibilityElement(children: .contain)
                InsightsReadinessSection(dataIssues: snapshot.dataIssues, isDrillDownEnabled: effectiveRange() != nil) { openDrillDown(dataIssueType: $0) }.accessibilityElement(children: .contain)
                if viewModel.state.isLoading { HStack(spacing: 8) { ProgressView().scaleEffect(0.8); Text("Updating…").font(.caption).foregroundStyle(Theme.textSecondary) }.frame(maxWidth: .infinity).padding(.vertical, 4).accessibilityLabel("Updating insights") }
            }.padding()
        }.accessibilityIdentifier("insights_dashboard").refreshable { _ = await viewModel.reloadAsync() }
    }

    private var controlsSection: some View {
        InsightCardContainer {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 6) {
                    Image(systemName: viewModel.state.patientId == nil ? "pawprint.fill" : "pawprint.circle.fill").font(.caption).foregroundStyle(Theme.forestGreen).accessibilityHidden(true)
                    Text(patientScopeLabel).font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary).lineLimit(1).minimumScaleFactor(0.7)
                    Spacer()
                    if let from = viewModel.state.from, let to = viewModel.state.to { Text("\(from.displayString) – \(to.displayString)").font(.caption2).foregroundStyle(Theme.textSecondary).lineLimit(1).minimumScaleFactor(0.6).accessibilityLabel("Range \(from.displayString) to \(to.displayString)") }
                    else if viewModel.state.preset == InsightsPreset.allTime { Text("All time").font(.caption2.weight(.medium)).foregroundStyle(Theme.textSecondary) }
                }.accessibilityElement(children: .combine).accessibilityLabel("Scope: \(patientScopeLabel)")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        InsightPillButton(title: "30 days", isSelected: viewModel.state.preset == .thirtyDays, identifier: "insights_preset_30_days", action: { viewModel.selectPreset(.thirtyDays) })
                        InsightPillButton(title: "90 days", isSelected: viewModel.state.preset == .ninetyDays, identifier: "insights_preset_90_days", action: { viewModel.selectPreset(.ninetyDays) })
                        InsightPillButton(title: "All time", isSelected: viewModel.state.preset == .allTime, identifier: "insights_preset_all_time", action: { viewModel.selectPreset(.allTime) })
                        InsightPillButton(title: "Custom", isSelected: viewModel.state.preset == .custom, identifier: "insights_preset_custom", action: { viewModel.selectPreset(.custom) })
                    }
                }.accessibilityLabel("Time presets")
                if viewModel.state.preset == InsightsPreset.custom {
                    VStack(spacing: 10) {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) { Text("From").font(.caption.weight(.medium)).foregroundStyle(Theme.textSecondary); DatePicker("From", selection: Binding(get: { viewModel.customFromDate }, set: { viewModel.customFromDate = $0 }), displayedComponents: .date).labelsHidden().datePickerStyle(.compact).accessibilityLabel("Custom range start date") }
                            VStack(alignment: .leading, spacing: 4) { Text("To").font(.caption.weight(.medium)).foregroundStyle(Theme.textSecondary); DatePicker("To", selection: Binding(get: { viewModel.customToDate }, set: { viewModel.customToDate = $0 }), displayedComponents: .date).labelsHidden().datePickerStyle(.compact).accessibilityLabel("Custom range end date") }
                            Spacer()
                        }
                        Button { viewModel.setCustomRange(from: viewModel.customFromDate, to: viewModel.customToDate) } label: {
                            Text("Apply custom range").font(.subheadline.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 10).background(Theme.forestGreen).foregroundStyle(.white).clipShape(RoundedRectangle(cornerRadius: 10))
                        }.buttonStyle(.plain).accessibilityLabel("Apply custom range").accessibilityHint("Applies the selected start and end dates")
                    }.padding(12).background(Color(.systemBackground)).clipShape(RoundedRectangle(cornerRadius: 12)).shadow(color: .black.opacity(0.05), radius: 6, y: 2).accessibilityElement(children: .contain)
                }
            }
        }
    }

    private var patientScopeLabel: String {
        if let n = patientName, !n.isEmpty { return n }
        if viewModel.state.patientId != nil { return "Selected patient" }
        return "All active patients"
    }
    private func effectiveRange() -> (Kotlinx_datetimeLocalDate, Kotlinx_datetimeLocalDate)? { guard let f = viewModel.state.snapshot?.appliedFilter else { return nil }; return (f.from, f.to) }
    private func openDrillDown(recordType: RecordType? = nil, reproductionEventType: ReproductionEventType? = nil, dataIssueType: InsightsDataIssueType? = nil) {
        guard let filter = viewModel.state.snapshot?.appliedFilter else { return }
        let dd = InsightsDrillDown(
            from: filter.from,
            to: filter.to,
            patientId: viewModel.state.patientId,
            recordType: recordType,
            reproductionEventType: reproductionEventType,
            dataIssueType: dataIssueType
        )
        drillDownKey = InsightsDrillDownKey(drillDown: dd)
    }

    private var loadingView: some View {
        VStack(spacing: 16) { ProgressView().scaleEffect(1.2); Text("Loading insights…").font(.subheadline).foregroundStyle(Theme.textSecondary) }.frame(maxWidth: .infinity, maxHeight: .infinity).padding().accessibilityLabel("Loading insights")
    }
    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "chart.bar.doc.horizontal").font(.system(size: 56)).foregroundStyle(Theme.forestGreen.opacity(0.35)).accessibilityHidden(true)
            Text("No activity in this period").font(.headline).foregroundStyle(Theme.textPrimary)
            Text("Try a different date range or add records for this period.").font(.subheadline).foregroundStyle(Theme.textSecondary).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            Button { viewModel.selectPreset(.thirtyDays) } label: { Text("Back to 30 days").font(.subheadline.weight(.semibold)).padding(.horizontal, 16).padding(.vertical, 10).background(Theme.forestGreen).foregroundStyle(.white).clipShape(Capsule()) }.buttonStyle(.plain).padding(.top, 4).accessibilityLabel("Back to 30 days").accessibilityHint("Resets to last 30 days")
        }.frame(maxWidth: .infinity).padding(.vertical, 32).padding(.horizontal).background(Theme.surfaceElevated).clipShape(RoundedRectangle(cornerRadius: 14)).accessibilityElement(children: .combine).accessibilityLabel("No activity in this period")
    }
    private func validationEmptyView(message: String) -> some View {
        VStack(spacing: 12) { Image(systemName: "exclamationmark.triangle").font(.system(size: 44)).foregroundStyle(Theme.amber).accessibilityHidden(true); Text(message).font(.subheadline.weight(.medium)).foregroundStyle(Theme.textPrimary).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true); Text("Choose a start date on or before the end date.").font(.caption).foregroundStyle(Theme.textSecondary) }
            .frame(maxWidth: .infinity).padding(20).background(Theme.surfaceElevated).clipShape(RoundedRectangle(cornerRadius: 14)).accessibilityLabel("Validation: \(message)")
    }
    private func errorEmptyView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill").font(.system(size: 48)).foregroundStyle(Theme.amber).accessibilityHidden(true)
            Text(message).font(.subheadline).foregroundStyle(Theme.textPrimary).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            Button { Task { _ = await viewModel.reloadAsync() } } label: { Text("Retry").font(.subheadline.weight(.semibold)).padding(.horizontal, 16).padding(.vertical, 10).background(Theme.forestGreen).foregroundStyle(.white).clipShape(Capsule()) }.buttonStyle(.plain).accessibilityLabel("Retry loading insights")
            Button { viewModel.dismissError() } label: { Text("Dismiss").font(.caption.weight(.medium)).foregroundStyle(Theme.textSecondary) }.buttonStyle(.plain).accessibilityLabel("Dismiss error")
        }.frame(maxWidth: .infinity).padding(.vertical, 32).padding(.horizontal).background(Theme.surfaceElevated).clipShape(RoundedRectangle(cornerRadius: 14)).accessibilityElement(children: .combine).accessibilityLabel("Error: \(message)")
    }
}
