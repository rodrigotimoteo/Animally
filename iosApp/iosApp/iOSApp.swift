import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        IosAppBridge.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .tint(Theme.forestGreen)
        }
    }
}
