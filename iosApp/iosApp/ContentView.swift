import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Tab = .patients
    @StateObject private var theme = ThemeViewModel()

    enum Tab: Hashable {
        case patients, owners, timeline, search, assistant
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            PatientsTab()
                .tabItem {
                    Label("Patients", systemImage: "pawprint.fill")
                }
                .tag(Tab.patients)

            OwnersTab()
                .tabItem {
                    Label("Owners", systemImage: "person.crop.circle.fill")
                }
                .tag(Tab.owners)

            TimelineView()
                .tabItem {
                    Label("Timeline", systemImage: "clock.arrow.circlepath")
                }
                .tag(Tab.timeline)

            SearchView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(Tab.search)

            AssistantView()
                .tabItem {
                    Label("Assistant", systemImage: "sparkles")
                }
                .tag(Tab.assistant)
        }
        .preferredColorScheme(theme.preferredColorScheme)
        // Sheets capture color scheme at presentation; republishing via the
        // environment lets SettingsView apply live scheme changes itself.
        .environmentObject(theme)
    }
}
