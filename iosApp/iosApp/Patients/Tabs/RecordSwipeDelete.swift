import SwiftUI

/// Adds a trailing red delete swipe action to a row. Apply directly to the
/// row content inside `ForEach`. The explicit confirmation protects clinical
/// history from an accidental swipe, especially on small screens.
private struct ConfirmationSwipeDelete: ViewModifier {
    let title: String
    let message: String
    let onDelete: () -> Void
    @State private var isShowingConfirmation = false

    func body(content: Content) -> some View {
        content
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                // This button only opens the confirmation dialog. Keeping it
                // non-destructive prevents SwiftUI's List coordinator from
                // treating the swipe as an immediate row deletion before the
                // user confirms the operation.
                Button {
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
                Text(message)
            }
    }
}

extension View {
    func confirmationSwipeDelete(
        title: String,
        message: String = "This record will be removed from the patient's active history.",
        onDelete: @escaping () -> Void
    ) -> some View {
        modifier(ConfirmationSwipeDelete(title: title, message: message, onDelete: onDelete))
    }
}
