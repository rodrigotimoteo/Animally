import SwiftUI
import Shared

// MARK: - AppNavigation — single source for all navigation keys

// Patients / Timeline / Search shared route
enum Route: Hashable {
    case patientDetail(Int64)
    case patientEdit(Int64?)
    case ownerDetail(Int64)
    case ownerEdit(Int64?)
}

// MARK: - Single-source registry for record edit types

/// Canonical kind for every record editor. Single source for wireName, displayName,
/// lowerAliases and view building — eliminates 3× string→case drifts.
enum RecordEditKind: String, CaseIterable {
    case weight = "Weight"
    case vaccination = "Vaccination"
    case deworming = "Deworming"
    case consultation = "Consultation"
    case dentistry = "Dentistry"
    case farrierVisit = "FarrierVisit"
    case anamnese = "Anamnese"
    case lameness = "Lameness"
    case surgery = "Surgery"
    case medication = "Medication"
    case substance = "ControlledSubstance"
    case labResult = "LabResult"
    case customReminder = "CustomReminder"
    case reproductionEvent = "ReproductionEvent"
    case ultrasound = "Ultrasound"
    case gestation = "Gestation"
    case reproMedication = "ReproMedication"
    case imaging = "Imaging"
    case embryoTransfer = "EmbryoTransfer"
    case icsi = "Icsi"

    var wireName: String { rawValue }

    var displayName: String {
        switch self {
        case .farrierVisit: return "Farrier Visit"
        case .labResult: return "Lab Result"
        case .customReminder: return "Custom Reminder"
        case .reproductionEvent: return "Reproduction Event"
        case .reproMedication: return "Repro Medication"
        case .embryoTransfer: return "Embryo Transfer"
        case .substance: return "Controlled Substance"
        default: return rawValue
        }
    }

    var lowerAliases: [String] {
        switch self {
        case .weight: return ["weight"]
        case .vaccination: return ["vaccination"]
        case .deworming: return ["deworming"]
        case .consultation: return ["consultation"]
        case .dentistry: return ["dentistry"]
        case .farrierVisit: return ["farrier", "farrier_visit"]
        case .anamnese: return ["anamnese"]
        case .lameness: return ["lameness"]
        case .surgery: return ["surgery"]
        case .medication: return ["medication"]
        case .substance: return ["substance", "controlled substance", "controlled_substance"]
        case .labResult: return ["lab result", "lab_result"]
        case .customReminder: return ["custom reminder", "custom_reminder", "customreminder"]
        case .reproductionEvent: return ["reproduction", "reproduction_event"]
        case .ultrasound: return ["ultrasound"]
        case .gestation: return ["gestation"]
        case .reproMedication: return ["repro medication", "repro_medication"]
        case .imaging: return ["imaging"]
        case .embryoTransfer: return ["embryo transfer", "embryo_transfer"]
        case .icsi: return ["icsi"]
        }
    }

    /// Single lookup: wireName + displayName + all aliases lowercased.
    static let byLower: [String: RecordEditKind] = {
        var dict: [String: RecordEditKind] = [:]
        for kind in allCases {
            dict[kind.wireName.lowercased()] = kind
            dict[kind.displayName.lowercased()] = kind
            for alias in kind.lowerAliases { dict[alias.lowercased()] = kind }
            dict[kind.rawValue.lowercased()] = kind
        }
        return dict
    }()
}

/// Push destinations for adding/editing patient records — single struct.
///
/// Derives wire/display via `kind`, eliminating 4×20 switches.
struct RecordEditRoute: Hashable, Identifiable {
    let kind: RecordEditKind
    let patientId: Int64
    let recordId: Int64?

    var id: String { "\(kind.wireName)-\(patientId)-\(recordId.map(String.init) ?? "new")" }

    init(kind: RecordEditKind, patientId: Int64, recordId: Int64? = nil) {
        self.kind = kind
        self.patientId = patientId
        self.recordId = recordId
    }

    init?(rawString: String, patientId: Int64, recordId: Int64?) {
        let key = rawString.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard let kind = RecordEditKind.byLower[key] else { return nil }
        self = Self(kind: kind, patientId: patientId, recordId: recordId)
    }

    // Back-compat: timeline/search passes lowercased displayType
    init?(displayType: String, patientId: Int64, recordId: Int64) {
        self.init(rawString: displayType, patientId: patientId, recordId: recordId)
    }

    init?(descriptor: RecordEditRouteDescriptor) {
        self.init(rawString: descriptor.typeName, patientId: descriptor.patientId, recordId: descriptor.recordId)
    }

    // Convenience derived strings
    var wireName: String { kind.wireName }
    var displayName: String { kind.displayName }
}

// MARK: - ViewBuilder registry — no AnyView erasure, preserves diffing

@ViewBuilder
func recordEditDestination(_ route: RecordEditRoute) -> some View {
    switch route.kind {
    case .weight: WeightEditView(patientId: route.patientId, weightId: route.recordId)
    case .vaccination: VaccinationEditView(patientId: route.patientId, vaccinationId: route.recordId)
    case .deworming: DewormingEditView(patientId: route.patientId, dewormingId: route.recordId)
    case .consultation: ConsultationEditView(patientId: route.patientId, consultationId: route.recordId)
    case .dentistry: DentistryEditView(patientId: route.patientId, dentistryId: route.recordId)
    case .farrierVisit: FarrierVisitEditView(patientId: route.patientId, farrierVisitId: route.recordId)
    case .anamnese: AnamneseEditView(patientId: route.patientId, anamneseId: route.recordId)
    case .lameness: LamenessEditView(patientId: route.patientId, lamenessId: route.recordId)
    case .surgery: SurgeryEditView(patientId: route.patientId, surgeryId: route.recordId)
    case .medication: MedicationEditView(patientId: route.patientId, medicationId: route.recordId)
    case .substance: SubstanceEditView(patientId: route.patientId, substanceId: route.recordId)
    case .labResult: LabResultEditView(patientId: route.patientId, labResultId: route.recordId)
    case .customReminder: CustomReminderEditView(patientId: route.patientId, customReminderId: route.recordId)
    case .reproductionEvent: ReproductionEventEditView(patientId: route.patientId, reproductionEventId: route.recordId)
    case .ultrasound: UltrasoundEditView(patientId: route.patientId, ultrasoundId: route.recordId)
    case .gestation: GestationEditView(patientId: route.patientId, gestationId: route.recordId)
    case .reproMedication: ReproMedicationEditView(patientId: route.patientId, reproMedicationId: route.recordId)
    case .imaging: ImagingEditView(patientId: route.patientId, imagingId: route.recordId)
    case .embryoTransfer: EmbryoTransferEditView(patientId: route.patientId, embryoTransferId: route.recordId)
    case .icsi: IcsiEditView(patientId: route.patientId, icsiId: route.recordId)
    }
}

// MARK: - Insights navigation

struct InsightsNavKey: Hashable {
    let patientId: Int64?
    let patientName: String?
}

/// Stable navigation key for drill-down — stores primitives, not display strings.
/// Hash/equality via epochDays + wire names so formatting changes cannot fragment navigation.
struct InsightsDrillDownKey: Hashable, Identifiable {
    let from: Kotlinx_datetimeLocalDate
    let to: Kotlinx_datetimeLocalDate
    let patientId: Int64?
    let recordType: RecordType?
    let reproductionEventType: ReproductionEventType?
    let dataIssueType: InsightsDataIssueType?

    var id: String {
        let pid = patientId ?? -1
        let rt = recordType?.wireName ?? "all"
        let et = reproductionEventType?.storageLabel ?? "all"
        let dt = dataIssueType?.name ?? "all"
        return "records-\(from.epochDaysCompat())-\(to.epochDaysCompat())-\(pid)-\(rt)-\(et)-\(dt)"
    }

    init(drillDown: InsightsDrillDown) {
        self.from = drillDown.from
        self.to = drillDown.to
        self.patientId = drillDown.patientId?.int64Value
        self.recordType = drillDown.recordType
        self.reproductionEventType = drillDown.reproductionEventType
        self.dataIssueType = drillDown.dataIssueType
    }

    init(from: Kotlinx_datetimeLocalDate, to: Kotlinx_datetimeLocalDate, patientId: Int64? = nil, recordType: RecordType? = nil, reproductionEventType: ReproductionEventType? = nil, dataIssueType: InsightsDataIssueType? = nil) {
        self.from = from
        self.to = to
        self.patientId = patientId
        self.recordType = recordType
        self.reproductionEventType = reproductionEventType
        self.dataIssueType = dataIssueType
    }

    var drillDown: InsightsDrillDown {
        InsightsDrillDown(
            from: from,
            to: to,
            patientId: patientId.map { KotlinLong(longLong: $0) },
            recordType: recordType,
            reproductionEventType: reproductionEventType,
            dataIssueType: dataIssueType
        )
    }

    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.from.epochDaysCompat() == rhs.from.epochDaysCompat() &&
        lhs.to.epochDaysCompat() == rhs.to.epochDaysCompat() &&
        lhs.patientId == rhs.patientId &&
        lhs.recordType == rhs.recordType &&
        lhs.reproductionEventType == rhs.reproductionEventType &&
        lhs.dataIssueType == rhs.dataIssueType
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(from.epochDaysCompat())
        hasher.combine(to.epochDaysCompat())
        hasher.combine(patientId ?? -1)
        hasher.combine(recordType?.wireName ?? "all")
        hasher.combine(reproductionEventType?.storageLabel ?? "all")
        hasher.combine(dataIssueType?.name ?? "all")
    }
}

// MARK: - Record detail key — single source

struct RecordDetailKey: Hashable, Identifiable {
    let displayType: String
    let patientId: Int64
    let recordId: Int64
    var id: String { "\(patientId)-\(recordId)-\(displayType)" }
}
