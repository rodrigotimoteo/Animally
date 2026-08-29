import XCTest

/// Owner list coverage through the real UI.
final class OwnerUITests: AnimallyTestCase {
    func testOwnersTabListsSeedOwner() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Owners"].tap()

        XCTAssertTrue(app.navigationBars["Owners"].waitForExistence(timeout: 8))
        _ = TestHelpers.firstOwnerRow(app)
    }

    func testOwnerRowOpensDetail() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Owners"].tap()
        let ownerRow = TestHelpers.firstOwnerRow(app)

        ownerRow.tap()
        _ = app.navigationBars.firstMatch.waitForExistence(timeout: 8)
        // Detail must not crash and must leave the Owners tab stack.
        XCTAssertFalse(app.navigationBars["Owners"].exists || app.tabBars.buttons["Owners"].isSelected == false)
    }
}
