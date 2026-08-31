import Foundation
import Shared
import os

/// Swift-facing observable wrapper around the shared [InsightsStore].
///
/// Mirrors `TimelineViewModel` / `PatientDetailViewModel`: subscribes to the
/// Kotlin `StateFlow` via `NativeFlow`, publishes `InsightsUiState` on the
/// main actor, and forwards every user action without calculation or date
/// arithmetic. All metrics remain deterministically computed in
/// `GetInsightsDashboardUseCase`.
///
/// Swift type is deliberately named `InsightsViewModel` to match `tasks/plan.md` Task 8
/// file map; it shadows the Kotlin `InsightsViewModel` only inside the app
/// module. All Kotlin access goes through `InsightsStore` / `IosInsightsStores`
/// so no name collision occurs at call sites.
@MainActor
final class InsightsViewModel: ObservableObject {
    @Published var state: InsightsUiState

    private let store: InsightsStore
    private var cancellable: NativeCancellable?

    /// Creates an observable dashboard for `patientId` (`nil` → all active patients).
    init(patientId: Int64? = nil) {
        let kotlinPatientId: KotlinLong? = patientId.map { KotlinLong(longLong: $0) }
        store = IosInsightsStores.shared.insightsStore(patientId: kotlinPatientId)
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    // MARK: - Actions — every method delegates straight to the store, zero logic

    func reload() {
        store.reload()
    }

    func reloadAsync() async {
        reload()
        // Await load completion so .refreshable spinner stays in sync with Kotlin StateFlow.
        for _ in 0..<50 {
            if !state.isLoading { break }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        if state.isLoading {
            // 5s bound reached but isLoading still true → log timeout (spinner hang guard)
            Logger(subsystem: "com.animally.insights", category: "insights")
                .error("Insights reloadAsync timeout: isLoading still true after 5s")
        }
        // One extra yield to let StateFlow publish final snapshot
        try? await Task.sleep(nanoseconds: 50_000_000)
    }

    func retry() {
        store.reload()
    }

    func selectPreset(_ preset: InsightsPreset) {
        store.selectPreset(preset: preset)
    }

    func setCustomRange(from: Kotlinx_datetimeLocalDate?, to: Kotlinx_datetimeLocalDate?) {
        store.setCustomRange(from: from, to: to)
    }

    /// Convenience bridge from Swift `Date` pickers to Kotlin `LocalDate`.
    func setCustomRange(from startDate: Date?, to endDate: Date?) {
        store.setCustomRange(from: startDate?.kotlinLocalDate, to: endDate?.kotlinLocalDate)
    }

    func setPatientScope(patientId: Int64?) {
        let kotlinId: KotlinLong? = patientId.map { KotlinLong(longLong: $0) }
        store.setPatientScope(patientId: kotlinId)
    }

    func dismissError() {
        store.dismissError()
    }

    func dismissValidationError() {
        store.dismissValidationError()
    }

    deinit {
        cancellable?.cancel()
        store.clear()
    }
}

// MARK: - Date helpers bridging Swift Date ↔ Kotlin LocalDate

extension Date {
    /// Converts this `Date` (in current calendar) to a Kotlin `LocalDate` at midnight.
    var kotlinLocalDate: Kotlinx_datetimeLocalDate {
        let comps = Calendar.current.dateComponents([.year, .month, .day], from: self)
        let resolvedYear = comps.year ?? Calendar.current.component(.year, from: self)
        return Kotlinx_datetimeLocalDate(
            year: Int32(resolvedYear),
            month: Int32(comps.month ?? 1),
            day: Int32(comps.day ?? 1)
        )
    }
}

extension Kotlinx_datetimeLocalDate {
    /// Converts to a Swift `Date` at noon to avoid DST edge shifts.
    var swiftDate: Date {
        var comps = DateComponents()
        comps.year = Int(year)
        // `monthNumber` deprecated but still available and returns Int 1..12;
        // `month` is the new enum type without Int bridge.
        comps.month = Int(monthNumber)
        comps.day = Int(day)
        comps.hour = 12
        return Calendar.current.date(from: comps) ?? Date()
    }
}
