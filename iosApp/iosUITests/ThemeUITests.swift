import XCTest

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
}
