import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        IosAppBridge.shared.start()
        ExportShareInstaller.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .tint(Theme.forestGreen)
        }
    }
}
