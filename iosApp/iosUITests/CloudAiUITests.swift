import XCTest

/// Cloud AI end-to-end coverage through the real UI:
/// model discovery (GET {baseUrl}/models), the fetched-models picker,
/// and the assistant availability gate when Foundation Models is
/// unavailable but cloud is configured.
///
/// Requires network access from the simulator. Normal runs use a deterministic
/// dummy key for the public `/models` smoke path. Set `ANIMALLY_LIVE_CLOUD=1`
/// to preserve the configured credential and exercise real provider requests.
final class CloudAiUITests: AnimallyTestCase {
    private var isLiveCloudRun: Bool {
        ProcessInfo.processInfo.environment["ANIMALLY_LIVE_CLOUD"] == "1"
    }

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

    /// Enters the paid Mimo model id before model discovery turns the manual
    /// field into a picker. This is useful when a provider's `/models` response
    /// advertises only its free tier even though the paid id is routable.
    private func configureLivePaidMimoModelManually(_ app: XCUIApplication) throws {
        openSettings(app)
        ensureCloudEnabled(app)

        let provider = app.buttons["settings_cloud_provider"].firstMatch
        XCTAssertTrue(provider.waitForExistence(timeout: 5), "Cloud provider picker is unavailable")
        provider.tap()
        let openCodeGo = app.buttons["OpenCode Go"].firstMatch
        XCTAssertTrue(openCodeGo.waitForExistence(timeout: 5), "OpenCode Go provider option is unavailable")
        openCodeGo.tap()

        let modelField = app.textFields["settings_cloud_model"].firstMatch
        XCTAssertTrue(
            modelField.waitForExistence(timeout: 5),
            "The model field was not editable before discovery; cannot test the paid id directly",
        )
        TestHelpers.typeTextAndVerify(modelField, text: "mimo-v2.5")

        let selected = app.textFields["settings_cloud_model"].firstMatch
        XCTAssertTrue(selected.waitForExistence(timeout: 5), "Selected model field is unavailable")
        XCTAssertTrue(
            ((selected.value as? String) ?? selected.label).lowercased().hasPrefix("mimo-v2.5"),
            "Selected model was not a Mimo 2.5 variant: \(selected.value ?? selected.label)",
        )

        let done = app.buttons["Done"].firstMatch
        if done.exists { done.tap() }
    }

    func testFetchModelsPopulatesPickerAndSelectionWorks() throws {
        try runFetchPickAskFlow(forceFmUnavailable: false)
    }

    /// Forces the on-device engine to report unavailable so the routing engine
    /// must serve the turn from the cloud model - asserted via the cloud badge.
    func testCloudServesAnswerWhenFmUnavailable() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live cloud test; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
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

        // Normal tests use a deterministic dummy key for the public /models
        // smoke path. Live coverage opts in through the environment and keeps
        // the key already configured in Settings.
        if !isLiveCloudRun || app.launchArguments.contains("-animally-ui-test-dummy-cloud-key") {
            let keyField = app.secureTextFields["API Key"].firstMatch
            if keyField.waitForExistence(timeout: 5) {
                keyField.tap()
                keyField.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 128))
                keyField.typeText("uitest-dummy-key")
                app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
            }
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
        print("CLOUDAI_MODEL_SELECTED: \(chosen)")
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

    /// Bounded live-provider regression matrix. The simulator contains only
    /// fictional demo records; no user data is required for this coverage.
    /// Keep these turns sequential so each answer is settled before the next
    /// request and the provider receives no background/retry traffic.
    func testLiveCloudGroundingAndAnalysisMatrix() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live cloud matrix; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
        let app = TestHelpers.launchApp(arguments: ["-forceFmUnavailable"])
        let patientName = TestHelpers.firstPatientName(app)
        try configureLivePaidMimoModelManually(app)
        openAssistant(app)

        XCTAssertFalse(
            app.staticTexts["On-device AI not available here"].waitForExistence(timeout: 5),
            "Cloud configuration did not make the assistant available",
        )
        let input = app.textFields["assistant_input"].firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 10), "Assistant input is unavailable")

        // Do not let a previous persisted conversation change the grounding
        // subject of this matrix.
        let newChat = app.buttons["assistant_new_chat"].firstMatch
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat action is unavailable")
        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not clear the visible transcript",
        )

        let patientReply = try askAndWait(app, input: input, question: "Tell me about \(patientName)", replyIndex: 0)
        XCTAssertTrue(patientReply.localizedCaseInsensitiveContains(patientName), "Patient answer lost its subject: \(patientReply)")
        XCTAssertFalse(patientReply.localizedCaseInsensitiveContains("http"), "Patient answer fabricated a URL: \(patientReply)")
        XCTAssertTrue(
            app.buttons["assistant_source_chip"].firstMatch.waitForExistence(timeout: 10),
            "Grounded patient answer did not expose a source card",
        )
        XCTAssertTrue(
            app.descendants(matching: .any)
                .matching(identifier: "assistant_cloud_badge")
                .firstMatch
                .waitForExistence(timeout: 10),
            "The paid Mimo turn was not marked as cloud-served",
        )

        // This is deliberately a direct data projection: it verifies that the
        // breeding-card date is visible to the assistant independently of the
        // cloud model's ability to calculate it.
        let breedingReply = try askAndWait(
            app,
            input: input,
            question: "How long ago was \(patientName) bred?",
            replyIndex: 1,
        )
        XCTAssertTrue(
            breedingReply.contains("2026") || breedingReply.localizedCaseInsensitiveContains("apr"),
            "Breeding-card date was not surfaced: \(breedingReply)",
        )
        XCTAssertTrue(
            breedingReply.localizedCaseInsensitiveContains("bred") ||
                breedingReply.localizedCaseInsensitiveContains("breeding") ||
                breedingReply.localizedCaseInsensitiveContains("reproduction") ||
                breedingReply.localizedCaseInsensitiveContains("insemination"),
            "Breeding answer did not identify the recorded breeding event: \(breedingReply)",
        )

        let vaccinationReply = try askAndWait(
            app,
            input: input,
            question: "What vaccination is recorded for her?",
            replyIndex: 2,
        )
        XCTAssertTrue(
            vaccinationReply.localizedCaseInsensitiveContains("vaccin") || vaccinationReply.localizedCaseInsensitiveContains("tetanus") || vaccinationReply.localizedCaseInsensitiveContains("influenza"),
            "Vaccination answer did not reflect the record: \(vaccinationReply)",
        )
        XCTAssertFalse(vaccinationReply.localizedCaseInsensitiveContains("http"), "Vaccination answer fabricated a URL: \(vaccinationReply)")

        let censusReply = try askAndWait(
            app,
            input: input,
            question: "How many patients do I have?",
            replyIndex: 3,
        )
        XCTAssertTrue(
            censusReply.localizedCaseInsensitiveContains("patient") || censusReply.localizedCaseInsensitiveContains("horse"),
            "Census answer did not describe the patient count: \(censusReply)",
        )
        XCTAssertFalse(censusReply.localizedCaseInsensitiveContains("http"), "Census answer fabricated a URL: \(censusReply)")

        let analysisReply = try askAndWait(
            app,
            input: input,
            question: "Analyse the recorded weights across my patients and report any clear trend.",
            replyIndex: 4,
        )
        XCTAssertTrue(
            analysisReply.localizedCaseInsensitiveContains("weight") || analysisReply.localizedCaseInsensitiveContains("trend") || analysisReply.localizedCaseInsensitiveContains("measurement"),
            "Analysis answer did not discuss the requested data: \(analysisReply)",
        )
        XCTAssertFalse(analysisReply.localizedCaseInsensitiveContains("http"), "Analysis answer fabricated a URL: \(analysisReply)")

        let absentFactReply = try askAndWait(
            app,
            input: input,
            question: "What is the vaccination date for a horse named Pegasus?",
            replyIndex: 5,
        )
        XCTAssertTrue(
            absentFactReply.localizedCaseInsensitiveContains("couldn't find") ||
                absentFactReply.localizedCaseInsensitiveContains("not found") ||
                absentFactReply.localizedCaseInsensitiveContains("no record") ||
                absentFactReply.localizedCaseInsensitiveContains("não encontrei") ||
                absentFactReply.localizedCaseInsensitiveContains("não encontrado") ||
                absentFactReply.localizedCaseInsensitiveContains("nenhuma informação") ||
                absentFactReply.localizedCaseInsensitiveContains("sem registo"),
            "Missing patient fact was not answered honestly: \(absentFactReply)",
        )
        XCTAssertFalse(absentFactReply.localizedCaseInsensitiveContains("http"), "Missing-fact answer fabricated a URL: \(absentFactReply)")

        let generalReply = try askAndWait(
            app,
            input: input,
            question: "What is the capital of Portugal?",
            replyIndex: 6,
        )
        XCTAssertTrue(
            generalReply.localizedCaseInsensitiveContains("lisbon") || generalReply.localizedCaseInsensitiveContains("lisboa"),
            "General cloud question was not answered directly: \(generalReply)",
        )
        XCTAssertFalse(
            generalReply.localizedCaseInsensitiveContains("not found in records"),
            "General cloud question was incorrectly rejected as a record lookup: \(generalReply)",
        )
    }

    private func askAndWait(
        _ app: XCUIApplication,
        input: XCUIElement,
        question: String,
        replyIndex: Int,
    ) throws -> String {
        TestHelpers.typeSearchText(app, field: input, text: question)
        let send = app.buttons["assistant_send"].firstMatch
        XCTAssertTrue(send.waitForExistence(timeout: 5), "Send action missing for '\(question)'")
        send.tap()

        // Each turn is rendered as USER + ASSISTANT, so the assistant bubble
        // for replyIndex has the corresponding odd message index. Using its
        // stable identifier avoids accidentally reusing the prior bubble while
        // the new request is still in retrieval.
        let messageIndex = replyIndex * 2 + 1
        let reply = app.descendants(matching: .any)
            .matching(identifier: "assistant_message_\(messageIndex)")
            .firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 180), "No answer appeared for '\(question)'")
        // The assistant exposes a cumulative accessibility label while its
        // retrieval/tool loop is streaming. Do not mistake the interim
        // "Searching your records…" bubble or an early cumulative provider
        // chunk for the final response.
        let deadline = Date().addingTimeInterval(180)
        var label = reply.label
        var previousLabel = ""
        var stableSince: Date?

        while Date() < deadline {
            label = reply.label
            let isRetrieving = label.localizedCaseInsensitiveContains("Searching your records")

            if !isRetrieving {
                if label == previousLabel {
                    if let stableSince,
                       Date().timeIntervalSince(stableSince) >= 1.5 {
                        break
                    }
                } else {
                    previousLabel = label
                    stableSince = Date()
                }
            } else {
                previousLabel = ""
                stableSince = nil
            }

            RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.25))
        }
        XCTAssertFalse(
            label.localizedCaseInsensitiveContains("Searching your records"),
            "Answer did not leave the retrieval state for '\(question)': \(label)",
        )
        let inputReadyDeadline = Date().addingTimeInterval(30)
        while !input.isEnabled && Date() < inputReadyDeadline {
            RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.25))
        }
        XCTAssertTrue(input.isEnabled, "Input remained disabled after '\(question)': \(label)")
        print("CLOUDAI_REPLY_\(replyIndex): \(label)")
        let diagnosticLabels = app.staticTexts.allElementsBoundByIndex
            .map(\.label)
            .filter { label in
                let lowered = label.lowercased()
                return lowered.contains("cloud") || lowered.contains("finish") ||
                    lowered.contains("unable") || lowered.contains("couldn") ||
                    lowered.contains("error")
            }
        if !diagnosticLabels.isEmpty {
            print("CLOUDAI_DIAGNOSTICS_\(replyIndex): \(diagnosticLabels.joined(separator: " | "))")
        }
        XCTAssertGreaterThan(label.count, 20, "Cloud answer suspiciously short for '\(question)': \(label)")
        return label
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
