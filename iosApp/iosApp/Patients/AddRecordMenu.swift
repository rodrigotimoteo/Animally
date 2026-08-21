import SwiftUI

/// Catalog of record types that can be added from inside a patient,
/// grouped the same way as the patient detail tabs.
///
/// Each option carries an `isAvailable` flag: an option only becomes
/// tappable once a Swift-facing add/edit store AND a matching SwiftUI
/// screen exist for it in the iOS app. Today every shared `*EditViewModel`
/// is Android-only — see `IosRecordStores` (list stores, read-only).
struct AddRecordOption: Identifiable {
    let id: String
    let title: String
    let systemImage: String
    /// Builds the push destination for a given patient. `nil` means no iOS
    /// add/edit store + screen exists yet — the option renders disabled.
    let makeRoute: ((Int64) -> RecordEditRoute)?

    var isAvailable: Bool { makeRoute != nil }

    init(_ id: String, _ title: String, _ systemImage: String, makeRoute: ((Int64) -> RecordEditRoute)? = nil) {
        self.id = id
        self.title = title
        self.systemImage = systemImage
        self.makeRoute = makeRoute
    }
}

struct AddRecordSection: Identifiable {
    let id: String
    let title: String
    let options: [AddRecordOption]
}

enum AddRecordCatalog {
    private static func record(_ id: String, _ title: String, _ systemImage: String, route: @escaping (Int64) -> RecordEditRoute) -> AddRecordOption {
        AddRecordOption(id, title, systemImage, makeRoute: route)
    }

    static let sections: [AddRecordSection] = [
        AddRecordSection(id: "medical", title: "Medical", options: [
            record("anamnese", "Anamnese", "list.clipboard") { .anamnese(patientId: $0, recordId: nil) },
            record("consultation", "Consultation", "stethoscope") { .consultation(patientId: $0, recordId: nil) },
            record("lameness", "Lameness Evaluation", "figure.walk") { .lameness(patientId: $0, recordId: nil) },
            record("surgery", "Surgery", "cross.case.fill") { .surgery(patientId: $0, recordId: nil) },
            record("medication", "Medication", "pills") { .medication(patientId: $0, recordId: nil) },
            record("substance", "Controlled Substance", "lock.shield") { .substance(patientId: $0, recordId: nil) },
            record("labResult", "Lab Result", "testtube.2") { .labResult(patientId: $0, recordId: nil) },
        ]),
        AddRecordSection(id: "preventive", title: "Preventive", options: [
            record("vaccination", "Vaccination", "syringe") { .vaccination(patientId: $0, recordId: nil) },
            record("deworming", "Deworming", "worm") { .deworming(patientId: $0, recordId: nil) },
            record("dentistry", "Dentistry", "wrench.fill") { .dentistry(patientId: $0, recordId: nil) },
            record("farrierVisit", "Farrier Visit", "hammer.fill") { .farrierVisit(patientId: $0, recordId: nil) },
            record("weight", "Weight Entry", "scalemass") { .weight(patientId: $0, recordId: nil) },
            record("customReminder", "Custom Reminder", "bell.badge") { .customReminder(patientId: $0, recordId: nil) },
        ]),
        AddRecordSection(id: "reproduction", title: "Reproduction", options: [
            record("reproductionEvent", "Reproduction Event", "heart.circle") { .reproductionEvent(patientId: $0, recordId: nil) },
            record("ultrasound", "Ultrasound", "waveform.path.ecg") { .ultrasound(patientId: $0, recordId: nil) },
            record("gestation", "Gestation", "hourglass") { .gestation(patientId: $0, recordId: nil) },
            record("reproMedication", "Repro Medication", "capsule.fill") { .reproMedication(patientId: $0, recordId: nil) },
        ]),
        AddRecordSection(id: "diagnostics", title: "Diagnostics & Files", options: [
            record("imaging", "Imaging Study", "photo.stack") { .imaging(patientId: $0, recordId: nil) },
        ]),
    ]
}

/// Toolbar "+" menu listing every record type that can be attached to a
/// patient, grouped by detail-tab section. Options without an iOS add/edit
/// store render disabled and light up as stores land in the shared module.
struct AddRecordMenu: View {
    let patientId: Int64
    /// Called with the route of a tapped, available option. The owner view
    /// pushes it via `.navigationDestination(item:)`.
    let onSelect: (RecordEditRoute) -> Void

    var body: some View {
        Menu {
            ForEach(AddRecordCatalog.sections) { section in
                Section(section.title) {
                    ForEach(section.options) { option in
                        Button {
                            if let route = option.makeRoute?(patientId) {
                                onSelect(route)
                            }
                        } label: {
                            Label(option.title, systemImage: option.systemImage)
                        }
                        .disabled(!option.isAvailable)
                    }
                }
            }
        } label: {
            Image(systemName: "plus")
        }
        .accessibilityLabel("Add record")
    }
}
