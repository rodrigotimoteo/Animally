import XCTest

final class TreeDumpUITests: XCTestCase {
    func testDumpCreateThenSearch() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 15))

        // Create a patient exactly like the failing test does.
        let name = "UITest\(String(UUID().uuidString.prefix(6)))"
        app.buttons["Add patient"].firstMatch.tap()
        let nameField = app.textFields["Name"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText(name)
        app.buttons["Save"].tap()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 8))

        // Search for it in the same session.
        app.tabBars.buttons["Search"].tap()
        let direct = app.searchFields.firstMatch
        let field = direct.waitForExistence(timeout: 4) ? direct : app.navigationBars.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 6))
        field.tap()
        field.typeText(name)
        sleep(5)

        print("===== DUMP: CreateThenSearch =====")
        print("NAME: \(name)")
        print("FIELD VALUE: \(field.value ?? "nil")")
        print(app.debugDescription)
    }
}
