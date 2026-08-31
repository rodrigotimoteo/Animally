import SwiftUI
import Shared

/// Research-readiness section with explicit issue counts and drill-down.
///
/// Each rule has a written definition surfaced as "missing for analysis"
/// rather than clinically wrong, and each row drills to the affected source
/// rows via `InsightsRecordsView` (reusing `RecordDetailKey` navigation).
/// No combined quality score is used.
struct InsightsReadinessSection: View {
    let dataIssues: [InsightsDataIssueCount]
    var isDrillDownEnabled = true
    var onSelectIssue: ((InsightsDataIssueType) -> Void)?

    private var totalIssues: Int {
        dataIssues.reduce(0) { $0 + Int($1.count) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header
            if dataIssues.isEmpty {
                emptyState
            } else {
                issueList
            }
            footer
        }
        .padding(14)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("insights_readiness_section")
    }

    private var header: some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
                .accessibilityHidden(true)
            Text("Research readiness")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Spacer()
            Text("\(totalIssues) missing for analysis")
                .font(.caption.weight(.semibold))
                .foregroundStyle(totalIssues > 0 ? Theme.amber : Theme.textSecondary)
                .accessibilityLabel("Total missing for analysis \(totalIssues)")
        }
        .accessibilityAddTraits(.isHeader)
        .accessibilityElement(children: .combine)
    }

    private var issueList: some View {
        VStack(spacing: 0) {
            ForEach(dataIssues, id: \.type) { issue in
                let type = issue.type
                let count = issue.count
                Button {
                    onSelectIssue?(type)
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: iconFor(type))
                            .foregroundStyle(Theme.amber)
                            .frame(width: 28, height: 28)
                            .background(Theme.amber.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 7))
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(type.displayName)
                                .font(.subheadline.weight(.medium))
                                .foregroundStyle(Theme.textPrimary)
                                .lineLimit(1)
                            Text(type.missingForAnalysisLabel)
                                .font(.caption2)
                                .foregroundStyle(Theme.textSecondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Text("\(count)")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Theme.textPrimary)
                            .accessibilityLabel("\(count) records")
                        Image(systemName: "chevron.right")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(Theme.textTertiary)
                            .accessibilityHidden(true)
                    }
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(!isDrillDownEnabled)
                .opacity(isDrillDownEnabled ? 1 : 0.52)
                .accessibilityLabel("\(type.displayName), \(count) missing for analysis")
                .accessibilityHint("Shows records missing for analysis")
                .accessibilityIdentifier("insights_readiness_\(type.name.lowercased())")
                if issue.type != dataIssues.last?.type {
                    Divider().opacity(0.5)
                }
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
    }

    private var emptyState: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.forestGreen)
                .accessibilityHidden(true)
            Text("No research-readiness issues in this period")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
        }
        .padding(12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityLabel("No research-readiness issues in this period")
    }

    private var footer: some View {
        Text("Records flagged as missing for analysis — not clinically wrong. Each row shows only the affected source records.")
            .font(.caption2)
            .foregroundStyle(Theme.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityLabel("Records flagged as missing for analysis, not clinically wrong")
    }

    private func iconFor(_ type: InsightsDataIssueType) -> String {
        switch type {
        case .unknownreproductioncategory: return "questionmark.circle.fill"
        case .missingvetname: return "stethoscope"
        case .unlinkedowner: return "person.crop.circle.badge.exclamationmark"
        case .freetextembryorecipient: return "arrow.triangle.branch"
        case .incompleteultrasounddata: return "waveform.path.ecg"
        default: return "exclamationmark.triangle.fill"
        }
    }
}
