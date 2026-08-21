import Foundation
import SwiftUI
import Shared

/// Root-level observer of the persisted theme preference so the whole SwiftUI
/// hierarchy honors the Appearance setting chosen in Settings.
///
/// The Kotlin preference store is a plain NSUserDefaults wrapper without a
/// reactive flow, so changes are detected via the global defaults-change
/// notification and re-read through the store on every fire.
@MainActor
final class ThemeViewModel: ObservableObject {
    @Published var preferredColorScheme: ColorScheme?

    private let store: ThemePreferenceStore
    private var defaultsObserver: NSObjectProtocol?

    init() {
        store = IosThemePreferenceStoreKt.createPlatformThemePreferenceStore()
        preferredColorScheme = Self.colorScheme(for: store.getThemeMode())
        defaultsObserver = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: UserDefaults.standard,
            queue: .main
        ) { [weak self] _ in
            guard let self else { return }
            let scheme = Self.colorScheme(for: self.store.getThemeMode())
            if scheme != self.preferredColorScheme {
                self.preferredColorScheme = scheme
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
        case .system: return nil
        default: return nil
        }
    }
}
