import XCTest

/// Utility: opens a New Ultrasound form and pauses so screenshots can be taken.
final class FolliclePauseUITests: AnimallyTestCase {
    func testPauseOnUltrasoundForm() throws {
        let app = XCUIApplication()
        app.launch()
        _ = app.staticTexts["Patients"].waitForExistence(timeout: 10)
        _ = TestHelpers.openPatientDetail(app)
        _ = app.buttons["Add record"].firstMatch.waitForExistence(timeout: 8)
        app.buttons["Add record"].firstMatch.tap()
        // SwiftUI Menu exposes the item label reliably, while custom
        // accessibility identifiers are not consistently surfaced for menu
        // actions on the simulator.
        let us = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS[c] %@", "Ultrasound")
        ).firstMatch
        let menu = app.collectionViews.firstMatch
        var scrollAttempts = 0
        while (!us.exists || !us.isHittable) && scrollAttempts < 4 {
            if menu.exists && menu.isHittable {
                menu.swipeUp()
            } else {
                app.swipeUp()
            }
            usleep(300_000)
            scrollAttempts += 1
        }
        XCTAssertTrue(
            us.waitForExistence(timeout: 3),
            "Ultrasound item is unavailable after scrolling the add-record menu"
        )
        XCTAssertTrue(us.isHittable, "Ultrasound item is not hittable in the add-record menu")
        us.tap()
        XCTAssertTrue(app.navigationBars["New Ultrasound"].waitForExistence(timeout: 10))
        // Scroll to the ovary sections.
        app.swipeUp()
        sleep(75)
    }
}
