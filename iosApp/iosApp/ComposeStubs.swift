import Foundation
import ObjectiveC

// Stub for Compose Multiplatform symbol that's referenced by Shared framework
// but not needed for native SwiftUI consumption.
@objc(UIViewLayoutRegion)
public class UIViewLayoutRegionStub: NSObject {
    @objc public override init() {
        super.init()
    }
}
