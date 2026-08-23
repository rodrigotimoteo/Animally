import Foundation
import Shared

/// Pure field-row builders for the id-loaded read-only record detail.
///
/// One function per display type; each mirrors the eager-mode field list
/// written in the matching tab view's tap builder (same labels, same order),
/// but sources values from the Kotlin edit store's loaded FormState instead
/// of an in-memory entity. Dates arrive as ISO `yyyy-MM-dd` strings — the
/// same format the tab views render via `LocalDate.displayString`.
enum RecordDetailContent {
    /// Renders an ISO `yyyy-MM-dd` string for display. Identity today (the
    /// tab views show ISO dates too); single place to change later.
    static func isoToDisplay(_ iso: String?) -> String {
        iso ?? ""
    }

    private static func rows(_ pairs: [(String, String?)]) -> [RecordDetailNav.FieldRow] {
        pairs.map { RecordDetailNav.FieldRow(label: $0.0, value: $0.1 ?? "") }
            .filter { !$0.value.isEmpty }
    }

    // MARK: - Medical

    static func consultationFields(_ f: ConsultationFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Subjective", f.subjective),
            ("Objective", f.objective),
            ("Assessment", f.assessment),
            ("Plan", f.plan),
            ("Veterinarian", f.vetName),
            ("Next Visit", isoToDisplay(f.nextVisitDate)),
        ])
    }

    static func lamenessFields(_ f: LamenessFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("AAEP Grade", f.gradeAAEP),
            ("Limb Location", f.limbLocation),
            ("Flexion Test", f.flexionTest),
            ("Diagnosis", f.diagnosis),
            ("Treatment", f.treatment),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func surgeryFields(_ f: SurgeryFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Type", f.type),
            ("Description", f.description),
            ("Outcome", f.outcome),
            ("Surgeon", f.surgeon),
            ("Anesthesia", f.anesthesia),
            ("Analgesia", f.analgesia),
            ("Complications", f.complications),
            ("Recovery Notes", f.recoveryNotes),
        ])
    }

    static func medicationFields(_ f: MedicationFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Name", f.name),
            ("Dosage", f.dosage),
            ("Route", f.route),
            ("Frequency", f.frequency),
            ("Start Date", isoToDisplay(f.startDate)),
            ("End Date", isoToDisplay(f.endDate)),
            ("Prescribed By", f.prescribedBy),
            ("Notes", f.notes),
        ])
    }

    static func substanceFields(_ f: ControlledSubstanceFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Drug Name", f.drugName),
            ("Dose", f.dose),
            ("Unit", f.unit),
            ("Route", f.route),
            ("Date", isoToDisplay(f.date)),
            ("Administered By", f.administeredBy),
            ("Witness", f.witness),
            ("Reason", f.reason),
            ("Notes", f.notes),
        ])
    }

    static func weightFields(_ f: WeightFormState) -> [RecordDetailNav.FieldRow] {
        var weight = ""
        if let kg = Double(f.weightKg) {
            weight = String(format: "%.1f kg", kg)
        }
        return rows([
            ("Date", isoToDisplay(f.date)),
            ("Weight (kg)", weight),
            ("Notes", f.notes),
        ])
    }

    // MARK: - Diagnostics

    static func labResultFields(_ f: LabResultFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Test Type", f.testType),
            ("Results", f.results),
            ("Normal Range", f.normalRange),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func imagingFields(_ f: ImagingFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Type", f.type),
            ("Findings", f.findings),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    // MARK: - Preventive

    static func vaccinationFields(_ f: VaccinationFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date Administered", isoToDisplay(f.dateAdministered)),
            ("Vaccine", f.vaccineName),
            ("Batch Number", f.batchNumber),
            ("Site", f.site),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func dewormingFields(_ f: DewormingFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Product", f.product),
            ("Dose", f.dose),
            ("Date Administered", isoToDisplay(f.dateAdministered)),
            ("Next Due", isoToDisplay(f.nextDueDate)),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func dentistryFields(_ f: DentistryFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Treatment", f.treatment),
            ("Findings", f.findings),
            ("Next Due", isoToDisplay(f.nextDueDate)),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func farrierFields(_ f: FarrierVisitFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Trim or Shoe", f.trimOrShoe),
            ("Shoe Type", f.shoeType),
            ("Findings", f.findings),
            ("Next Due", isoToDisplay(f.nextDueDate)),
            ("Farrier", f.farrier),
            ("Notes", f.notes),
        ])
    }

    // MARK: - Reproduction

    static func reproductionFields(_ f: ReproductionEventFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Event Type", f.eventType),
            ("Details", f.details),
            ("Initial Exam Findings", f.initialExamFindings),
            ("Stallion", f.stallionName),
            ("Breeding Type", f.breedingType),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func ultrasoundFields(_ f: UltrasoundFormState) -> [RecordDetailNav.FieldRow] {
        var fields: [RecordDetailNav.FieldRow] = [
            .init(label: "Date", value: isoToDisplay(f.date)),
            .init(label: "Ovary Status", value: f.ovaryStatus ?? ""),
            .init(label: "Uterine Status", value: f.uterineStatus ?? ""),
        ]
        fields.append(.init(label: "Left Ovary Status", value: f.leftOvaryStatus ?? ""))
        let leftSummary = follicleSummary(f.leftFollicles)
        if !leftSummary.isEmpty {
            fields.append(.init(label: "Left Follicles", value: leftSummary))
        }
        fields.append(.init(label: "Right Ovary Status", value: f.rightOvaryStatus ?? ""))
        let rightSummary = follicleSummary(f.rightFollicles)
        if !rightSummary.isEmpty {
            fields.append(.init(label: "Right Follicles", value: rightSummary))
        }
        fields.append(.init(label: "Uterine Edema", value: f.uterineEdema ?? ""))
        if f.uterineLiquid == true, let fluid = f.uterineLiquidDescription {
            fields.append(.init(label: "Fluid Description", value: fluid))
        }
        fields.append(.init(label: "Uterus Description", value: f.uterusDescription ?? ""))
        fields.append(.init(label: "Findings", value: f.findings ?? ""))
        fields.append(.init(label: "Veterinarian", value: f.vetName ?? ""))
        fields.append(.init(label: "Notes", value: f.notes ?? ""))
        return fields.filter { !$0.value.isEmpty }
    }

    /// Renders recorded follicles as one compact line: "12.5 mm, 9 mm — note".
    private static func follicleSummary(_ follicles: [FollicleRow]) -> String {
        let parts = follicles.compactMap { follicle -> String? in
            let size = follicle.sizeMm.trimmingCharacters(in: .whitespaces)
            guard !size.isEmpty else { return nil }
            if let note = follicle.note, !note.isEmpty {
                return "\(size) mm — \(note)"
            }
            return "\(size) mm"
        }
        return parts.joined(separator: ", ")
    }

    static func gestationFields(_ f: GestationFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Breeding Date", isoToDisplay(f.breedingDate)),
            ("Status", f.status),
            ("Fetal Count", f.fetalCount),
            ("Last Check Date", isoToDisplay(f.lastCheckDate)),
            ("Notes", f.notes),
        ])
    }

    static func reproMedicationFields(_ f: ReproMedicationFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Medication", f.medication),
            ("Date Administered", isoToDisplay(f.dateAdministered)),
            ("Dosage", f.dosage),
            ("Purpose", f.purpose),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func embryoTransferFields(_ f: EmbryoTransferFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Embryo Count", f.embryoCount),
            ("Recipient Mares", f.recipientMares),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }

    static func icsiFields(_ f: IcsiFormState) -> [RecordDetailNav.FieldRow] {
        rows([
            ("Date", isoToDisplay(f.date)),
            ("Follicles Recovered", f.folliclesRecovered),
            ("Veterinarian", f.vetName),
            ("Notes", f.notes),
        ])
    }
}
