import XCTest

/// Owner list coverage through the real UI.
final class OwnerUITests: AnimallyTestCase {
    func testOwnersTabListsSeedOwner() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Owners"].tap()

        XCTAssertTrue(app.navigationBars["Owners"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["Owner Daniela"].waitForExistence(timeout: 8))
    }

    func testOwnerRowOpensDetail() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Owners"].tap()
        XCTAssertTrue(app.buttons["Owner Daniela"].waitForExistence(timeout: 8))

        app.buttons["Owner Daniela"].firstMatch.tap()
        _ = app.navigationBars.firstMatch.waitForExistence(timeout: 8)
        // Detail must not crash and must leave the Owners tab stack.
        XCTAssertFalse(app.navigationBars["Owners"].exists || app.tabBars.buttons["Owners"].isSelected == false)
    }
}
