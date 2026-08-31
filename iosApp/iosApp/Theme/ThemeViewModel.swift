import Foundation
import SwiftUI
import Shared
import UIKit

/// Root-level observer of the persisted theme preference so the whole SwiftUI
/// hierarchy honors the Appearance setting chosen in Settings.
///
/// The Kotlin preference store is a plain NSUserDefaults wrapper without a
/// reactive flow, so changes are detected via the global defaults-change
/// notification and re-read through the store on every fire.
@MainActor
final class ThemeViewModel: ObservableObject {
    @Published var preferredColorScheme: ColorScheme?
    @Published var accentColor: Color

    private let store: ThemePreferenceStore
    private var defaultsObserver: NSObjectProtocol?
    private var selectedAccent: AccentColor

    init() {
        store = IosThemePreferenceStoreKt.createPlatformThemePreferenceStore()
        preferredColorScheme = Self.colorScheme(for: store.getThemeMode())
        selectedAccent = store.getAccentColor()
        accentColor = Theme.color(for: selectedAccent)
        applyWindowAppearance()
        defaultsObserver = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            // Kotlin/Native may bridge the notification with a different
            // NSObject identity even though it writes the standard suite.
            // Filtering by object can therefore miss a real preference change.
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.reloadFromPreferences()
            }
        }
    }

    deinit {
        if let defaultsObserver {
            NotificationCenter.default.removeObserver(defaultsObserver)
        }
    }

    private static func colorScheme(for mode: ThemeMode) -> ColorScheme? {
        switch mode {
        case .light: return .light
        case .dark: return .dark
        case .system:
            // nil defers to presentation-time inheritance so iOS tracks live
            // system-appearance changes instead of a scheme pinned at init.
            return nil
        default: return nil
        }
    }

    /// Re-reads the Kotlin-backed preference store immediately after a
    /// settings edit. The notification observer remains as a safety net for
    /// changes made by another settings owner or by a restored preference.
    func reloadFromPreferences() {
        let scheme = Self.colorScheme(for: store.getThemeMode())
        if scheme != preferredColorScheme {
            preferredColorScheme = scheme
        }
        applyWindowAppearance()
        let accent = store.getAccentColor()
        if accent != selectedAccent {
            selectedAccent = accent
            accentColor = Theme.color(for: accent)
        }
    }

    /// Applies the preference to every app window, including an already
    /// presented Settings sheet. Unlike a SwiftUI presentation preference,
    /// `.unspecified` explicitly releases the previous Dark/Light override.
    private func applyWindowAppearance() {
        let style: UIUserInterfaceStyle
        switch preferredColorScheme {
        case .light: style = .light
        case .dark: style = .dark
        case nil: style = .unspecified
        @unknown default: style = .unspecified
        }

        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows where window.overrideUserInterfaceStyle != style {
                window.overrideUserInterfaceStyle = style
            }
        }
    }
}
