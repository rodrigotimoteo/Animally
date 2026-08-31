import Foundation

/// Navigation key for presenting Insights from any list/detail stack.
/// `nil` means global (all active patients); non-nil scopes to one patient.
/// Reused by TimelineView (global) and PatientDetailView (patient-scoped)
/// so Back always returns to the originating screen and no sixth tab is added.
struct InsightsNavKey: Hashable {
    let patientId: Int64?
    let patientName: String?
}
