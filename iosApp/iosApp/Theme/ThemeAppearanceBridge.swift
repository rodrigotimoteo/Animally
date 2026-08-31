import SwiftUI
import UIKit

/// Applies the app's appearance choice to the containing UIKit window.
///
/// SwiftUI's `preferredColorScheme(nil)` is a presentation preference and can
/// remain sticky on an already-presented sheet after switching from Dark to
/// System. Updating the window's override style directly keeps the existing
/// hierarchy alive and lets UIKit resolve dynamic system colors correctly.
/// This is deliberately a native UI bridge; theme persistence and selection
/// remain owned by the shared Kotlin settings layer.
struct ThemeAppearanceBridge: UIViewControllerRepresentable {
    let colorScheme: ColorScheme?

    func makeUIViewController(context: Context) -> Controller {
        Controller(colorScheme: colorScheme)
    }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.colorScheme = colorScheme
        controller.applyIfAttached()
    }

    final class Controller: UIViewController {
        var colorScheme: ColorScheme?

        init(colorScheme: ColorScheme?) {
            self.colorScheme = colorScheme
            super.init(nibName: nil, bundle: nil)
            view.isHidden = true
            view.isUserInteractionEnabled = false
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            applyIfAttached()
        }

        func applyIfAttached() {
            guard let window = viewIfLoaded?.window else { return }
            let style: UIUserInterfaceStyle
            switch colorScheme {
            case .light: style = .light
            case .dark: style = .dark
            case nil: style = .unspecified
            @unknown default: style = .unspecified
            }
            if window.overrideUserInterfaceStyle != style {
                window.overrideUserInterfaceStyle = style
            }
        }
    }
}
