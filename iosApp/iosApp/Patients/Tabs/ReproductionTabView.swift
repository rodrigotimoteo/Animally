import SwiftUI
import Shared

struct ReproductionTabView: View {
    let patientId: Int64
    let gestationRefreshToken: Int
    @StateObject private var viewModel: ReproductionTabViewModel
    var onOpenRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        gestationRefreshToken: Int = 0,
        onOpenRecord: ((String, Int64) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: ReproductionTabViewModel(patientId: patientId))
        self.gestationRefreshToken = gestationRefreshToken
        self.onOpenRecord = onOpenRecord
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .accessibilityLabel("Loading reproduction records")
            } else if totalRecords == 0 {
                TabEmptyStateView(
                    icon: "heart.fill",
                    message: "No reproduction records yet"
                )
            } else {
                recordList
            }
        }
        .onAppear {
            viewModel.reload()
        }
        .onChange(of: gestationRefreshToken) { _, _ in
            viewModel.reload()
        }
        .accessibilityIdentifier("reproduction_tab_list")
    }

    private var totalRecords: Int {
        viewModel.reproductionEvents.totalCount +
        viewModel.ultrasounds.totalCount +
        viewModel.gestations.totalCount +
        viewModel.reproMedications.totalCount +
        viewModel.embryoTransfers.totalCount +
        viewModel.icsiRecords.totalCount
    }

    private var recordList: some View {
        List {
            if let active = viewModel.gestations.allItems
                .filter({ Self.isActive($0) })
                .max(by: { $0.breedingDate.epochDaysCompat() < $1.breedingDate.epochDaysCompat() }) {
                let gestationDay = viewModel.gestationDay(for: active)
                let daysUntilDue = viewModel.daysUntilDue(for: active)
                Section {
                    GestationCard(
                        gestation: active,
                        gestationDay: gestationDay,
                        daysUntilDue: daysUntilDue
                    ) {
                        onOpenRecord?("Gestation", active.id)
                    }
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            recordSection(
                RecordSectionSpec(
                    title: "Events",
                    icon: "heart.fill",
                    items: viewModel.reproductionEvents.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { Self.reproductionDisplay($0.eventType) },
                    rowSubtitle: { $0.details },
                    rowDate: { $0.date.displayString },
                    displayType: "Reproduction",
                    onDelete: { viewModel.deleteReproductionEvent($0.id) },
                    deleteTitle: "Reproduction Event",
                    display: viewModel.display(for: .reproductionEvents)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Gestations",
                    icon: "baby.fill",
                    items: viewModel.gestations.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { "Day \(viewModel.gestationDay(for: $0))" },
                    rowSubtitle: { $0.status },
                    rowDate: { $0.breedingDate.displayString },
                    displayType: "Gestation",
                    onDelete: { viewModel.deleteGestation($0.id) },
                    extraLine: { record in
                        var details = ["Due: \(record.expectedDueDate.displayString)"]
                        if let fetalCount = record.fetalCount {
                            details.append("\(fetalCount) fetus\(fetalCount.intValue > 1 ? "es" : "")")
                        }
                        return details.joined(separator: " · ")
                    },
                    extraLineLabel: nil,
                    display: viewModel.display(for: .gestations)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Ultrasounds",
                    icon: "waveform.path.ecg",
                    items: viewModel.ultrasounds.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { _ in "Reproductive Ultrasound" },
                    rowSubtitle: { $0.ovaryStatus },
                    rowDate: { $0.date.displayString },
                    displayType: "Ultrasound",
                    onDelete: { viewModel.deleteUltrasound($0.id) },
                    extraLine: { Self.ultrasoundExtraLine($0) },
                    extraLineLabel: nil,
                    display: viewModel.display(for: .ultrasounds)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Medications",
                    icon: "pills",
                    items: viewModel.reproMedications.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.medication },
                    rowSubtitle: { $0.purpose ?? $0.dosage },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Repro Medication",
                    onDelete: { viewModel.deleteReproMedication($0.id) },
                    display: viewModel.display(for: .reproMedications)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Embryo Transfers",
                    icon: "arrow.triangle.branch",
                    items: viewModel.embryoTransfers.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { "\($0.embryoCount) embryo\($0.embryoCount == 1 ? "" : "s")" },
                    rowSubtitle: { $0.recipientMares },
                    rowDate: { $0.date.displayString },
                    displayType: "Embryo Transfer",
                    onDelete: { viewModel.deleteEmbryoTransfer($0.id) },
                    display: viewModel.display(for: .embryoTransfers)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "ICSI",
                    icon: "scope",
                    items: viewModel.icsiRecords.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { "\($0.folliclesRecovered) follicle\($0.folliclesRecovered == 1 ? "" : "s") recovered" },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "ICSI",
                    onDelete: { viewModel.deleteIcsi($0.id) },
                    display: viewModel.display(for: .icsi)
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
    }

    private static func reproductionDisplay(_ raw: String) -> String {
        ReproductionEventTypes.shared.displayLabel(raw: raw)
    }

    private static func isActive(_ gestation: Gestation_) -> Bool {
        RecordDetailOpener.shared.isGestationActive(gestation: gestation)
    }

    private static func ultrasoundExtraLine(_ record: Ultrasound_) -> String? {
        var details: [String] = []
        if let follicleSize = record.follicleSizeMm {
            details.append(String(format: "Follicle: %.1f mm", locale: Locale(identifier: "en_US_POSIX"), follicleSize.doubleValue))
        }
        let findings = record.findings?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !findings.isEmpty {
            details.append(findings)
            return details.joined(separator: " · ")
        }
        let uterine = record.uterineStatus?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !uterine.isEmpty { details.append(uterine) }
        return details.isEmpty ? nil : details.joined(separator: " · ")
    }
}

// ActiveGestationCard removed — use shared GestationCard from RecordComponents.
private typealias ActiveGestationCard = GestationCard
