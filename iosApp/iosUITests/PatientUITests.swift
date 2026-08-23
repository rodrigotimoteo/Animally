import XCTest

/// Patient CRUD coverage through the real UI.
final class PatientUITests: AnimallyTestCase {
    private var patientName: String!

    override func setUpWithError() throws {
        try super.setUpWithError()
        patientName = "UITest\(TestHelpers.uniqueSuffix())"
    }

    func testCreatePatientPersistsAndIsSearchable() throws {
        let app = TestHelpers.launchApp()

        app.buttons["Add patient"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["New Patient"].waitForExistence(timeout: 8))

        let nameField = app.textFields["Name"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText(patientName)

        app.buttons["Save"].tap()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 8))

        // Verify persistence through global search — deterministic regardless
        // of list position.
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch.waitForExistence(timeout: 4)
            ? app.searchFields.firstMatch
            : app.navigationBars.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 6), "Search field not found")
        TestHelpers.typeSearchText(app, field: field, text: patientName)

        XCTAssertTrue(
            app.staticTexts[patientName].waitForExistence(timeout: 20),
            "Created patient not found via search"
        )
    }

    func testOpenPatientDetailShowsOverview() throws {
        let app = TestHelpers.launchApp()
        TestHelpers.openThunderDetail(app)

        XCTAssertTrue(app.staticTexts["Basic Information"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.staticTexts["Breed"].exists)
        XCTAssertTrue(app.staticTexts["Gender"].exists)

        // All five detail tabs are present.
        for tab in ["Overview", "Medical", "Preventive", "Reproduction", "Diagnostics & Files"] {
            XCTAssertTrue(app.buttons[tab].exists, "Detail tab \(tab) missing")
        }
    }

    func testDetailTabsSwitchContent() throws {
        let app = TestHelpers.launchApp()
        TestHelpers.openThunderDetail(app)

        app.buttons["Medical"].firstMatch.tap()
        sleep(1)
        XCTAssertTrue(app.buttons["Medical"].isSelected, "Medical tab should be selected")

        app.buttons["Overview"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["Basic Information"].waitForExistence(timeout: 8))
    }

    func testEditPatientOpensPrefilledForm() throws {
        let app = TestHelpers.launchApp()
        TestHelpers.openThunderDetail(app)

        app.buttons["Edit patient"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars.firstMatch.waitForExistence(timeout: 8))
        // The form should carry the existing name.
        XCTAssertTrue(app.textFields["Thunder"].firstMatch.waitForExistence(timeout: 8))
    }
}
