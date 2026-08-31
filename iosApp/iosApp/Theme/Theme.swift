import SwiftUI
import Shared

private struct AnimallySystemColorSchemeKey: EnvironmentKey {
    static let defaultValue: ColorScheme = .light
}

extension EnvironmentValues {
    var animallySystemColorScheme: ColorScheme {
        get { self[AnimallySystemColorSchemeKey.self] }
        set { self[AnimallySystemColorSchemeKey.self] = newValue }
    }
}

enum Theme {
    static let forestGreen = Color.accentColor
    static let amber = Color(red: 0.85, green: 0.65, blue: 0.13)
    static let surfaceElevated = Color(.secondarySystemBackground)
    static let textPrimary = Color(.label)
    static let textSecondary = Color(.secondaryLabel)
    static let textTertiary = Color(.tertiaryLabel)

    // Fixed palette — 8 semantic colors, no generative hue loop (fixes chartPalette magic).
    static let chartPalette: [Color] = [
        Color(red: 0.11, green: 0.35, blue: 0.29),
        Color(red: 0.09, green: 0.39, blue: 0.65),
        Color(red: 0.61, green: 0.30, blue: 0.20),
        Color(red: 0.48, green: 0.24, blue: 0.45),
        Color(red: 0.85, green: 0.65, blue: 0.13),
        Color(red: 0.26, green: 0.35, blue: 0.47),
        Color(red: 0.20, green: 0.55, blue: 0.45),
        Color(red: 0.75, green: 0.45, blue: 0.20),
    ]

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
