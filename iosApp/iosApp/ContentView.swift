import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Tab = .patients

    enum Tab: Hashable {
        case patients, timeline, search, settings
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            PatientsTab()
                .tabItem {
                    Label("Patients", systemImage: "pawprint.fill")
                }
                .tag(Tab.patients)

            TimelinePlaceholderView()
                .tabItem {
                    Label("Timeline", systemImage: "clock.arrow.circlepath")
                }
                .tag(Tab.timeline)

            SearchPlaceholderView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(Tab.search)

            SettingsPlaceholderView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(Tab.settings)
        }
    }
}
