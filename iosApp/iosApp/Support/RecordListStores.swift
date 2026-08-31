import Shared

/// Central facade over the Kotlin store factories grouped by record domain.
/// SwiftUI callers use one surface while Kotlin owns each store's state and
/// business logic.
enum RecordListStores {
    // MARK: - Reproduction / Diagnostics lane (via IosReproAndDiagnosticsStores)
    static func reproductionListStore(patientId: Int64) -> ReproductionListStore {
        IosReproAndDiagnosticsStores.shared.reproductionListStore(patientId: patientId)
    }
    static func ultrasoundListStore(patientId: Int64) -> UltrasoundListStore {
        IosReproAndDiagnosticsStores.shared.ultrasoundListStore(patientId: patientId)
    }
    static func gestationListStore(patientId: Int64) -> GestationListStore {
        IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
    }
    static func reproMedicationListStore(patientId: Int64) -> ReproMedicationListStore {
        IosReproAndDiagnosticsStores.shared.reproMedicationListStore(patientId: patientId)
    }
    static func labResultListStore(patientId: Int64) -> LabResultListStore {
        IosReproAndDiagnosticsStores.shared.labResultListStore(patientId: patientId)
    }
    static func imagingListStore(patientId: Int64) -> ImagingListStore {
        IosReproAndDiagnosticsStores.shared.imagingListStore(patientId: patientId)
    }
    static func embryoTransferListStore(patientId: Int64) -> EmbryoTransferListStore {
        IosReproAndDiagnosticsStores.shared.embryoTransferListStore(patientId: patientId)
    }
    static func icsiListStore(patientId: Int64) -> IcsiListStore {
        IosReproAndDiagnosticsStores.shared.icsiListStore(patientId: patientId)
    }

    // MARK: - Medical lane (via IosMedicalRecordStores)
    static func consultationListStore(patientId: Int64) -> ConsultationListStore {
        IosMedicalRecordStores.shared.consultationListStore(patientId: patientId)
    }
    static func lamenessListStore(patientId: Int64) -> LamenessListStore {
        IosMedicalRecordStores.shared.lamenessListStore(patientId: patientId)
    }
    static func surgeryListStore(patientId: Int64) -> SurgeryListStore {
        IosMedicalRecordStores.shared.surgeryListStore(patientId: patientId)
    }
    static func medicationListStore(patientId: Int64) -> MedicationListStore {
        IosMedicalRecordStores.shared.medicationListStore(patientId: patientId)
    }
    static func substanceListStore(patientId: Int64) -> SubstanceListStore {
        IosMedicalRecordStores.shared.substanceListStore(patientId: patientId)
    }

    // MARK: - Preventive lane (via IosPreventiveRecordStores)
    static func weightListStore(patientId: Int64) -> WeightListStore {
        IosPreventiveRecordStores.shared.weightListStore(patientId: patientId)
    }
    static func vaccinationListStore(patientId: Int64) -> VaccinationListStore {
        IosPreventiveRecordStores.shared.vaccinationListStore(patientId: patientId)
    }
    static func dewormingListStore(patientId: Int64) -> DewormingListStore {
        IosPreventiveRecordStores.shared.dewormingListStore(patientId: patientId)
    }
    static func dentistryListStore(patientId: Int64) -> DentistryListStore {
        IosPreventiveRecordStores.shared.dentistryListStore(patientId: patientId)
    }
    static func farrierVisitListStore(patientId: Int64) -> FarrierVisitListStore {
        IosPreventiveRecordStores.shared.farrierVisitListStore(patientId: patientId)
    }
    static func upcomingCareStore(patientId: Int64) -> UpcomingCareStore {
        IosCareStores.shared.upcomingCareStore(patientId: patientId)
    }
}
