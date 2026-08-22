package com.github.rodrigotimoteo.animally.di.database

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseQueries
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationQueries
import com.github.rodrigotimoteo.animally.data.customreminder.CustomReminderQueries
import com.github.rodrigotimoteo.animally.data.dentistry.DentistryQueries
import com.github.rodrigotimoteo.animally.data.deworming.DewormingQueries
import com.github.rodrigotimoteo.animally.data.embryotransfer.EmbryoTransferQueries
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitQueries
import com.github.rodrigotimoteo.animally.data.follicle.FollicleQueries
import com.github.rodrigotimoteo.animally.data.gestation.GestationQueries
import com.github.rodrigotimoteo.animally.data.icsi.IcsiQueries
import com.github.rodrigotimoteo.animally.data.imaging.ImagingQueries
import com.github.rodrigotimoteo.animally.data.labresult.LabResultQueries
import com.github.rodrigotimoteo.animally.data.lameness.LamenessQueries
import com.github.rodrigotimoteo.animally.data.medication.MedicationQueries
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.data.patient.PatientQueries
import com.github.rodrigotimoteo.animally.data.reproduction.ReproductionQueries
import com.github.rodrigotimoteo.animally.data.repromedication.ReproMedicationQueries
import com.github.rodrigotimoteo.animally.data.substance.SubstanceQueries
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryQueries
import com.github.rodrigotimoteo.animally.data.sync.SyncStateQueries
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundQueries
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationQueries
import com.github.rodrigotimoteo.animally.data.weight.WeightQueries
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ObjCHidden
internal class QueriesModule {
    fun provide() =
        module {
            single<AnamneseQueries> { get<AnimallyDatabase>().anamneseQueries }
            single<ConsultationQueries> { get<AnimallyDatabase>().consultationQueries }
            single<CustomReminderQueries> { get<AnimallyDatabase>().customReminderQueries }
            single<DentistryQueries> { get<AnimallyDatabase>().dentistryQueries }
            single<DewormingQueries> { get<AnimallyDatabase>().dewormingQueries }
            single<EmbryoTransferQueries> { get<AnimallyDatabase>().embryoTransferQueries }
            single<FollicleQueries> { get<AnimallyDatabase>().follicleQueries }
            single<IcsiQueries> { get<AnimallyDatabase>().icsiQueries }
            single<FarrierVisitQueries> { get<AnimallyDatabase>().farrierVisitQueries }
            single<GestationQueries> { get<AnimallyDatabase>().gestationQueries }
            single<ImagingQueries> { get<AnimallyDatabase>().imagingQueries }
            single<LabResultQueries> { get<AnimallyDatabase>().labResultQueries }
            single<LamenessQueries> { get<AnimallyDatabase>().lamenessQueries }
            single<MedicationQueries> { get<AnimallyDatabase>().medicationQueries }
            single<OwnerQueries> { get<AnimallyDatabase>().ownerQueries }
            single<PatientQueries> { get<AnimallyDatabase>().patientQueries }
            single<ReproductionQueries> { get<AnimallyDatabase>().reproductionQueries }
            single<ReproMedicationQueries> { get<AnimallyDatabase>().reproMedicationQueries }
            single<SubstanceQueries> { get<AnimallyDatabase>().substanceQueries }
            single<SurgeryQueries> { get<AnimallyDatabase>().surgeryQueries }
            single<SyncStateQueries> { get<AnimallyDatabase>().syncStateQueries }
            single<UltrasoundQueries> { get<AnimallyDatabase>().ultrasoundQueries }
            single<VaccinationQueries> { get<AnimallyDatabase>().vaccinationQueries }
            single<WeightQueries> { get<AnimallyDatabase>().weightQueries }
        }
}
