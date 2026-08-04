import SwiftUI

struct TimelinePlaceholderView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 64))
                    .foregroundStyle(Theme.forestGreen.opacity(0.4))
                Text("Coming soon")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                Text("Timeline of events will appear here")
                    .font(.subheadline)
                    .foregroundStyle(Theme.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle("Timeline")
        }
    }
}
