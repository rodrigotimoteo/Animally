import SwiftUI
import Shared

struct PatientDetailView: View {
    @StateObject private var viewModel: PatientDetailViewModel
    @State private var selectedTab: DetailTab = .overview
    @State private var addRecordRoute: RecordEditRoute?

    enum DetailTab: String, CaseIterable, Identifiable {
        case overview = "Overview"
        case medical = "Medical"
        case preventive = "Care"
        case reproduction = "Repro"
        case diagnostics = "Files"

        var id: String { rawValue }

        /// Full name for accessibility — the short segmented labels are
        /// compressed, VoiceOver should speak the real section names.
        var accessibilityName: String {
            switch self {
            case .overview: return "Overview"
            case .medical: return "Medical"
            case .preventive: return "Preventive"
            case .reproduction: return "Reproduction"
            case .diagnostics: return "Diagnostics & Files"
            }
        }
    }

    init(patientId: Int64) {
        _viewModel = StateObject(wrappedValue: PatientDetailViewModel(patientId: patientId))
    }

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.patient == nil {
                loadingView
            } else if let patient = viewModel.state.patient {
                contentTabs(patient: patient)
            } else {
                notFoundView
            }
        }
        .navigationTitle(viewModel.state.patient?.name ?? "Patient")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.patientEdit(viewModel.state.patient?.id)) {
                    Image(systemName: "pencil")
                        .accessibilityLabel("Edit patient")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                if let patientId = viewModel.state.patient?.id {
                    AddRecordMenu(patientId: patientId) { route in
                        addRecordRoute = route
                    }
                }
            }
        }
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.state.errorMessage {
                errorBanner(message: errorMessage)
            }
        }
        .onAppear {
            viewModel.load()
        }
        .navigationDestination(item: $addRecordRoute) { route in
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
            case .anamnese(_, let recordId):
                AnamneseEditView(patientId: route.patientId, anamneseId: recordId)
            case .lameness(_, let recordId):
                LamenessEditView(patientId: route.patientId, lamenessId: recordId)
            case .surgery(_, let recordId):
                SurgeryEditView(patientId: route.patientId, surgeryId: recordId)
            case .medication(_, let recordId):
                MedicationEditView(patientId: route.patientId, medicationId: recordId)
            case .substance(_, let recordId):
                SubstanceEditView(patientId: route.patientId, substanceId: recordId)
            case .labResult(_, let recordId):
                LabResultEditView(patientId: route.patientId, labResultId: recordId)
            case .customReminder(_, let recordId):
                CustomReminderEditView(patientId: route.patientId, customReminderId: recordId)
            case .reproductionEvent(_, let recordId):
                ReproductionEventEditView(patientId: route.patientId, reproductionEventId: recordId)
            case .ultrasound(_, let recordId):
                UltrasoundEditView(patientId: route.patientId, ultrasoundId: recordId)
            case .gestation(_, let recordId):
                GestationEditView(patientId: route.patientId, gestationId: recordId)
            case .reproMedication(_, let recordId):
                ReproMedicationEditView(patientId: route.patientId, reproMedicationId: recordId)
            case .imaging(_, let recordId):
                ImagingEditView(patientId: route.patientId, imagingId: recordId)
            }
        }
    }

    private func contentTabs(patient: Patient_) -> some View {
        VStack(spacing: 0) {
            Picker("Detail Tab", selection: $selectedTab) {
                ForEach(DetailTab.allCases) { tab in
                    Text(tab.rawValue)
                        .tag(tab)
                        .accessibilityLabel(tab.accessibilityName)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.vertical, 8)

            switch selectedTab {
            case .overview:
                OverviewTab(patient: patient, ownerName: viewModel.state.ownerName)
            case .medical:
                MedicalTabView(patientId: patient.id)
            case .preventive:
                PreventiveTabView(patientId: patient.id)
            case .reproduction:
                ReproductionTabView(patientId: patient.id)
            case .diagnostics:
                DiagnosticsTabView(patientId: patient.id)
            }
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading patient…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var notFoundView: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.amber)
            Text("Patient not found")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

struct OverviewTab: View {
    let patient: Patient_
    let ownerName: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                patientHeader
                basicInfoSection
                if ownerName != nil {
                    ownerSection
                }
                identificationSection
            }
            .padding()
        }
    }

    private var patientHeader: some View {
        VStack(spacing: 12) {
            Image(systemName: "pawprint.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(Theme.forestGreen)
            Text(patient.name)
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Theme.textPrimary)
            Text(patient.species)
                .font(.title3)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }

    private var basicInfoSection: some View {
        Section {
            infoRow(label: "Breed", value: patient.breed)
            infoRow(label: "Gender", value: patient.gender)
            infoRow(label: "Date of Birth", value: formatDate(patient.dateOfBirth))
            infoRow(label: "Location", value: patient.stableLocation)
        } header: {
            sectionHeader("Basic Information")
        }
    }

    private var ownerSection: some View {
        Section {
            if let ownerName = ownerName {
                HStack {
                    Image(systemName: "person.crop.circle.fill")
                        .foregroundStyle(Theme.forestGreen)
                    Text(ownerName)
                        .foregroundStyle(Theme.textPrimary)
                }
            }
        } header: {
            sectionHeader("Owner")
        }
    }

    private var identificationSection: some View {
        Section {
            infoRow(label: "Microchip", value: patient.microchipId)
            infoRow(label: "UELN", value: patient.ueln)
            infoRow(label: "Registration", value: patient.registrationNumber)
        } header: {
            sectionHeader("Identification")
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
    }

    private func infoRow(label: String, value: String?) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
            Text(value ?? "—")
                .foregroundStyle(value != nil ? Theme.textPrimary : Theme.textTertiary)
        }
        .font(.subheadline)
    }

    private func formatDate(_ date: Kotlinx_datetimeLocalDate?) -> String? {
        guard let date = date else { return nil }
        return "\(date.year)-\(String(format: "%02d", date.monthNumber))-\(String(format: "%02d", date.dayOfMonth))"
    }
}

struct StubTabView: View {
    let title: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: systemImage)
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.4))
            Text("Coming soon")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("\(title) information will appear here")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
