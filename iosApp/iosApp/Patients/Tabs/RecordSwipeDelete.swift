import SwiftUI

/// Adds a trailing red delete swipe action to a record row. Apply directly
/// to the row content inside `ForEach`. Deletes immediately on tap — standard
/// iOS list behavior, no confirmation step.
private struct RecordSwipeDelete: ViewModifier {
    let onDelete: () -> Void

    func body(content: Content) -> some View {
        content
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                Button(role: .destructive, action: onDelete) {
                    Text("Delete")
                }
            }
    }
}

extension View {
    func recordSwipeDelete(
        title: String,
        onDelete: @escaping () -> Void,
    ) -> some View {
        modifier(RecordSwipeDelete(onDelete: onDelete))
    }
}
