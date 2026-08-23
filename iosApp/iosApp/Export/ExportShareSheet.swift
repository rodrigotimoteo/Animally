import Shared
import UIKit

/// Presents the system share sheet for artifacts exported by the shared module
/// (CSV exports, JSON backups, PDF reports).
enum ExportShareInstaller {
    /// Installs the Kotlin→Swift share hook. Call once at app startup.
    ///
    /// The shared code may invoke the hook from a background dispatcher, so the
    /// presentation is always re-dispatched onto the main queue.
    static func install() {
        ExportShareHook.shared.onShareRequest = { path in
            DispatchQueue.main.async {
                presentShareSheet(forPath: path)
            }
        }
    }

    private static func presentShareSheet(forPath path: String) {
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first,
              let rootViewController = scene.windows.first(where: \.isKeyWindow)?.rootViewController
        else { return }

        var topViewController = rootViewController
        while let presented = topViewController.presentedViewController {
            topViewController = presented
        }

        let controller = UIActivityViewController(activityItems: [URL(fileURLWithPath: path)], applicationActivities: nil)
        controller.popoverPresentationController?.sourceView = topViewController.view
        topViewController.present(controller, animated: true)
    }
}
