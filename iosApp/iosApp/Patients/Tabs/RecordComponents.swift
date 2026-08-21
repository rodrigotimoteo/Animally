import SwiftUI
import Shared

// MARK: - Date Formatting Helper

extension Kotlinx_datetimeLocalDate {
    var displayString: String {
        "\(year)-\(String(format: "%02d", monthNumber))-\(String(format: "%02d", dayOfMonth))"
    }
}

// MARK: - Generic Record Row

/// Standard row for displaying a record with date, title, subtitle, and icon.
struct RecordRowView: View {
    let icon: String
    let iconTint: Color
    let title: String
    let subtitle: String?
    let date: String?

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(iconTint)
                .frame(width: 36, height: 36)
                .background(iconTint.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                    .lineLimit(1)

                if let subtitle = subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Theme.textSecondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            if let date = date {
                Text(date)
                    .font(.caption)
                    .foregroundStyle(Theme.textTertiary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Section Container

/// Collapsible section for a record type within a tab.
struct RecordSection<Content: View>: View {
    let title: String
    let icon: String
    let count: Int
    @ViewBuilder let content: () -> Content

    var body: some View {
        if count > 0 {
            Section {
                content()
            } header: {
                HStack(spacing: 6) {
                    Image(systemName: icon)
                        .font(.caption)
                        .foregroundStyle(Theme.forestGreen)
                    Text("\(title) (\(count))")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                        .textCase(nil)
                }
            }
        }
    }
}

// MARK: - Empty State for Tab

struct TabEmptyStateView: View {
    let icon: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundStyle(Theme.forestGreen.opacity(0.4))
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
