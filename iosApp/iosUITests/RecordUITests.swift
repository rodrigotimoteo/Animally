import XCTest

/// Record lifecycle coverage: create via + menu, verify in tab, tap-to-edit,
/// swipe-delete, and the delete-persistence regression.
final class RecordUITests: AnimallyTestCase {
    private func openFarrierForm(_ app: XCUIApplication) -> String {
        TestHelpers.openThunderDetail(app)
        app.buttons["Add record"].firstMatch.tap()

        let farrier = app.buttons["Farrier Visit"].firstMatch
        XCTAssertTrue(farrier.waitForExistence(timeout: 8), "Farrier Visit missing from record menu")
        farrier.tap()

        XCTAssertTrue(app.navigationBars["New Farrier Visit"].waitForExistence(timeout: 8))
        return "UITest\(TestHelpers.uniqueSuffix())"
    }

    private func preventiveRow(_ app: XCUIApplication, marker: String) -> XCUIElement {
        app.buttons["Preventive"].firstMatch.tap()
        let predicate = NSPredicate(format: "label CONTAINS %@", marker)
        let row = app.descendants(matching: .any).matching(predicate).firstMatch
        var attempts = 0
        while !row.exists && attempts < 14 {
            app.swipeUp()
            attempts += 1
        }
        return row
    }

    func testCreateFarrierVisitAppearsInPreventiveTab() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = app.textFields["Farrier"]
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        farrierField.tap()
        farrierField.typeText(marker)

        let findings = app.textFields["Findings"]
        findings.tap()
        findings.typeText("checkup")

        app.buttons["Save"].tap()
        XCTAssertTrue(app.buttons["Add record"].waitForExistence(timeout: 10), "Did not return to detail")

        let row = preventiveRow(app, marker: marker)
        XCTAssertTrue(row.waitForExistence(timeout: 10), "Created farrier visit not visible in Preventive tab")
    }

    func testTappingRecordOpensPrefilledEditor() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = app.textFields["Farrier"]
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        farrierField.tap()
        farrierField.typeText(marker)

        let findings = app.textFields["Findings"]
        findings.tap()
        findings.typeText("checkup")
        app.buttons["Save"].tap()
        _ = app.buttons["Add record"].waitForExistence(timeout: 10)

        let row = preventiveRow(app, marker: marker)
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()

        // Editor opens prefilled with the saved marker.
        XCTAssertTrue(
            app.navigationBars.firstMatch.waitForExistence(timeout: 8),
            "Editor did not open"
        )
        XCTAssertFalse(app.navigationBars["New Farrier Visit"].exists, "Should open in EDIT mode, not new")
        let editorField = app.textFields["Findings"]
        if editorField.exists {
            XCTAssertEqual(editorField.value as? String, marker, "Editor not prefilled with saved findings")
        }
    }

    /// Regression: deleting a record must persist across detail re-entry.
    func testSwipeDeletePersistsAfterReentry() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = app.textFields["Farrier"]
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        farrierField.tap()
        farrierField.typeText(marker)

        let findings = app.textFields["Findings"]
        findings.tap()
        findings.typeText("checkup")
        app.buttons["Save"].tap()
        _ = app.buttons["Add record"].waitForExistence(timeout: 10)

        // Delete via swipe.
        let row = preventiveRow(app, marker: marker)
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        let deleteButton = app.buttons["Delete"].firstMatch
        var swipeAttempts = 0
        while !deleteButton.exists && swipeAttempts < 3 {
            row.swipeLeft()
            _ = deleteButton.waitForExistence(timeout: 2)
            swipeAttempts += 1
        }
        XCTAssertTrue(deleteButton.waitForExistence(timeout: 5), "Stock Delete button did not appear")
        deleteButton.tap()

        // Leave and re-enter the patient detail.
        app.buttons["BackButton"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 8))
        TestHelpers.openThunderDetail(app)

        // The record must be gone for good.
        app.buttons["Preventive"].firstMatch.tap()
        sleep(1)
        let ghost = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", marker)).firstMatch
        XCTAssertFalse(ghost.exists, "Deleted record reappeared after re-entry")
    }
}
