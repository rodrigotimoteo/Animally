import XCTest

/// Record lifecycle coverage: create via + menu, verify in tab, tap-to-edit,
/// swipe-delete, and the delete-persistence regression.
final class RecordUITests: AnimallyTestCase {
    private func openFarrierForm(_ app: XCUIApplication) -> String {
        _ = TestHelpers.openPatientDetail(app)
        app.buttons["Add record"].firstMatch.tap()

        let farrier = app.buttons["Farrier Visit"].firstMatch
        XCTAssertTrue(farrier.waitForExistence(timeout: 8), "Farrier Visit missing from record menu")
        farrier.tap()

        XCTAssertTrue(app.navigationBars["New Farrier Visit"].waitForExistence(timeout: 8))
        return "UITest\(TestHelpers.uniqueSuffix())"
    }

    private func preventiveRow(_ app: XCUIApplication, marker: String) -> XCUIElement {
        app.buttons["Preventive"].firstMatch.tap()

        // Record sections intentionally collapse after five rows. Expand the
        // section through its real UI before looking for a newly inserted
        // marker, so the helper remains valid with older simulator data.
        let showAll = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@ AND label CONTAINS[c] %@", "Show all", "Farrier Visits")
        ).firstMatch
        var expansionScrolls = 0
        while (!showAll.exists || !showAll.isHittable) && expansionScrolls < 14 {
            app.swipeUp()
            expansionScrolls += 1
        }
        if showAll.exists && showAll.isHittable {
            // SwiftUI exposes the Button with a full-width frame but its
            // visible label is a nested StaticText. Resolve its screen frame
            // and synthesize the touch from the application coordinate space;
            // this matches a physical tap and avoids the List row's leading
            // spacer being selected by XCTest's element-centre heuristic.
            let showAllLabel = app.staticTexts.matching(
                NSPredicate(format: "label BEGINSWITH[c] %@", "Show all")
            ).firstMatch
            let labelFrame = showAllLabel.exists && showAllLabel.isHittable ? showAllLabel.frame : showAll.frame
            let appFrame = app.frame
            let touch = app.coordinate(
                withNormalizedOffset: CGVector(
                    dx: (labelFrame.midX / appFrame.width),
                    dy: (labelFrame.midY / appFrame.height)
                )
            )
            touch.tap()
            // Expanded rows are lazy-loaded by SwiftUI List, so the “Show
            // fewer” control may be below the current accessibility viewport.
            // Verify the requested record while scrolling instead.
            sleep(1)
        }

        let predicate = NSPredicate(format: "label CONTAINS[c] %@", marker)
        let row = app.descendants(matching: .any).matching(predicate).firstMatch
        var attempts = 0
        while !row.exists && attempts < 20 {
            app.swipeUp()
            attempts += 1
        }
        if !row.exists {
            let visibleLabels = app.descendants(matching: .any).allElementsBoundByIndex
                .map(\.label)
                .filter {
                    $0.localizedCaseInsensitiveContains("farrier")
                        || $0.localizedCaseInsensitiveContains("uitest")
                }
            XCTFail("Record marker '\(marker)' was not rendered; visible record labels: \(visibleLabels)")
        }
        return row
    }

    func testCreateFarrierVisitAppearsInPreventiveTab() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = TestHelpers.formField(app, label: "Farrier")
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(farrierField, text: marker)

        let findings = TestHelpers.formField(app, label: "Findings")
        XCTAssertTrue(findings.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(findings, text: "checkup")

        app.buttons["Save"].tap()
        XCTAssertTrue(app.buttons["Add record"].waitForExistence(timeout: 10), "Did not return to detail")

        let row = preventiveRow(app, marker: marker)
        XCTAssertTrue(row.waitForExistence(timeout: 10), "Created farrier visit not visible in Preventive tab")
    }

    func testTappingRecordOpensPrefilledEditor() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = TestHelpers.formField(app, label: "Farrier")
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(farrierField, text: marker)

        let findings = TestHelpers.formField(app, label: "Findings")
        XCTAssertTrue(findings.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(findings, text: "checkup")
        app.buttons["Save"].tap()
        _ = app.buttons["Add record"].waitForExistence(timeout: 10)

        let row = preventiveRow(app, marker: marker)
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()

        // The row opens the read-only record detail; editing is an explicit
        // second action so the detail can also be used for inspection.
        let editButton = app.buttons["Edit"].firstMatch
        XCTAssertTrue(editButton.waitForExistence(timeout: 8), "Record detail did not expose Edit")
        editButton.tap()
        XCTAssertTrue(
            app.navigationBars["Edit Farrier Visit"].waitForExistence(timeout: 8),
            "Editor did not open"
        )
        let editorField = TestHelpers.formField(app, label: "Farrier")
        XCTAssertTrue(editorField.waitForExistence(timeout: 5), "Farrier field missing from editor")
        XCTAssertEqual(editorField.value as? String, marker, "Editor not prefilled with saved farrier")
    }

    /// Regression: deleting a record must persist across detail re-entry.
    func testSwipeDeletePersistsAfterReentry() throws {
        let app = TestHelpers.launchApp()
        let marker = openFarrierForm(app)

        let farrierField = TestHelpers.formField(app, label: "Farrier")
        XCTAssertTrue(farrierField.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(farrierField, text: marker)

        let findings = TestHelpers.formField(app, label: "Findings")
        XCTAssertTrue(findings.waitForExistence(timeout: 5))
        TestHelpers.typeTextAndVerify(findings, text: "checkup")
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

        // The shared swipe action deliberately opens the same confirmation
        // dialog as patient/owner deletion. Tap the destructive confirmation,
        // not the red swipe affordance that opened it.
        let sheetConfirmation = app.sheets.buttons["Delete"].firstMatch
        if sheetConfirmation.waitForExistence(timeout: 5) {
            sheetConfirmation.tap()
        } else {
            let alertConfirmation = app.alerts.buttons["Delete"].firstMatch
            if alertConfirmation.waitForExistence(timeout: 2) {
                alertConfirmation.tap()
            } else {
                let deleteButtons = app.buttons.matching(
                    NSPredicate(format: "label == %@", "Delete")
                )
                let secondDelete = deleteButtons.element(boundBy: 1)
                XCTAssertTrue(
                    secondDelete.waitForExistence(timeout: 3),
                    "Delete confirmation did not appear"
                )
                secondDelete.tap()
            }
        }

        // Leave and re-enter the patient detail.
        app.buttons["BackButton"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 8))
        _ = TestHelpers.openPatientDetail(app)

        // The record must be gone for good.
        app.buttons["Preventive"].firstMatch.tap()
        sleep(1)
        let ghost = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", marker)).firstMatch
        XCTAssertFalse(ghost.exists, "Deleted record reappeared after re-entry")
    }
}
