import XCTest
import UIKit

/// Settings/theme coverage through the real UI.
final class ThemeUITests: AnimallyTestCase {
    private func openSettings(_ app: XCUIApplication) {
        app.buttons["Settings"].firstMatch.tap()
        _ = app.navigationBars.firstMatch.waitForExistence(timeout: 8)
    }

    func testSettingsOpensFromPatients() throws {
        let app = TestHelpers.launchApp()
        openSettings(app)

        // Theme controls exist somewhere on the settings surface.
        let hasThemeControls =
            app.buttons["Light"].exists || app.staticTexts["Theme"].exists ||
            app.buttons["Dark"].exists || app.segmentedControls.firstMatch.exists
        XCTAssertTrue(hasThemeControls, "No theme controls found on Settings")
    }

    func testThemePickerHasThreeOptions() throws {
        let app = TestHelpers.launchApp()
        openSettings(app)

        for option in ["Light", "Dark", "System"] {
            let control = app.buttons[option].firstMatch
            let exists = control.exists || app.staticTexts[option].exists
            XCTAssertTrue(exists, "Theme option \(option) missing")
        }
    }

    func testAccentPaletteCanBeChanged() throws {
        let app = TestHelpers.launchApp()
        openSettings(app)

        let ocean = app.buttons["settings_accent_ocean"].firstMatch
        XCTAssertTrue(ocean.waitForExistence(timeout: 8), "Ocean accent is missing")
        ocean.tap()

        XCTAssertEqual(ocean.value as? String, "Selected")
    }

    func testDarkToSystemFollowsSimulatorAppearance() throws {
        let app = TestHelpers.launchApp()
        openSettings(app)

        let dark = app.buttons["Dark"].firstMatch
        XCTAssertTrue(dark.waitForExistence(timeout: 8), "Dark theme option is missing")
        dark.tap()
        Thread.sleep(forTimeInterval: 1)
        let darkBrightness = backgroundBrightness(XCUIScreen.main.screenshot())

        let system = app.buttons["System"].firstMatch
        XCTAssertTrue(system.waitForExistence(timeout: 8), "System theme option is missing")
        system.tap()
        Thread.sleep(forTimeInterval: 1)
        let picker = app.descendants(matching: .any)
            .matching(identifier: "settings_theme_picker")
            .firstMatch
        XCTAssertEqual(picker.value as? String, "System", "System selection did not persist in the settings control")
        let systemBrightness = backgroundBrightness(XCUIScreen.main.screenshot())

        XCTAssertLessThan(darkBrightness, 0.25, "Dark selection did not darken Settings")
        XCTAssertGreaterThan(systemBrightness, 0.75, "System selection did not restore the simulator's light appearance")
    }

    private func backgroundBrightness(_ screenshot: XCUIScreenshot) -> CGFloat {
        guard
            let image = screenshot.image.cgImage,
            let data = image.dataProvider?.data,
            let bytes = CFDataGetBytePtr(data)
        else {
            XCTFail("Could not inspect the Settings screenshot")
            return 0
        }

        let bytesPerPixel = image.bitsPerPixel / 8
        let x = max(0, min(image.width - 1, image.width / 50))
        let y = image.height / 2
        let offset = y * image.bytesPerRow + x * bytesPerPixel
        let red = CGFloat(bytes[offset]) / 255
        let green = CGFloat(bytes[offset + 1]) / 255
        let blue = CGFloat(bytes[offset + 2]) / 255
        return (red + green + blue) / 3
    }
}
