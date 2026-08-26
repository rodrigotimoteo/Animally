import SwiftUI

/// Adds a trailing red delete swipe action to a record row. Apply directly
/// to the row content inside `ForEach`. The explicit confirmation protects
/// clinical history from an accidental swipe, especially on small screens.
private struct RecordSwipeDelete: ViewModifier {
    let title: String
    let onDelete: () -> Void
    @State private var isShowingConfirmation = false

    func body(content: Content) -> some View {
        content
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button(role: .destructive) {
                    isShowingConfirmation = true
                } label: {
                    Text("Delete")
                }
                .tint(.red)
            }
            .confirmationDialog(
                "Delete \(title)?",
                isPresented: $isShowingConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive, action: onDelete)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This record will be removed from the patient's active history.")
            }
    }
}

extension View {
    func recordSwipeDelete(
        title: String,
        onDelete: @escaping () -> Void,
    ) -> some View {
        modifier(RecordSwipeDelete(title: title, onDelete: onDelete))
    }
}
