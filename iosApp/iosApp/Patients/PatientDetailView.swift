import SwiftUI
import Shared

struct PatientDetailView: View {
    @StateObject private var viewModel: PatientDetailViewModel
    @State private var selectedTab: DetailTab = .overview
    @State private var addRecordRoute: RecordEditRoute?
    @State private var recordDetail: RecordDetailNav?
    /// Bumped when returning from a record editor so the visible tab's
    /// view model is recreated and reloads fresh data.
    @State private var recordsRefreshToken = 0

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
        let base = Group {
            if viewModel.state.isLoading && viewModel.state.patient == nil {
                loadingView
            } else if let patient = viewModel.state.patient {
                contentTabs(patient: patient)
            } else {
                notFoundView
            }
        }

        base
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

        .onChange(of: addRecordRoute) { oldValue, newValue in
            // Returning from a record editor: force the visible tab to reload
            // so freshly saved records appear without switching tabs.
            if oldValue != nil, newValue == nil {
                recordsRefreshToken += 1
                viewModel.load()
            }
        }
        .navigationDestination(item: $addRecordRoute) { route in
            recordEditDestination(route)
        }
        .navigationDestination(item: $recordDetail) { nav in
            // The detail view owns the Edit push itself, so saving an edit
            // pops back to the refreshed read-only detail instead of the tab.
            RecordDetailView(nav: nav)
        }
    }

    /// Friendly nav-bar titles per record display type.
    private static func recordTitle(for type: String) -> String {
        switch type {
        case "Lameness": return "Lameness Evaluation"
        case "Farrier": return "Farrier Visit"
        case "Weight": return "Weight Entry"
        case "Reproduction": return "Reproduction Event"
        default: return type
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
                OverviewTab(patient: patient, ownerName: viewModel.state.ownerName) {
                    selectedTab = .reproduction
                }
            case .medical:
                MedicalTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId, fields in
                        recordDetail = RecordDetailNav(
                            title: Self.recordTitle(for: type),
                            displayType: type,
                            patientId: patient.id,
                            recordId: recordId,
                            fields: fields
                        )
                    }
                )
            case .preventive:
                PreventiveTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId, fields in
                        recordDetail = RecordDetailNav(
                            title: Self.recordTitle(for: type),
                            displayType: type,
                            patientId: patient.id,
                            recordId: recordId,
                            fields: fields
                        )
                    }
                )
            case .reproduction:
                ReproductionTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId, fields in
                        recordDetail = RecordDetailNav(
                            title: Self.recordTitle(for: type),
                            displayType: type,
                            patientId: patient.id,
                            recordId: recordId,
                            fields: fields
                        )
                    }
                )
            case .diagnostics:
                DiagnosticsTabView(
                    patientId: patient.id,
                    refreshToken: recordsRefreshToken,
                    onOpenRecord: { type, recordId, fields in
                        recordDetail = RecordDetailNav(
                            title: Self.recordTitle(for: type),
                            displayType: type,
                            patientId: patient.id,
                            recordId: recordId,
                            fields: fields
                        )
                    }
                )
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
    /// Switches the detail screen to the Reproduction tab.
    var onOpenReproduction: (() -> Void)? = nil

    @StateObject private var careModel: CareDuePanelModel
    @StateObject private var gestationModel: GestationPanelModel

    init(
        patient: Patient_,
        ownerName: String?,
        onOpenReproduction: (() -> Void)? = nil
    ) {
        self.patient = patient
        self.ownerName = ownerName
        self.onOpenReproduction = onOpenReproduction
        _careModel = StateObject(wrappedValue: CareDuePanelModel(patientId: patient.id))
        _gestationModel = StateObject(wrappedValue: GestationPanelModel(patientId: patient.id))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let activeGestation = gestationModel.activeGestation {
                    pregnancyCard(activeGestation)
                }
                careDueSection
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

    /// Compact "In Foal" status card shown only while a gestation is active;
    /// taps through to the Reproduction tab.
    private func pregnancyCard(_ gestation: Gestation_) -> some View {
        Button {
            onOpenReproduction?()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "heart.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(Theme.forestGreen)

                VStack(alignment: .leading, spacing: 4) {
                    Text("In Foal")
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Theme.forestGreen)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())

                    Text("Day \(gestation.gestationDays) · Due \(gestation.expectedDueDate.friendlyString)")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.textPrimary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
            }
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("In foal, day \(gestation.gestationDays), due \(gestation.expectedDueDate.friendlyString). Opens reproduction tab")
    }

    /// Upcoming and overdue care, surfaced where the vet looks first.
    /// Static rows for v1; tapping through to the record is a follow-up.
    private var careDueSection: some View {
        Group {
            if !careModel.items.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 6) {
                        Image(systemName: "bell.badge")
                            .font(.subheadline.weight(.semibold))
                        Text("Care Due")
                            .font(.subheadline.weight(.semibold))
                    }
                    .foregroundStyle(Theme.forestGreen)

                    ForEach(
                        Array(careModel.items.enumerated()),
                        id: \.offset
                    ) { _, item in
                        HStack(spacing: 12) {
                            Image(systemName: item.overdue ? "exclamationmark.circle.fill" : "clock")
                                .foregroundStyle(item.overdue ? Theme.amber : Theme.forestGreen)
                                .frame(width: 20)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(Theme.textPrimary)
                                Text(item.typeLabel)
                                    .font(.caption)
                                    .foregroundStyle(Theme.textSecondary)
                            }
                            Spacer()
                            Text(Self.dueText(for: item))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Self.dueColor(for: item))
                        }
                        .padding(.vertical, 4)
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("\(item.typeLabel): \(item.title), \(Self.dueText(for: item))")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private static func dueText(for item: CareDueItem) -> String {
        let due = "\(item.dueDate.dayOfMonth) \(monthAbbreviation(item.dueDate.monthNumber)) \(item.dueDate.year)"
        if item.overdue {
            return "Overdue — due \(due)"
        }
        if daysUntil(item.dueDate) == 0 {
            return "Due today"
        }
        let days = daysUntil(item.dueDate)
        return "Due in \(days) day\(days == 1 ? "" : "s")"
    }

    private static func dueColor(for item: CareDueItem) -> Color {
        if item.overdue { return .red }
        if daysUntil(item.dueDate) <= 3 { return .orange }
        return Theme.amber
    }

    private static func daysUntil(_ date: Kotlinx_datetimeLocalDate) -> Int {
        let formatter = RecordFormStyle.isoDateFormatter
        let iso = "\(date.year)-\(String(format: "%02d", date.monthNumber))-\(String(format: "%02d", date.dayOfMonth))"
        guard let due = formatter.date(from: iso) else { return 0 }
        return Calendar.current.dateComponents([.day], from: Date(), to: due).day ?? 0
    }

    private static func monthAbbreviation(_ monthNumber: Int32) -> String {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let index = Int(monthNumber) - 1
        return index >= 0 && index < months.count ? months[index] : ""
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

/// Swift-side observation for the Care Due panel: bridges the Kotlin
/// UpcomingCareStore's state flow into published properties.
@MainActor
final class CareDuePanelModel: ObservableObject {
    @Published var items: [CareDueItem] = []

    private var cancellable: NativeCancellable?

    init(patientId: Int64) {
        let store = IosRecordStores.shared.upcomingCareStore(patientId: patientId)
        cancellable = store.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.items = state.items
            }
        })
        store.load()
    }

    deinit {
        cancellable?.cancel()
    }
}

/// Observes the patient's gestation list so the Overview can surface an
/// active pregnancy. Non-active (Completed/Failed) gestations are ignored.
@MainActor
final class GestationPanelModel: ObservableObject {
    @Published var activeGestation: Gestation_?

    private var cancellable: NativeCancellable?

    init(patientId: Int64) {
        let store = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        cancellable = store.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                // Blocklist semantics mirror the Kotlin reminder filter:
                // anything not explicitly ended counts as an ongoing pregnancy.
                let resolved = ["Completed", "Failed", "Foaled"]
                self?.activeGestation = state.records.first { record in
                    record.isActive && !resolved.contains { record.status.caseInsensitiveCompare($0) == .orderedSame }
                }
            }
        })
        store.load()
    }

    deinit {
        cancellable?.cancel()
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
