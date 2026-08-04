import SwiftUI

struct SettingsPlaceholderView: View {
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Theme.textSecondary)
                        Text("App settings and preferences")
                            .foregroundStyle(Theme.textSecondary)
                    }
                } header: {
                    Text("Settings")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Settings")
        }
    }
}
