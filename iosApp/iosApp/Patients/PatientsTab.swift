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
                    switch route {
                    case .weight(_, let recordId):
                        WeightEditView(patientId: route.patientId, weightId: recordId)
                    case .vaccination(_, let recordId):
                        VaccinationEditView(patientId: route.patientId, vaccinationId: recordId)
                    case .deworming(_, let recordId):
                        DewormingEditView(patientId: route.patientId, dewormingId: recordId)
                    case .consultation(_, let recordId):
                        ConsultationEditView(patientId: route.patientId, consultationId: recordId)
                    case .dentistry(_, let recordId):
                        DentistryEditView(patientId: route.patientId, dentistryId: recordId)
                    case .farrierVisit(_, let recordId):
                        FarrierVisitEditView(patientId: route.patientId, farrierVisitId: recordId)
                    }
                }
        }
    }
}

enum Route: Hashable {
    case patientDetail(Int64)
    case patientEdit(Int64?)
    case ownerDetail(Int64)
    case ownerEdit(Int64?)
}

/// Push destinations for adding/editing patient records. Kept separate
/// from `Route` so only the Patients stack registers them — Timeline and
/// Search stacks switch exhaustively over `Route` and must stay untouched.
enum RecordEditRoute: Hashable, Identifiable {
    case weight(patientId: Int64, recordId: Int64?)
    case vaccination(patientId: Int64, recordId: Int64?)
    case deworming(patientId: Int64, recordId: Int64?)
    case consultation(patientId: Int64, recordId: Int64?)
    case dentistry(patientId: Int64, recordId: Int64?)
    case farrierVisit(patientId: Int64, recordId: Int64?)

    var id: Self { self }

    var patientId: Int64 {
        switch self {
        case .weight(let patientId, _),
             .vaccination(let patientId, _),
             .deworming(let patientId, _),
             .consultation(let patientId, _),
             .dentistry(let patientId, _),
             .farrierVisit(let patientId, _):
            return patientId
        }
    }
}
