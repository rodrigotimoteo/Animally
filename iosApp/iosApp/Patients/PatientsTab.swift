import SwiftUI

struct PatientsTab: View {
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            PatientListView()
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .patientDetail(let id):
                        PatientDetailView(patientId: id)
                    }
                }
        }
    }
}

enum Route: Hashable {
    case patientDetail(Int64)
}
