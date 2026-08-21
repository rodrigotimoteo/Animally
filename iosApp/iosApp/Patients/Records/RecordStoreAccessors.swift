import Foundation
import Shared

/// Single resolution point for record edit stores whose factories may live
/// on either `IosEditStores` or `IosEditStoresRepro` (stores land in a
/// parallel lane). If a factory ends up on the other shared object, the fix
/// is one line here — views never reference the shared objects directly.
enum RecordStores {
    // MARK: Medical / Preventive group (IosEditStores)

    static func anamneseEditStore(patientId: Int64, anamneseId: Int64?) -> AnamneseEditStore {
        IosEditStoresMedical.shared.anamneseEditStore(
            patientId: patientId,
            anamneseId: anamneseId.map { KotlinLong(longLong: $0) }
        )
    }

    static func lamenessEditStore(patientId: Int64, lamenessId: Int64?) -> LamenessEditStore {
        IosEditStoresMedical.shared.lamenessEditStore(
            patientId: patientId,
            lamenessId: lamenessId.map { KotlinLong(longLong: $0) }
        )
    }

    static func surgeryEditStore(patientId: Int64, surgeryId: Int64?) -> SurgeryEditStore {
        IosEditStoresMedical.shared.surgeryEditStore(
            patientId: patientId,
            surgeryId: surgeryId.map { KotlinLong(longLong: $0) }
        )
    }

    static func medicationEditStore(patientId: Int64, medicationId: Int64?) -> MedicationEditStore {
        IosEditStoresMedical.shared.medicationEditStore(
            patientId: patientId,
            medicationId: medicationId.map { KotlinLong(longLong: $0) }
        )
    }

    static func substanceEditStore(patientId: Int64, substanceId: Int64?) -> SubstanceEditStore {
        IosEditStoresMedical.shared.substanceEditStore(
            patientId: patientId,
            substanceId: substanceId.map { KotlinLong(longLong: $0) }
        )
    }

    static func labResultEditStore(patientId: Int64, labResultId: Int64?) -> LabResultEditStore {
        IosEditStoresMedical.shared.labResultEditStore(
            patientId: patientId,
            labResultId: labResultId.map { KotlinLong(longLong: $0) }
        )
    }

    static func customReminderEditStore(patientId: Int64, customReminderId: Int64?) -> CustomReminderEditStore {
        IosEditStoresCare.shared.customReminderEditStore(
            patientId: patientId,
            reminderId: customReminderId.map { KotlinLong(longLong: $0) }
        )
    }

    static func imagingEditStore(patientId: Int64, imagingId: Int64?) -> ImagingEditStore {
        IosEditStoresFiles.shared.imagingEditStore(
            patientId: patientId,
            imagingId: imagingId.map { KotlinLong(longLong: $0) }
        )
    }

    // MARK: Reproduction group (IosEditStoresRepro)

    static func reproductionEventEditStore(patientId: Int64, reproductionEventId: Int64?) -> ReproductionEventEditStore {
        IosEditStoresRepro.shared.reproductionEventEditStore(
            patientId: patientId,
            reproductionEventId: reproductionEventId.map { KotlinLong(longLong: $0) }
        )
    }

    static func ultrasoundEditStore(patientId: Int64, ultrasoundId: Int64?) -> UltrasoundEditStore {
        IosEditStoresRepro.shared.ultrasoundEditStore(
            patientId: patientId,
            ultrasoundId: ultrasoundId.map { KotlinLong(longLong: $0) }
        )
    }

    static func gestationEditStore(patientId: Int64, gestationId: Int64?) -> GestationEditStore {
        IosEditStoresRepro.shared.gestationEditStore(
            patientId: patientId,
            gestationId: gestationId.map { KotlinLong(longLong: $0) }
        )
    }

    static func reproMedicationEditStore(patientId: Int64, reproMedicationId: Int64?) -> ReproMedicationEditStore {
        IosEditStoresRepro.shared.reproMedicationEditStore(
            patientId: patientId,
            reproMedicationId: reproMedicationId.map { KotlinLong(longLong: $0) }
        )
    }
}
