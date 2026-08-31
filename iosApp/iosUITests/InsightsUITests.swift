import XCTest

/// Simulator coverage for the iOS dashboard shell and shared-state interactions.
final class InsightsUITests: AnimallyTestCase {
    func testGlobalDashboardLoadsAndSwitchesPresets() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Timeline"].tap()

        let entry = app.buttons["timeline_insights_button"]
        XCTAssertTrue(entry.waitForExistence(timeout: 8), "Global Insights entry is missing")
        entry.tap()

        XCTAssertTrue(app.navigationBars["Insights"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.scrollViews["insights_dashboard"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.otherElements["insights_overview_section"].waitForExistence(timeout: 10))

        let ninetyDays = app.buttons["insights_preset_90_days"]
        XCTAssertTrue(ninetyDays.waitForExistence(timeout: 5))
        ninetyDays.tap()
        XCTAssertTrue(ninetyDays.isSelected)

        let allTime = app.buttons["insights_preset_all_time"]
        allTime.tap()
        XCTAssertTrue(allTime.isSelected)
        XCTAssertTrue(app.staticTexts["All time"].waitForExistence(timeout: 8))
    }

    func testDashboardExposesReproductionAndGestationSections() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Timeline"].tap()
        app.buttons["timeline_insights_button"].tap()
        XCTAssertTrue(app.navigationBars["Insights"].waitForExistence(timeout: 10))

        let dashboard = app.scrollViews["insights_dashboard"]
        XCTAssertTrue(dashboard.waitForExistence(timeout: 10))

        let reproduction = app.otherElements["insights_reproduction_section"]
        for _ in 0..<6 where !reproduction.exists {
            dashboard.swipeUp()
        }
        XCTAssertTrue(reproduction.exists, "Reproduction facts section is missing")

        let gestation = app.otherElements["insights_gestation_section"]
        for _ in 0..<5 where !gestation.exists {
            dashboard.swipeUp()
        }
        XCTAssertTrue(gestation.exists, "Current gestation section is missing")
    }

    func testOverviewOpensAuditableSourceRecords() throws {
        let app = TestHelpers.launchApp()
        app.tabBars.buttons["Timeline"].tap()
        app.buttons["timeline_insights_button"].tap()
        XCTAssertTrue(app.navigationBars["Insights"].waitForExistence(timeout: 10))

        let sourceRecords = app.buttons["View source records"]
        XCTAssertTrue(sourceRecords.waitForExistence(timeout: 10))
        sourceRecords.tap()

        XCTAssertTrue(app.navigationBars["Records"].waitForExistence(timeout: 10))
        let records = app.descendants(matching: .any)["insights_records_list"]
        let empty = app.descendants(matching: .any)["insights_records_empty"]
        let loaded = records.waitForExistence(timeout: 10) || empty.waitForExistence(timeout: 2)
        XCTAssertTrue(loaded, "Source-record drill-down never reached a terminal state")
    }

    func testPatientDashboardPreservesThePatientName() throws {
        let app = TestHelpers.launchApp()
        let patientName = TestHelpers.openPatientDetail(app)

        let insights = app.buttons["patient_detail_insights_button"]
        XCTAssertTrue(insights.waitForExistence(timeout: 10))
        insights.tap()

        XCTAssertTrue(app.navigationBars["Insights"].waitForExistence(timeout: 10))
        XCTAssertTrue(
            app.staticTexts["Scope: \(patientName)"].waitForExistence(timeout: 10),
            "Patient-scoped dashboard replaced the patient's name with an internal identifier"
        )
    }
}
