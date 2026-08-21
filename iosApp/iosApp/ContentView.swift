import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Tab = .patients

    enum Tab: Hashable {
        case patients, owners, timeline, search, settings
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

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(Tab.settings)
        }
    }
}
