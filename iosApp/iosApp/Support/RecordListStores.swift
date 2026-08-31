import Shared

/// Central facade over the two Kotlin store factories (`IosRecordStores` and `IosReproAndDiagnosticsStores`).
/// Eliminates lane-split leak in `ReproductionTabViewModel` — callers use one surface.
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

    // MARK: - Medical / Preventive lane (via IosRecordStores)
    static func embryoTransferListStore(patientId: Int64) -> EmbryoTransferListStore {
        IosRecordStores.shared.embryoTransferListStore(patientId: patientId)
    }
    static func icsiListStore(patientId: Int64) -> IcsiListStore {
        IosRecordStores.shared.icsiListStore(patientId: patientId)
    }
    static func consultationListStore(patientId: Int64) -> ConsultationListStore {
        IosRecordStores.shared.consultationListStore(patientId: patientId)
    }
    static func lamenessListStore(patientId: Int64) -> LamenessListStore {
        IosRecordStores.shared.lamenessListStore(patientId: patientId)
    }
    static func surgeryListStore(patientId: Int64) -> SurgeryListStore {
        IosRecordStores.shared.surgeryListStore(patientId: patientId)
    }
    static func medicationListStore(patientId: Int64) -> MedicationListStore {
        IosRecordStores.shared.medicationListStore(patientId: patientId)
    }
    static func substanceListStore(patientId: Int64) -> SubstanceListStore {
        IosRecordStores.shared.substanceListStore(patientId: patientId)
    }
    static func weightListStore(patientId: Int64) -> WeightListStore {
        IosRecordStores.shared.weightListStore(patientId: patientId)
    }
    static func vaccinationListStore(patientId: Int64) -> VaccinationListStore {
        IosRecordStores.shared.vaccinationListStore(patientId: patientId)
    }
    static func dewormingListStore(patientId: Int64) -> DewormingListStore {
        IosRecordStores.shared.dewormingListStore(patientId: patientId)
    }
    static func dentistryListStore(patientId: Int64) -> DentistryListStore {
        IosRecordStores.shared.dentistryListStore(patientId: patientId)
    }
    static func farrierVisitListStore(patientId: Int64) -> FarrierVisitListStore {
        IosRecordStores.shared.farrierVisitListStore(patientId: patientId)
    }
    static func upcomingCareStore(patientId: Int64) -> UpcomingCareStore {
        IosRecordStores.shared.upcomingCareStore(patientId: patientId)
    }
}
