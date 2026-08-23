import XCTest

/// Verifies every root tab is reachable and reports itself selected.
final class NavigationUITests: AnimallyTestCase {
    func testAllFiveTabsAreReachable() throws {
        let app = TestHelpers.launchApp()
        let tabBar = app.tabBars.firstMatch

        let expectations: [(tab: String, emptyState: String)] = [
            ("Patients", "Patients"),
            ("Owners", "Owners"),
            ("Timeline", "Timeline"),
            ("Search", "Search"),
            ("Assistant", "Assistant"),
        ]

        for (tab, navTitle) in expectations {
            tabBar.buttons[tab].tap()
            XCTAssertTrue(
                app.navigationBars[navTitle].waitForExistence(timeout: 8),
                "Navigation bar for \(tab) did not appear"
            )
            XCTAssertTrue(
                tabBar.buttons[tab].isSelected,
                "\(tab) tab should report selected"
            )
        }
    }

    func testAssistantShowsEmptyState() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Assistant"].tap()
        XCTAssertTrue(app.staticTexts["Ask about your patients"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["Send message"].exists)
    }
}
