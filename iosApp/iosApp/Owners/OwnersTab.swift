import SwiftUI

struct OwnersTab: View {
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            OwnerListView()
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .ownerDetail(let id):
                        OwnerDetailView(ownerId: id)
                    case .ownerEdit(let id):
                        OwnerEditView(ownerId: id)
                    case .patientDetail(let id):
                        PatientDetailView(patientId: id)
                    case .patientEdit(let id):
                        PatientEditView(patientId: id)
                    }
                }
        }
    }
}
