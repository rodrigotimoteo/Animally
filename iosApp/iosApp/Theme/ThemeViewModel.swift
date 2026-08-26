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
        defaultsObserver = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: UserDefaults.standard,
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

    private func reloadFromPreferences() {
        let scheme = Self.colorScheme(for: store.getThemeMode())
        if scheme != preferredColorScheme {
            preferredColorScheme = scheme
        }
        let accent = store.getAccentColor()
        if accent != selectedAccent {
            selectedAccent = accent
            accentColor = Theme.color(for: accent)
        }
    }
}
