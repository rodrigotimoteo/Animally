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
                    case .patientEdit(let id):
                        PatientEditView(patientId: id)
                    case .ownerDetail(let id):
                        OwnerDetailView(ownerId: id)
                    case .ownerEdit(let id):
                        OwnerEditView(ownerId: id)
                    }
                }
                .navigationDestination(for: RecordEditRoute.self) { route in
                    recordEditDestination(route)
                }
                .navigationDestination(for: InsightsNavKey.self) { key in
                    InsightsView(patientId: key.patientId, patientName: key.patientName)
                }
        }
    }
}
