import SwiftUI
import Shared

struct ReproductionTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: ReproductionTabViewModel
    /// Fires when a record row is tapped; carries displayType+recordId for lazy open via RecordDetailOpener.
    var onOpenRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: ReproductionTabViewModel(patientId: patientId))
        self.refreshToken = refreshToken
        self.onOpenRecord = onOpenRecord
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
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
            // Recalculate time-based gestation values whenever this tab becomes
            // visible, including after the app has crossed into a new day.
            viewModel.reload()
        }
        .onChange(of: refreshToken) { _, _ in
            viewModel.reload()
        }
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
            // Active pregnancy pinned to the very top — most important info.
            // Active check delegated to Kotlin single source (Gestation.isActivePregnancy).
            if let active = viewModel.gestations.allItems
                .filter({ Self.isActive($0) })
                .max(by: { $0.breedingDate.epochDaysCompat() < $1.breedingDate.epochDaysCompat() }) {
                let gestationDay = viewModel.gestationDay(for: active)
                let daysUntilDue = viewModel.daysUntilDue(for: active)
                Section {
                    ActiveGestationCard(
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

            // Reproduction Events
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

            // Gestations
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

            // Ultrasounds
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

            // Repro Medications
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

            // Embryo Transfers
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

            // ICSI
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

    /// Canonical display for reproduction event types: ensures legacy
    /// `PregnancyCheck` / `pregnancy_check` variants render as `Pregnancy Check`
    /// without persisting a migration. Unknown values surface trimmed raw.
    private static func reproductionDisplay(_ raw: String) -> String {
        ReproductionEventTypes.shared.displayLabel(raw: raw)
    }

    /// Single source: delegates to Kotlin RecordDetailOpener.isGestationActive.
    private static func isActive(_ gestation: Gestation_) -> Bool {
        RecordDetailOpener.shared.isGestationActive(gestation: gestation)
    }

    /// One-line identifying summary for an ultrasound card: findings text,
    /// follicle size, then uterine status when findings are empty.
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

/// Prominent pinned card for an active pregnancy: breeding date, computed
/// gestation day count, and the expected foaling date front and center.
/// Amber accents once foaling is within 30 days or overdue.
/// DaysUntilDue/gestationDay injected from viewModel shared today to avoid drift vs GetInsightsDashboardUseCase todayProvider.
private struct ActiveGestationCard: View {
    let gestation: Gestation_
    let gestationDay: Int
    let daysUntilDue: Int
    let onTap: () -> Void

    private static let dueSoonDays = 30

    private var isDueSoon: Bool {
        daysUntilDue <= Self.dueSoonDays
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "heart.circle.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Theme.forestGreen)

                    VStack(alignment: .leading, spacing: 4) {
                        Text("In Foal")
                            .font(.caption.weight(.bold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Theme.forestGreen)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())

                        Text("Day \(gestationDay)")
                            .font(.title.weight(.bold))
                            .foregroundStyle(Theme.textPrimary)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        Text(gestation.expectedDueDate.friendlyString)
                            .font(.headline)
                            .foregroundStyle(isDueSoon ? Theme.amber : Theme.textPrimary)
                        Text(dueLabel)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(isDueSoon ? Theme.amber : Theme.textSecondary)
                    }
                }

                Divider()

                HStack(spacing: 16) {
                    Label {
                        Text("Bred \(gestation.breedingDate.friendlyString)")
                    } icon: {
                        Image(systemName: "calendar")
                    }
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)

                    if let fetalCount = gestation.fetalCount {
                        Label {
                            Text("\(fetalCount) fetus\(fetalCount.intValue > 1 ? "es" : "")")
                        } icon: {
                            Image(systemName: "number")
                        }
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.textTertiary)
                }
            }
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("In foal, day \(gestationDay), due \(gestation.expectedDueDate.friendlyString)")
    }

    private var dueLabel: String {
        let days = daysUntilDue
        if days < 0 { return "Overdue by \(-days) day\(-days == 1 ? "" : "s")" }
        if days == 0 { return "Due today" }
        return "Due in \(days) day\(days == 1 ? "" : "s")"
    }
}
