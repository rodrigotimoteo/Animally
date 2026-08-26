import SwiftUI
import Shared

enum Theme {
    /// Semantic brand color. SwiftUI resolves `accentColor` from the nearest
    /// `.tint`, so existing screens automatically follow the selected accent.
    static let forestGreen = Color.accentColor
    static let amber = Color(red: 0.85, green: 0.65, blue: 0.13)
    static let surfaceElevated = Color(.secondarySystemBackground)
    static let textPrimary = Color(.label)
    static let textSecondary = Color(.secondaryLabel)
    static let textTertiary = Color(.tertiaryLabel)

    /// Concrete colors used by the accent picker and the root tint.
    static func color(for accent: AccentColor) -> Color {
        switch accent {
        case .forest:
            return Color(red: 0.11, green: 0.35, blue: 0.29)
        case .ocean:
            return Color(red: 0.09, green: 0.39, blue: 0.65)
        case .plum:
            return Color(red: 0.48, green: 0.24, blue: 0.45)
        case .terracotta:
            return Color(red: 0.61, green: 0.30, blue: 0.20)
        case .slate:
            return Color(red: 0.26, green: 0.35, blue: 0.47)
        default:
            return Color(red: 0.11, green: 0.35, blue: 0.29)
        }
    }
}
