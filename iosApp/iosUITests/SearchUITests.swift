import XCTest

/// Global search coverage: exact, fuzzy/partial, and owner results.
final class SearchUITests: AnimallyTestCase {
    private func activateSearch(_ app: XCUIApplication) -> XCUIElement {
        app.tabBars.buttons["Search"].tap()

        // iOS 26 may expose the searchable field directly or inside the nav bar.
        let direct = app.searchFields.firstMatch
        if direct.waitForExistence(timeout: 4) {
            return direct
        }
        let navField = app.navigationBars.textFields.firstMatch
        XCTAssertTrue(navField.waitForExistence(timeout: 6), "Search field not found")
        return navField
    }

    func testExactPatientSearchShowsResult() throws {
        let app = TestHelpers.launchApp()
        let patientName = TestHelpers.firstPatientName(app)
        let field = activateSearch(app)

        TestHelpers.typeSearchText(app, field: field, text: patientName)

        let found = app.staticTexts[patientName].waitForExistence(timeout: 12)
        if !found {
            let texts = app.staticTexts.allElementsBoundByIndex.prefix(10).map { $0.label }
            XCTFail("Patient result missing; field=\(field.value ?? "nil"); texts=\(texts)")
        }
    }

    func testPartialFuzzySearchMatches() throws {
        let app = TestHelpers.launchApp()
        let patientName = TestHelpers.firstPatientName(app)
        let partial = String(patientName.prefix(min(4, patientName.count)))
        let field = activateSearch(app)

        TestHelpers.typeSearchText(app, field: field, text: partial.lowercased()) // lowercase partial — fuzzy matching must still hit

        XCTAssertTrue(app.staticTexts[patientName].waitForExistence(timeout: 12), "Fuzzy patient match missing")
    }

    func testOwnerSearchShowsOwnerSection() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Owners"].tap()
        let ownerName = TestHelpers.firstOwnerName(app)
        let field = activateSearch(app)

        TestHelpers.typeSearchText(app, field: field, text: ownerName)

        let ownerFound = app.staticTexts[ownerName].waitForExistence(timeout: 12)
        if !ownerFound {
            let texts = app.staticTexts.allElementsBoundByIndex.prefix(10).map { $0.label }
            XCTFail("Owner result missing; field=\(field.value ?? "nil"); texts=\(texts)")
        }
    }

    func testNoResultsShowsEmptyState() throws {
        let app = TestHelpers.launchApp()
        let field = activateSearch(app)

        TestHelpers.typeSearchText(app, field: field, text: "zzqqxxnothing")

        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] 'No'")).firstMatch.waitForExistence(timeout: 8))
    }
}
