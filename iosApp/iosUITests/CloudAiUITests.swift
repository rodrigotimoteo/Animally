import XCTest

/// Cloud AI end-to-end coverage through the real UI:
/// model discovery (GET {baseUrl}/models), the fetched-models picker,
/// and the assistant availability gate when Foundation Models is
/// unavailable but cloud is configured.
///
/// Requires network access from the simulator (OpenCode Zen /models is
/// public and authless, so discovery works with any dummy key).
final class CloudAiUITests: AnimallyTestCase {
    @discardableResult
    private func openSettings(_ app: XCUIApplication) -> XCUIElement {
        let gear = app.buttons["Settings"].firstMatch
        XCTAssertTrue(gear.waitForExistence(timeout: 10), "Settings gear not found")
        gear.tap()
        XCTAssertTrue(app.staticTexts["CLOUD AI"].waitForExistence(timeout: 10)
            || app.staticTexts["Cloud AI"].waitForExistence(timeout: 3),
            "Cloud AI section missing in Settings")
        return app
    }

    /// Ensures the Cloud AI toggle is on without assuming prior state.
    private func ensureCloudEnabled(_ app: XCUIApplication) {
        let toggle = app.switches["Cloud AI"].firstMatch
        if toggle.exists, toggle.value as? String == "0" {
            toggle.tap()
        }
    }

    private func openAssistant(_ app: XCUIApplication) {
        let assistantTab = app.tabBars.buttons["Assistant"]
        if assistantTab.exists {
            assistantTab.tap()
        } else {
            app.tabBars.buttons["More"].tap()
            let row = app.buttons["Assistant"].firstMatch
            XCTAssertTrue(row.waitForExistence(timeout: 5))
            row.tap()
        }
    }

    func testFetchModelsPopulatesPickerAndSelectionWorks() throws {
        try runFetchPickAskFlow(forceFmUnavailable: false)
    }

    /// Forces the on-device engine to report unavailable so the routing engine
    /// must serve the turn from the cloud model - asserted via the cloud badge.
    func testCloudServesAnswerWhenFmUnavailable() throws {
        try runFetchPickAskFlow(forceFmUnavailable: true)
    }

    /// Full Cloud AI flow: discovery -> picker selection -> assistant gate ->
    /// question -> answer. When `forceFmUnavailable` is set the answer must be
    /// badged as cloud-served; otherwise whichever engine the host provides wins.
    private func runFetchPickAskFlow(forceFmUnavailable: Bool) throws {
        let app = XCUIApplication()
        if forceFmUnavailable {
            app.launchArguments += ["-forceFmUnavailable"]
        }
        app.launch()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 15))
        openSettings(app)
        ensureCloudEnabled(app)

        // Dummy key: Zen /models needs no valid auth; proves the key round-trips.
        let keyField = app.secureTextFields["API Key"].firstMatch
        if keyField.waitForExistence(timeout: 5) {
            keyField.tap()
            keyField.typeText("uitest-dummy-key")
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
        }

        let fetch = app.buttons["Fetch models"].firstMatch
        XCTAssertTrue(fetch.waitForExistence(timeout: 5))
        fetch.tap()

        // Success criterion: the Model TextField is replaced by the picker row.
        let pickerRow = app.buttons["settings_cloud_model"].firstMatch
        let statusQuery = app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS[cd] 'fetch'")
        )
        XCTAssertTrue(pickerRow.waitForExistence(timeout: 60),
            "Picker row never appeared; status='\(statusQuery.allElementsBoundByIndex.first?.label ?? "none")'")

        pickerRow.tap()

        // Sheet must show real rows, not the empty state.
        let emptyState = app.staticTexts["No models fetched"].firstMatch
        XCTAssertFalse(emptyState.waitForExistence(timeout: 3), "Picker sheet showed empty state")

        // The identifier can land on the Button or its List cell depending on
        // the rendered hierarchy - match any element type.
        let modelRow = app.descendants(matching: .any)
            .matching(identifier: "cloud_model_row").firstMatch
        if !modelRow.waitForExistence(timeout: 5) {
            print("CLOUDAI_DEBUG sheet tree:\n\(app.debugDescription)")
        }
        XCTAssertTrue(modelRow.exists, "No model rows rendered in picker sheet")
        let chosen = modelRow.label
        modelRow.tap()

        // Selection lands back in the (now picker-style) field.
        let selected = app.buttons["settings_cloud_model"].firstMatch
        XCTAssertTrue(selected.waitForExistence(timeout: 5))
        XCTAssertEqual(selected.label, chosen)

        // The gate: with cloud enabled+keyed and FM unavailable on sim,
        // the assistant must be fully usable - no dead-end warning screen.
        // Close Settings first so the tab switch starts from a clean sheet-free state.
        let done = app.buttons["Done"].firstMatch
        if done.exists { done.tap() }
        openAssistant(app)
        let unavailable = app.staticTexts["On-device AI not available here"]
        XCTAssertFalse(unavailable.waitForExistence(timeout: 5),
            "Availability gate dead-ends although cloud is configured")
        let input = app.textFields["assistant_input"].firstMatch
        if !input.waitForExistence(timeout: 10) {
            print("CLOUDAI_DEBUG assistant tree:\n\(app.debugDescription)")
        }
        XCTAssertTrue(input.exists, "Chat input missing although cloud engine is ready")

        // End-to-end: FM is unavailable on the simulator, so the routing engine
        // must serve this turn from the cloud model and badge it.
        TestHelpers.typeSearchText(app, field: input, text: "How many patients do I have?")
        let send = app.buttons["assistant_send"].firstMatch
        XCTAssertTrue(send.waitForExistence(timeout: 5))
        send.tap()

        let reply = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Assistant:")
        ).firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 120), "Cloud answer never appeared")
        XCTAssertGreaterThan(reply.label.count, 10, "Cloud reply suspiciously short")

        // On hosts where the simulator proxies the Mac's Foundation Model the
        // routing engine legitimately serves from the primary (no badge). With
        // -forceFmUnavailable the cloud engine MUST serve and badge the answer.
        let badgeAny = app.descendants(matching: .any)
            .matching(identifier: "assistant_cloud_badge").firstMatch
        if forceFmUnavailable {
            XCTAssertTrue(badgeAny.waitForExistence(timeout: 10),
                "Cloud badge missing although FM was forced unavailable")
        } else if badgeAny.exists {
            print("CLOUDAI_DEBUG: answer served by CLOUD engine (badged)")
        } else {
            print("CLOUDAI_DEBUG: answer served by on-device FM (host proxy active)")
        }
    }

    func testFetchFailureSurfacesErrorStatus() throws {
        let app = TestHelpers.launchApp()
        openSettings(app)
        ensureCloudEnabled(app)

        // Point the endpoint at an unreachable host via the Advanced field.
        app.staticTexts["Advanced"].firstMatch.tap()
        let urlField = app.textFields["settings_cloud_base_url"].firstMatch
        XCTAssertTrue(urlField.waitForExistence(timeout: 5))
        // Healing typer: plain typeText drops keystrokes on live-binding fields,
        // which would leave a mangled URL persisted for subsequent runs.
        TestHelpers.typeSearchText(app, field: urlField, text: "https://127.0.0.1:9/v1")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()

        app.buttons["Fetch models"].firstMatch.tap()

        let failure = app.staticTexts.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Could not fetch models")
        ).firstMatch
        XCTAssertTrue(failure.waitForExistence(timeout: 60),
            "Network failure did not surface an error status")

        // Restore the working endpoint for subsequent runs/tests.
        TestHelpers.typeSearchText(app, field: urlField, text: "https://opencode.ai/zen/v1")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
    }
}
