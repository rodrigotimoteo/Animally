import SwiftUI
import Shared

/// Dashboard screen rendering the deterministic `InsightsSnapshot` from shared Kotlin.
///
/// Owns no calculation, date arithmetic or taxonomy — every string and number is
/// formatted straight from the use-case snapshot. Handles five polished states
/// (content / loading / empty / validation / retry) and respects light / dark /
/// system themes, compact width, and large Dynamic Type without clipping.
struct InsightsView: View {
    @StateObject private var viewModel: InsightsViewModel
    @State private var customFromDate: Date
    @State private var customToDate: Date

    // Single enum-driven sheet — avoids double .sheet collision (only one presents)
    private enum InsightsSheet: Identifiable {
        case records(InsightsDrillDown)
        case gestation(RecordDetailKey)
        var id: String {
            switch self {
            case .records(let dd):
                let pid = dd.patientId?.int64Value ?? -1
                let rt = dd.recordType?.wireName ?? "all"
                let et = dd.reproductionEventType?.storageLabel ?? "all"
                let dt = dd.dataIssueType?.name ?? "all"
                // sanitize spaces for stable id
                let rtSafe = rt.replacingOccurrences(of: " ", with: "_")
                let etSafe = et.replacingOccurrences(of: " ", with: "_")
                let dtSafe = dt.replacingOccurrences(of: " ", with: "_")
                return "records-\(dd.from.displayString)-\(dd.to.displayString)-\(pid)-\(rtSafe)-\(etSafe)-\(dtSafe)"
            case .gestation(let key):
                return "gestation-\(key.id)"
            }
        }
    }

    @State private var activeSheet: InsightsSheet?
    private let patientName: String?

    init(patientId: Int64? = nil, patientName: String? = nil) {
        self.patientName = patientName
        let vm = InsightsViewModel(patientId: patientId)
        _viewModel = StateObject(wrappedValue: vm)
        // Init picker from Kotlin customFrom/To so first frame matches shared todayProvider; avoids DST drift.
        if let from = vm.state.customFrom?.swiftDate, let to = vm.state.customTo?.swiftDate {
            _customFromDate = State(initialValue: from)
            _customToDate = State(initialValue: to)
        } else if let toDate = vm.state.to?.swiftDate ?? vm.state.customTo?.swiftDate {
            // Presentation-only fallback: -29 days ≈ 30-day window. DST shift vs Kotlin DatePeriod(days:29) acceptable here.
            // Kotlin period semantics remain canonical; this branch only seeds Swift Date pickers before first Kotlin state arrives.
            let fromDate = Calendar.current.date(byAdding: .day, value: -29, to: toDate) ?? toDate
            _customFromDate = State(initialValue: fromDate)
            _customToDate = State(initialValue: toDate)
        } else {
            // Presentation-only fallback when shared state has no date. Preset
            // range semantics remain exclusively in Kotlin.
            var comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
            comps.hour = 12
            let today = Calendar.current.date(from: comps) ?? Date()
            _customFromDate = State(initialValue: today)
            _customToDate = State(initialValue: today)
        }
    }

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.snapshot == nil {
                loadingView
            } else if viewModel.state.snapshot == nil, let error = viewModel.state.errorMessage {
                errorEmptyView(message: error)
            } else {
                dashboardScroll
            }
        }
        .navigationTitle("Insights")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    viewModel.reload()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .accessibilityLabel("Reload insights")
                }
                .disabled(viewModel.state.isLoading)
            }
        }
        .overlay(alignment: .top) {
            VStack(spacing: 8) {
                if let error = viewModel.state.errorMessage {
                    errorBanner(message: error)
                }
                if let validation = viewModel.state.validationError {
                    validationBanner(message: validation)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: viewModel.state.errorMessage)
            .animation(.easeInOut(duration: 0.2), value: viewModel.state.validationError)
        }
        .onAppear {
            syncCustomDatesFromState()
        }
        .onChange(of: viewModel.state.customFrom) { _, newValue in
            if let d = newValue?.swiftDate { customFromDate = d }
        }
        .onChange(of: viewModel.state.customTo) { _, newValue in
            if let d = newValue?.swiftDate { customToDate = d }
        }
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .records(let dd):
                NavigationStack {
                    InsightsRecordsView(drillDown: dd, patientName: patientName)
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button("Done") { activeSheet = nil }
                            }
                        }
                }
            case .gestation(let key):
                NavigationStack {
                    RecordDetailView(displayType: key.displayType, patientId: key.patientId, recordId: key.recordId)
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button("Done") { activeSheet = nil }
                            }
                        }
                }
            }
        }
    }

    // MARK: - Dashboard scroll

    private var dashboardScroll: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Scope + range controls — wrap, never clip at AX sizes
                controlsSection

                // State-dependent body
                if let validation = viewModel.state.validationError, viewModel.state.snapshot == nil {
                    // Validation with no prior snapshot — show guidance, no chart
                    validationEmptyView(message: validation)
                } else if let snapshot = viewModel.state.snapshot {
                    // Content — even when validation banner is showing, keep last snapshot visible
                    // Overview + case mix are activity-dependent; gestation is current-state independent.
                    InsightsOverviewSection(
                        overview: snapshot.overview,
                        isComparisonEnabled: viewModel.state.isComparisonEnabled,
                        onOpenSourceRecords: { openDrillDown(recordType: nil) }
                    )
                    .accessibilityElement(children: .contain)

                    if snapshot.overview.activityCount == 0 {
                        emptyView
                    } else {
                        InsightsCaseMixSection(
                            activitySeries: snapshot.activitySeries as? [ActivityPoint] ?? [],
                            recordMix: snapshot.recordMix as? [RecordTypeCount] ?? [],
                            totalActivityCount: snapshot.overview.activityCount,
                            isDrillDownEnabled: effectiveRange() != nil,
                            onSelectRecordType: { type in
                                openDrillDown(recordType: type)
                            }
                        )
                        .accessibilityElement(children: .contain)
                    }

                    InsightsReproductionSection(
                        metrics: snapshot.reproduction,
                        isDrillDownEnabled: effectiveRange() != nil
                    ) { recordType, eventType in
                        openDrillDown(
                            recordType: recordType,
                            reproductionEventType: eventType
                        )
                    }
                    .accessibilityElement(children: .contain)

                    // Task 11: current gestation snapshot (today-based, not period-limited)
                    if let care = snapshot.currentCare as? CurrentCareSnapshot {
                        InsightsGestationSection(currentCare: care) { patientId, gestationId in
                            activeSheet = .gestation(
                                RecordDetailKey(
                                    displayType: "GESTATION",
                                    patientId: patientId,
                                    recordId: gestationId
                                )
                            )
                        }
                        .accessibilityElement(children: .contain)
                    }

                    // Task 13: research-readiness readiness (period-based, explicit counts)
                    InsightsReadinessSection(
                        dataIssues: snapshot.dataIssues,
                        isDrillDownEnabled: effectiveRange() != nil
                    ) { issueType in
                        openReadinessDrillDown(issueType: issueType)
                    }
                    .accessibilityElement(children: .contain)

                    if viewModel.state.isLoading {
                        HStack(spacing: 8) {
                            ProgressView().scaleEffect(0.8)
                            Text("Updating…")
                                .font(.caption)
                                .foregroundStyle(Theme.textSecondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                        .accessibilityLabel("Updating insights")
                    }
                } else if viewModel.state.isLoading {
                    loadingView
                }
            }
            .padding()
        }
        .accessibilityIdentifier("insights_dashboard")
        .refreshable {
            await viewModel.reloadAsync()
        }
    }

    // MARK: - Controls

    private var controlsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Patient scope label
            HStack(spacing: 6) {
                Image(systemName: viewModel.state.patientId == nil ? "pawprint.fill" : "pawprint.circle.fill")
                    .font(.caption)
                    .foregroundStyle(Theme.forestGreen)
                Text(patientScopeLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Spacer()
                if let from = viewModel.state.from, let to = viewModel.state.to {
                    Text("\(from.displayString) – \(to.displayString)")
                        .font(.caption2)
                        .foregroundStyle(Theme.textSecondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                } else if viewModel.state.preset == InsightsPreset.allTime {
                    Text("All time")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(Theme.textSecondary)
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Scope: \(patientScopeLabel)")

            // Preset segmented control — wraps to menu on compact via horizontal scroll
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    presetButton(title: "30 days", preset: .thirtyDays)
                    presetButton(title: "90 days", preset: .ninetyDays)
                    presetButton(title: "All time", preset: .allTime)
                    presetButton(title: "Custom", preset: .custom)
                }
            }

            // Custom range pickers — only when custom active, never clip
            if viewModel.state.preset == InsightsPreset.custom {
                VStack(spacing: 10) {
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("From")
                                .font(.caption.weight(.medium))
                                .foregroundStyle(Theme.textSecondary)
                            DatePicker(
                                "From",
                                selection: $customFromDate,
                                displayedComponents: .date
                            )
                            .labelsHidden()
                            .datePickerStyle(.compact)
                            .accessibilityLabel("Custom range start date")
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text("To")
                                .font(.caption.weight(.medium))
                                .foregroundStyle(Theme.textSecondary)
                            DatePicker(
                                "To",
                                selection: $customToDate,
                                displayedComponents: .date
                            )
                            .labelsHidden()
                            .datePickerStyle(.compact)
                            .accessibilityLabel("Custom range end date")
                        }
                        Spacer()
                    }
                    Button {
                        viewModel.setCustomRange(from: customFromDate, to: customToDate)
                    } label: {
                        Text("Apply custom range")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Theme.forestGreen)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Apply custom range")
                    .accessibilityHint("Applies the selected start and end dates")
                }
                .padding(12)
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
            }
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func presetButton(title: String, preset: InsightsPreset) -> some View {
        let isSelected = viewModel.state.preset == preset
        return Button {
            if preset == .custom {
                // Entering custom without dates keeps current custom inputs visible
                syncCustomDatesFromState()
            }
            viewModel.selectPreset(preset)
        } label: {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .frame(minHeight: 36)
                .foregroundStyle(isSelected ? Color.white : Theme.textSecondary)
                .background(isSelected ? Theme.forestGreen : Color(.systemBackground))
                .clipShape(Capsule())
                .shadow(color: .black.opacity(isSelected ? 0.08 : 0.04), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityIdentifier("insights_preset_\(title.lowercased().replacingOccurrences(of: " ", with: "_"))")
    }

    private var patientScopeLabel: String {
        if let patientName, !patientName.isEmpty {
            return patientName
        }
        if viewModel.state.patientId != nil {
            return "Selected patient"
        }
        return "All active patients"
    }

    private func syncCustomDatesFromState() {
        if let from = viewModel.state.customFrom?.swiftDate { customFromDate = from }
        if let to = viewModel.state.customTo?.swiftDate { customToDate = to }
    }

    private func effectiveRange() -> (Kotlinx_datetimeLocalDate, Kotlinx_datetimeLocalDate)? {
        guard let filter = viewModel.state.snapshot?.appliedFilter else { return nil }
        return (filter.from, filter.to)
    }

    private func makeDrillDown(
        recordType: RecordType?,
        reproductionEventType: ReproductionEventType? = nil
    ) -> InsightsDrillDown? {
        guard let (from, to) = effectiveRange() else { return nil }
        return InsightsDrillDown(
            from: from,
            to: to,
            patientId: viewModel.state.patientId,
            recordType: recordType,
            reproductionEventType: reproductionEventType,
            dataIssueType: nil
        )
    }

    private func openDrillDown(
        recordType: RecordType?,
        reproductionEventType: ReproductionEventType? = nil
    ) {
        guard let next = makeDrillDown(
            recordType: recordType,
            reproductionEventType: reproductionEventType
        ) else { return }
        activeSheet = .records(next)
    }

    private func makeReadinessDrillDown(issueType: InsightsDataIssueType) -> InsightsDrillDown? {
        guard let (from, to) = effectiveRange() else { return nil }
        return InsightsDrillDown(
            from: from,
            to: to,
            patientId: viewModel.state.patientId,
            recordType: nil,
            reproductionEventType: nil,
            dataIssueType: issueType
        )
    }

    private func openReadinessDrillDown(issueType: InsightsDataIssueType) {
        guard let next = makeReadinessDrillDown(issueType: issueType) else { return }
        activeSheet = .records(next)
    }

    // MARK: - State views (light/dark/system aware via Theme.* semantic colors)

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
            Text("Loading insights…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .accessibilityLabel("Loading insights")
    }

    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "chart.bar.doc.horizontal")
                .font(.system(size: 56))
                .foregroundStyle(Theme.forestGreen.opacity(0.35))
            Text("No activity in this period")
                .font(.headline)
                .foregroundStyle(Theme.textPrimary)
            Text("Try a different date range or add records for this period.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                viewModel.selectPreset(.thirtyDays)
            } label: {
                Text("Back to 30 days")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Theme.forestGreen)
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .padding(.horizontal)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("No activity in this period")
    }

    private func validationEmptyView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 44))
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Theme.textPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Text("Choose a start date on or before the end date.")
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityLabel("Validation: \(message)")
    }

    private func errorEmptyView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                Task { await viewModel.reloadAsync() }
            } label: {
                Text("Retry")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Theme.forestGreen)
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Retry loading insights")
            Button {
                viewModel.dismissError()
            } label: {
                Text("Dismiss")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Theme.textSecondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Dismiss error")
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .padding(.horizontal)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Error: \(message)")
    }

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Theme.amber)
            Text(message).font(.subheadline).foregroundStyle(Theme.textPrimary).lineLimit(2)
            Spacer()
            Button("Retry") { viewModel.retry() }
                .font(.caption.weight(.bold))
                .foregroundStyle(Theme.forestGreen)
                .accessibilityLabel("Retry loading insights")
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark").font(.caption.weight(.bold)).foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private func validationBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle.fill").foregroundStyle(Theme.amber)
            Text(message).font(.subheadline).foregroundStyle(Theme.textPrimary).lineLimit(2)
            Spacer()
            Button {
                viewModel.dismissValidationError()
            } label: {
                Image(systemName: "xmark").font(.caption.weight(.bold)).foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss validation message")
            }
        }
        .padding(12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}
