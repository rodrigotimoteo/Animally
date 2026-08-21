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
            AddRecordOption("anamnese", "Anamnese", "list.clipboard"),
            record("consultation", "Consultation", "stethoscope") { .consultation(patientId: $0, recordId: nil) },
            AddRecordOption("lameness", "Lameness Evaluation", "figure.walk"),
            AddRecordOption("surgery", "Surgery", "cross.case.fill"),
            AddRecordOption("medication", "Medication", "pills"),
            AddRecordOption("substance", "Controlled Substance", "lock.shield"),
            AddRecordOption("labResult", "Lab Result", "testtube.2"),
        ]),
        AddRecordSection(id: "preventive", title: "Preventive", options: [
            record("vaccination", "Vaccination", "syringe") { .vaccination(patientId: $0, recordId: nil) },
            record("deworming", "Deworming", "worm") { .deworming(patientId: $0, recordId: nil) },
            record("dentistry", "Dentistry", "wrench.fill") { .dentistry(patientId: $0, recordId: nil) },
            record("farrierVisit", "Farrier Visit", "hammer.fill") { .farrierVisit(patientId: $0, recordId: nil) },
            record("weight", "Weight Entry", "scalemass") { .weight(patientId: $0, recordId: nil) },
            AddRecordOption("customReminder", "Custom Reminder", "bell.badge"),
        ]),
        AddRecordSection(id: "reproduction", title: "Reproduction", options: [
            AddRecordOption("reproductionEvent", "Reproduction Event", ("heart.circle")),
            AddRecordOption("ultrasound", "Ultrasound", ("waveform.path.ecg")),
            AddRecordOption("gestation", "Gestation", "hourglass"),
            AddRecordOption("reproMedication", "Repro Medication", "capsule.fill"),
        ]),
        AddRecordSection(id: "diagnostics", title: "Diagnostics & Files", options: [
            AddRecordOption("imaging", "Imaging Study", "photo.stack"),
        ]),
    ]
}

/// Toolbar "+" menu listing every record type that can be attached to a
/// patient, grouped by detail-tab section. Unavailable types stay visible
/// but disabled so the roadmap is discoverable; they light up as iOS
/// add/edit stores land in the shared module.
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
