import SwiftUI

struct SearchPlaceholderView: View {
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(Theme.textSecondary)
                        Text("Search patients, records, and more")
                            .foregroundStyle(Theme.textSecondary)
                    }
                } header: {
                    Text("Search")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Search")
        }
    }
}
