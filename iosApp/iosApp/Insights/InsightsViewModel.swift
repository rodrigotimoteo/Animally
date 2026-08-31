import Foundation
import Shared

/// Swift-facing observable wrapper around the shared [InsightsStore].
@MainActor
final class InsightsViewModel: ObservableObject {
    @Published var state: InsightsUiState
    @Published var customFromDate: Date
    @Published var customToDate: Date

    private let store: InsightsStore
    private var cancellable: NativeCancellable?

    init(patientId: Int64? = nil) {
        let kotlinPatientId: KotlinLong? = patientId.map { KotlinLong(longLong: $0) }
        let s = IosInsightsStores.shared.insightsStore(patientId: kotlinPatientId)
        let cur = s.state.current
        // Single source fallback: to = cur.to ?? cur.customTo ?? todayNoon; from = cur.customFrom ?? (to -29d)
        let fallbackTo = cur.to?.swiftDate ?? cur.customTo?.swiftDate ?? DateFormatters.todayAtNoon()
        let defaultFrom = Calendar.current.date(byAdding: .day, value: -29, to: fallbackTo) ?? fallbackTo
        let initialFrom = cur.customFrom?.swiftDate ?? defaultFrom
        let initialTo = cur.customTo?.swiftDate ?? fallbackTo
        store = s
        state = cur
        customFromDate = initialFrom
        customToDate = initialTo
        cancellable = s.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
                if let f = newState.customFrom?.swiftDate { self?.customFromDate = f }
                if let t = newState.customTo?.swiftDate { self?.customToDate = t }
            }
        })
    }

    @Published var awaitError: String?

    func reload() { store.reload(); awaitError = nil }

    func reloadAsync() async -> Bool {
        awaitError = nil
        reload()
        let ok = await StoreAwait.awaitIdle(store.state, isLoading: { $0.isLoading })
        if !ok { awaitError = StoreAwaitError.timeout.localizedDescription }
        return ok
    }

    func dismissAwaitError() { awaitError = nil }

    func retry() { store.reload() }

    func selectPreset(_ preset: InsightsPreset) { store.selectPreset(preset: preset) }

    func setCustomRange(from: Kotlinx_datetimeLocalDate?, to: Kotlinx_datetimeLocalDate?) { store.setCustomRange(from: from, to: to) }

    func setCustomRange(from startDate: Date?, to endDate: Date?) {
        store.setCustomRange(from: startDate?.kotlinLocalDate, to: endDate?.kotlinLocalDate)
    }

    func setPatientScope(patientId: Int64?) {
        let kotlinId: KotlinLong? = patientId.map { KotlinLong(longLong: $0) }
        store.setPatientScope(patientId: kotlinId)
    }

    func dismissError() { store.dismissError() }
    func dismissValidationError() { store.dismissValidationError() }

    deinit {
        cancellable?.cancel()
        store.clear()
    }
}
