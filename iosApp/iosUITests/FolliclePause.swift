import XCTest

/// Utility: opens a New Ultrasound form and pauses so screenshots can be taken.
final class FolliclePauseUITests: XCTestCase {
    func testPauseOnUltrasoundForm() throws {
        let app = XCUIApplication()
        app.launch()
        _ = app.staticTexts["Patients"].waitForExistence(timeout: 10)
        _ = TestHelpers.openPatientDetail(app)
        _ = app.buttons["Add record"].firstMatch.waitForExistence(timeout: 8)
        app.buttons["Add record"].firstMatch.tap()
        let us = app.buttons["Ultrasound"].firstMatch
        _ = us.waitForExistence(timeout: 6)
        us.tap()
        _ = app.navigationBars["New Ultrasound"].waitForExistence(timeout: 10)
        // Scroll to the ovary sections.
        app.swipeUp()
        sleep(75)
    }
}
