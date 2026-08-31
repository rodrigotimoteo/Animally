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
    /// Builds the push destination for a given patient. `nil` is reserved for
    /// record types that are not yet supported by the iOS editor.
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
            record("anamnese", "Anamnese", "list.clipboard") { RecordEditRoute(kind: .anamnese, patientId: $0) },
            record("consultation", "Consultation", "stethoscope") { RecordEditRoute(kind: .consultation, patientId: $0) },
            record("lameness", "Lameness Evaluation", "figure.walk") { RecordEditRoute(kind: .lameness, patientId: $0) },
            record("surgery", "Surgery", "cross.case.fill") { RecordEditRoute(kind: .surgery, patientId: $0) },
            record("medication", "Medication", "pills") { RecordEditRoute(kind: .medication, patientId: $0) },
            record("substance", "Controlled Substance", "lock.shield") { RecordEditRoute(kind: .substance, patientId: $0) },
            record("labResult", "Lab Result", "testtube.2") { RecordEditRoute(kind: .labResult, patientId: $0) },
        ]),
        AddRecordSection(id: "preventive", title: "Preventive", options: [
            record("vaccination", "Vaccination", "syringe") { RecordEditRoute(kind: .vaccination, patientId: $0) },
            record("deworming", "Deworming", "worm") { RecordEditRoute(kind: .deworming, patientId: $0) },
            record("dentistry", "Dentistry", "wrench.fill") { RecordEditRoute(kind: .dentistry, patientId: $0) },
            record("farrierVisit", "Farrier Visit", "hammer.fill") { RecordEditRoute(kind: .farrierVisit, patientId: $0) },
            record("weight", "Weight Entry", "scalemass") { RecordEditRoute(kind: .weight, patientId: $0) },
            record("customReminder", "Custom Reminder", "bell.badge") { RecordEditRoute(kind: .customReminder, patientId: $0) },
        ]),
        AddRecordSection(id: "reproduction", title: "Reproduction", options: [
            record("reproductionEvent", "Reproduction Event", "heart.circle") { RecordEditRoute(kind: .reproductionEvent, patientId: $0) },
            record("ultrasound", "Ultrasound", "waveform.path.ecg") { RecordEditRoute(kind: .ultrasound, patientId: $0) },
            record("gestation", "Gestation", "hourglass") { RecordEditRoute(kind: .gestation, patientId: $0) },
            record("reproMedication", "Repro Medication", "capsule.fill") { RecordEditRoute(kind: .reproMedication, patientId: $0) },
            record("embryoTransfer", "Embryo Transfer", "arrow.triangle.branch") { RecordEditRoute(kind: .embryoTransfer, patientId: $0) },
            record("icsi", "ICSI", "scope") { RecordEditRoute(kind: .icsi, patientId: $0) },
        ]),
        AddRecordSection(id: "diagnostics", title: "Diagnostics & Files", options: [
            record("imaging", "Imaging Study", "photo.stack") { RecordEditRoute(kind: .imaging, patientId: $0) },
        ]),
    ]
}

/// Toolbar "+" menu listing every record type that can be attached to a
/// patient, grouped by detail-tab section. Unsupported options remain visible
/// but disabled so the catalog documents the complete record surface.
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
                        .accessibilityIdentifier("add_record_\(option.id)")
                    }
                }
            }
        } label: {
            Image(systemName: "plus")
        }
        .accessibilityLabel("Add record")
    }
}
