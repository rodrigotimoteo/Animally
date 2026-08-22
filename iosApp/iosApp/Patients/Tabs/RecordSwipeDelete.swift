import SwiftUI

/// Adds a trailing red delete swipe action with a system confirmation dialog
/// to a record row. Apply directly to the row content inside `ForEach`.
private struct RecordSwipeDelete: ViewModifier {
    let title: String
    let onDelete: () -> Void

    @State private var isConfirming = false

    func body(content: Content) -> some View {
        content
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button(role: .destructive) {
                    isConfirming = true
                } label: {
                    Label("Delete", systemImage: "trash")
                }
            }
            .confirmationDialog(
                "Delete \(title)?",
                isPresented: $isConfirming,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive, action: onDelete)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This record will be removed from the patient's history.")
            }
    }
}

extension View {
    func recordSwipeDelete(title: String, onDelete: @escaping () -> Void) -> some View {
        modifier(RecordSwipeDelete(title: title, onDelete: onDelete))
    }
}
