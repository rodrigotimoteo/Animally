import SwiftUI
import Shared

// TodayProvider now lives in DateFormatters.swift — single conversion primitive
// View helper accessibilityIdentifierIfPresent removed — single use inlined at call site

// MARK: - InsightCardContainer — single container primitive

struct InsightCardContainer<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) { self.content = content() }
    var body: some View {
        content
            .padding(14)
            .background(Theme.surfaceElevated)
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - InsightSectionHeader — replaces repeated HStack(icon+title+spacer)

struct InsightSectionHeader: View {
    let systemImage: String
    let title: String
    var countText: String? = nil
    var tint: Color = Theme.forestGreen
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage).foregroundStyle(tint).accessibilityHidden(true)
            Text(title).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textPrimary)
            Spacer()
            if let c = countText {
                Text(c).font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - InsightStatCard — single gate (isInteractive) replaces double-gate

struct InsightStatCard: View {
    let title: String
    let value: String
    let subtitle: String
    var isAvailable: Bool = true
    var action: (() -> Void)? = nil

    private var isInteractive: Bool { isAvailable && action != nil }

    var body: some View {
        Group {
            if isInteractive, let act = action {
                Button(action: act) { cardContent }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isButton)
            } else {
                cardContent
                    .accessibilityElement(children: .combine)
            }
        }
        .accessibilityLabel("\(title), \(value), \(subtitle)")
        .accessibilityHint(isAvailable ? "" : "Unavailable")
    }

    private var cardContent: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.caption.weight(.semibold)).foregroundStyle(Theme.textSecondary).lineLimit(2).fixedSize(horizontal: false, vertical: true)
            Text(value).font(.title3.weight(.bold)).foregroundStyle(isAvailable ? Theme.textPrimary : Theme.textSecondary).lineLimit(1).minimumScaleFactor(0.6)
            Text(subtitle).font(.caption2).foregroundStyle(Theme.textSecondary).lineLimit(2).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        .contentShape(Rectangle())
    }
}

// MARK: - InsightPillButton — single Button body, no duplicate branches

struct InsightPillButton: View {
    let title: String
    let isSelected: Bool
    var identifier: String? = nil
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .frame(minHeight: 36)
                .foregroundStyle(isSelected ? Color.white : Theme.textSecondary)
                .background(isSelected ? Theme.forestGreen : Color(.systemBackground))
                .clipShape(Capsule())
                .shadow(color: .black.opacity(isSelected ? 0.08 : 0.04), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .optionalAccessibilityIdentifier(identifier)
    }
}

private extension View {
    @ViewBuilder func optionalAccessibilityIdentifier(_ id: String?) -> some View {
        if let id { self.accessibilityIdentifier(id) } else { self }
    }
}

// RecordBadgeIcon moved to RecordComponents.swift — single location, used by Search/Timeline/RecordRowView

// MARK: - InlineErrorBanner — single init, systemImage derived unless explicit

struct InlineErrorBanner: View {
    let message: String
    var tint: Color = Theme.amber
    var systemImage: String
    var onRetry: (() -> Void)? = nil
    var onDismiss: (() -> Void)? = nil

    init(message: String, tint: Color = Theme.amber, systemImage: String? = nil, onRetry: (() -> Void)? = nil, onDismiss: (() -> Void)? = nil) {
        self.message = message
        self.tint = tint
        self.systemImage = systemImage ?? (onRetry != nil ? "exclamationmark.triangle.fill" : "info.circle.fill")
        self.onRetry = onRetry
        self.onDismiss = onDismiss
    }
    // Legacy shim — now correctly forwards systemImage instead of ignoring it
    init(systemImage: String, message: String, tint: Color = Theme.amber, showsRetry: Bool = false, onRetry: (() -> Void)? = nil, onDismiss: (() -> Void)? = nil) {
        self.init(message: message, tint: tint, systemImage: systemImage, onRetry: showsRetry ? onRetry : nil, onDismiss: onDismiss)
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage).foregroundStyle(tint).accessibilityHidden(true)
            Text(message).font(.subheadline).foregroundStyle(Theme.textPrimary).lineLimit(2)
            Spacer()
            if let retry = onRetry {
                Button("Retry", action: retry).font(.caption.weight(.bold)).foregroundStyle(Theme.forestGreen).accessibilityLabel("Retry").accessibilityHint("Retries loading insights")
            }
            if let dismiss = onDismiss {
                Button(action: dismiss) {
                    Image(systemName: "xmark").font(.caption.weight(.bold)).foregroundStyle(Theme.textSecondary)
                }.accessibilityLabel("Dismiss error").accessibilityHint("Dismisses banner")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(message)
    }
}
