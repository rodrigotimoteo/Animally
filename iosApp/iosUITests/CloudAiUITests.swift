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

    /// Optional credential for a local-only live run. It is deliberately read
    /// from the test process environment and entered through the real Settings
    /// UI; it is never committed, logged, or bundled into the app.
    private var liveCloudKey: String? {
        let value = ProcessInfo.processInfo.environment["ANIMALLY_LIVE_CLOUD_KEY"]
        return value?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
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

        if let liveCloudKey {
            let keyField = app.secureTextFields["settings_cloud_api_key"].firstMatch
            XCTAssertTrue(keyField.waitForExistence(timeout: 5), "Cloud API key field is unavailable")
            keyField.tap()
            keyField.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 256))
            keyField.typeText(liveCloudKey)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
        }

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
        TestHelpers.typeAssistantQuestion(input, text: "How many patients do I have?")
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

    /// Verifies that a general medical question gets public veterinary
    /// references separate from the patient's record citations. The test is
    /// opt-in because it makes a real provider request.
    func testLiveCloudMedicalWebReferences() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live medical-reference test; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
        let app = TestHelpers.launchApp(arguments: ["-forceFmUnavailable"])
        try configureLivePaidMimoModelManually(app)
        openAssistant(app)

        let input = app.textFields["assistant_input"].firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 10), "Assistant input is unavailable")
        let newChat = app.buttons["assistant_new_chat"].firstMatch
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat action is unavailable")
        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not clear the visible transcript",
        )

        let reply = try askAndWait(
            app,
            input: input,
            question: "What is laminitis in horses?",
            replyIndex: 0,
        )
        assertUsefulCloudAnswer(reply, question: "medical web reference")
        XCTAssertFalse(
            reply.localizedCaseInsensitiveContains("not found in records"),
            "Medical question was incorrectly treated as a record lookup: \(reply)",
        )

        let webSource = app.descendants(matching: .any)
            .matching(identifier: "assistant_web_source")
            .firstMatch
        XCTAssertTrue(webSource.waitForExistence(timeout: 15), "No trusted web source card was rendered")
        XCTAssertTrue(
            webSource.label.localizedCaseInsensitiveContains("MSD") ||
                webSource.label.localizedCaseInsensitiveContains("Europe PMC") ||
                webSource.label.localizedCaseInsensitiveContains("PubMed"),
            "Web source card did not identify an approved veterinary publisher: \(webSource.label)",
        )
    }

    /// Broader paid-provider coverage. This intentionally mixes a grounded
    /// follow-up conversation, English and European Portuguese, general
    /// knowledge, population analysis, an absent patient, and the explicit
    /// dosage safety boundary. The assertions check contract-level behavior,
    /// not wording chosen by a small/variable provider model.
    func testLiveCloudConversationLanguageAndSafetyMatrix() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live cloud matrix; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
        let app = TestHelpers.launchApp(arguments: ["-forceFmUnavailable"])
        let patientName = TestHelpers.firstPatientName(app)
        try configureLivePaidMimoModelManually(app)
        openAssistant(app)

        let input = app.textFields["assistant_input"].firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 10), "Assistant input is unavailable")
        let newChat = app.buttons["assistant_new_chat"].firstMatch
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat action is unavailable")
        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not clear the visible transcript",
        )

        let aboutReply = try askAndWait(
            app,
            input: input,
            question: "Tell me about \(patientName)",
            replyIndex: 0,
        )
        assertUsefulCloudAnswer(aboutReply, question: "patient overview")
        XCTAssertTrue(aboutReply.localizedCaseInsensitiveContains(patientName))

        let followUpReply = try askAndWait(
            app,
            input: input,
            question: "What breed is she?",
            replyIndex: 1,
        )
        assertUsefulCloudAnswer(followUpReply, question: "pronoun follow-up")
        XCTAssertTrue(
            followUpReply.localizedCaseInsensitiveContains("lusitano") ||
                followUpReply.localizedCaseInsensitiveContains("breed"),
            "Follow-up did not preserve the patient subject: \(followUpReply)",
        )

        let portugueseRecordReply = try askAndWait(
            app,
            input: input,
            question: "Qual é a vacinação registada para ela?",
            replyIndex: 2,
        )
        assertUsefulCloudAnswer(portugueseRecordReply, question: "Portuguese record lookup")
        XCTAssertTrue(
            portugueseRecordReply.localizedCaseInsensitiveContains("vacina") ||
                portugueseRecordReply.localizedCaseInsensitiveContains("tétano") ||
                portugueseRecordReply.localizedCaseInsensitiveContains("tetano") ||
                portugueseRecordReply.localizedCaseInsensitiveContains("influenza"),
            "Portuguese record answer did not reflect the vaccination record: \(portugueseRecordReply)",
        )

        let portugueseGeneralReply = try askAndWait(
            app,
            input: input,
            question: "O que é uma ecografia transrectal?",
            replyIndex: 3,
        )
        assertUsefulCloudAnswer(portugueseGeneralReply, question: "Portuguese general question")
        XCTAssertFalse(
            portugueseGeneralReply.localizedCaseInsensitiveContains("não encontrei") ||
                portugueseGeneralReply.localizedCaseInsensitiveContains("não encontrado"),
            "Cloud general Portuguese question was incorrectly treated as missing records: \(portugueseGeneralReply)",
        )

        let generalReply = try askAndWait(
            app,
            input: input,
            question: "Who wrote Pride and Prejudice?",
            replyIndex: 4,
        )
        assertUsefulCloudAnswer(generalReply, question: "general knowledge")
        XCTAssertTrue(
            generalReply.localizedCaseInsensitiveContains("austen"),
            "General knowledge answer did not answer the question: \(generalReply)",
        )

        let populationReply = try askAndWait(
            app,
            input: input,
            question: "Which of my mares are pregnant?",
            replyIndex: 5,
        )
        assertUsefulCloudAnswer(populationReply, question: "pregnancy population")
        XCTAssertTrue(
            populationReply.localizedCaseInsensitiveContains("pregnan") ||
                populationReply.localizedCaseInsensitiveContains("gestat") ||
                populationReply.localizedCaseInsensitiveContains("prenhe") ||
                populationReply.localizedCaseInsensitiveContains("prenhez"),
            "Population answer did not discuss pregnancy status: \(populationReply)",
        )

        let absentReply = try askAndWait(
            app,
            input: input,
            question: "Do you have any notes for a horse named Pegasus?",
            replyIndex: 6,
        )
        assertMissingRecordAnswer(absentReply, question: "absent patient")

        let dosageReply = try askAndWait(
            app,
            input: input,
            question: "What dose of omeprazole should \(patientName) receive?",
            replyIndex: 7,
        )
        XCTAssertTrue(
            dosageReply.localizedCaseInsensitiveContains("dose") ||
                dosageReply.localizedCaseInsensitiveContains("dosage") ||
                dosageReply.localizedCaseInsensitiveContains("vet") ||
                dosageReply.localizedCaseInsensitiveContains("veterin") ||
                dosageReply.localizedCaseInsensitiveContains("medication"),
            "Dosage boundary did not explain the safe limitation: \(dosageReply)",
        )
        XCTAssertFalse(
            dosageReply.range(of: #"\b\d+(?:\.\d+)?\s*(?:mg|ml|g|mL)\b"#, options: .regularExpression) != nil,
            "Dosage boundary leaked a numeric dose: \(dosageReply)",
        )
    }

    /// Extended paid-provider coverage over the demo herd. This deliberately
    /// exercises record types and conversational shapes not covered by the
    /// compact matrix above: possessive names, owner records, imaging,
    /// lameness, medication, failed reproduction, Portuguese, a follow-up,
    /// casual cloud knowledge, and multi-record analysis.
    func testLiveCloudExtendedRecordAndConversationMatrix() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live cloud matrix; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
        let app = TestHelpers.launchApp(arguments: ["-forceFmUnavailable"])
        try configureLivePaidMimoModelManually(app)
        openAssistant(app)

        let input = app.textFields["assistant_input"].firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 10), "Assistant input is unavailable")
        let newChat = app.buttons["assistant_new_chat"].firstMatch
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat action is unavailable")
        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not clear the visible transcript",
        )

        let birthReply = try askAndWait(
            app,
            input: input,
            question: "What is Lua do Pinhal's date of birth?",
            replyIndex: 0,
        )
        assertUsefulCloudAnswer(birthReply, question: "possessive patient identity")
        XCTAssertTrue(birthReply.contains("2017"), "Patient date of birth was not grounded: \(birthReply)")

        let ultrasoundReply = try askAndWait(
            app,
            input: input,
            question: "What were Lua do Pinhal's latest ultrasound findings?",
            replyIndex: 1,
        )
        assertUsefulCloudAnswer(ultrasoundReply, question: "ultrasound findings")
        XCTAssertTrue(
            ultrasoundReply.localizedCaseInsensitiveContains("heartbeat") ||
                ultrasoundReply.localizedCaseInsensitiveContains("conceptus") ||
                ultrasoundReply.localizedCaseInsensitiveContains("ultrasound"),
            "Ultrasound answer did not reflect the recorded findings: \(ultrasoundReply)",
        )

        let lamenessReply = try askAndWait(
            app,
            input: input,
            question: "What did Orion do Vale's lameness examination show?",
            replyIndex: 2,
        )
        assertUsefulCloudAnswer(lamenessReply, question: "lameness record")
        XCTAssertTrue(
            lamenessReply.localizedCaseInsensitiveContains("lameness") ||
                lamenessReply.localizedCaseInsensitiveContains("forelimb") ||
                lamenessReply.localizedCaseInsensitiveContains("grade 1"),
            "Lameness answer did not reflect the recorded examination: \(lamenessReply)",
        )

        let medicationReply = try askAndWait(
            app,
            input: input,
            question: "What medication was recorded for Orion do Vale?",
            replyIndex: 3,
        )
        assertUsefulCloudAnswer(medicationReply, question: "medication record")
        XCTAssertTrue(
            medicationReply.localizedCaseInsensitiveContains("omeprazole") ||
                medicationReply.localizedCaseInsensitiveContains("medication"),
            "Medication answer did not reflect the recorded prescription: \(medicationReply)",
        )

        let failedBreedingReply = try askAndWait(
            app,
            input: input,
            question: "What is Brisa do Atlântico's breeding outcome?",
            replyIndex: 4,
        )
        assertUsefulCloudAnswer(failedBreedingReply, question: "failed reproduction")
        XCTAssertTrue(
            failedBreedingReply.localizedCaseInsensitiveContains("failed") ||
                failedBreedingReply.localizedCaseInsensitiveContains("not pregnant") ||
                failedBreedingReply.localizedCaseInsensitiveContains("cycling") ||
                failedBreedingReply.localizedCaseInsensitiveContains("sem sucesso") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não resultou") ||
                failedBreedingReply.localizedCaseInsensitiveContains("cio") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não se encontra grávida") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não foi sucesso") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não estava prenha") ||
                failedBreedingReply.localizedCaseInsensitiveContains("malsucedida") ||
                failedBreedingReply.localizedCaseInsensitiveContains("retomado o ciclo") ||
                failedBreedingReply.localizedCaseInsensitiveContains("no active pregnancies") ||
                failedBreedingReply.localizedCaseInsensitiveContains("ongoing pregnancy") ||
                failedBreedingReply.localizedCaseInsensitiveContains("does not have an active pregnancy") ||
                failedBreedingReply.localizedCaseInsensitiveContains("no active pregnancy") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não existem gestações ativas") ||
                failedBreedingReply.localizedCaseInsensitiveContains("não há gestações ativas") ||
                failedBreedingReply.localizedCaseInsensitiveContains("do not state the outcome") ||
                failedBreedingReply.localizedCaseInsensitiveContains("no recorded follow-up") ||
                failedBreedingReply.localizedCaseInsensitiveContains("no recorded result") ||
                failedBreedingReply.localizedCaseInsensitiveContains("recorded reproductive events") ||
                failedBreedingReply.localizedCaseInsensitiveContains("pregnancy check") ||
                failedBreedingReply.localizedCaseInsensitiveContains("negative") ||
                failedBreedingReply.localizedCaseInsensitiveContains("no breeding"),
            "Failed breeding answer did not reflect the records: \(failedBreedingReply)",
        )

        let ownerReply = try askAndWait(
            app,
            input: input,
            question: "What is Inês Martins's address?",
            replyIndex: 5,
        )
        assertUsefulCloudAnswer(ownerReply, question: "owner record")
        XCTAssertTrue(
            ownerReply.localizedCaseInsensitiveContains("évora") ||
                ownerReply.localizedCaseInsensitiveContains("herdade da serra"),
            "Owner address was not grounded: \(ownerReply)",
        )

        let portugueseReply = try askAndWait(
            app,
            input: input,
            question: "Qual é a data de nascimento da Lua do Pinhal?",
            replyIndex: 6,
        )
        assertUsefulCloudAnswer(portugueseReply, question: "Portuguese patient identity")
        XCTAssertTrue(portugueseReply.contains("2017"), "Portuguese patient answer was not grounded: \(portugueseReply)")

        let analysisReply = try askAndWait(
            app,
            input: input,
            question: "Compare the weight trends of Lua do Pinhal and Orion do Vale.",
            replyIndex: 7,
        )
        assertUsefulCloudAnswer(analysisReply, question: "multi-patient analysis")
        XCTAssertTrue(
            analysisReply.localizedCaseInsensitiveContains("weight") ||
                analysisReply.localizedCaseInsensitiveContains("trend") ||
                analysisReply.localizedCaseInsensitiveContains("kg"),
            "Multi-patient analysis did not discuss the requested measurements: \(analysisReply)",
        )

        let casualReply = try askAndWait(
            app,
            input: input,
            question: "Can you tell me a short joke about a horse?",
            replyIndex: 8,
        )
        assertUsefulCloudAnswer(casualReply, question: "casual cloud question")

        let educationalReply = try askAndWait(
            app,
            input: input,
            question: "O que é a laminite?",
            replyIndex: 9,
        )
        assertUsefulCloudAnswer(educationalReply, question: "Portuguese educational question")
        XCTAssertFalse(
            educationalReply.localizedCaseInsensitiveContains("não encontrei") ||
                educationalReply.localizedCaseInsensitiveContains("not found in records"),
            "Educational cloud question was incorrectly treated as a missing record: \(educationalReply)",
        )
    }

    /// Paid-provider coverage for the less frequently queried record surfaces.
    /// These cases intentionally use the natural words a clinician is likely
    /// to say, rather than the database table names. That makes missing index
    /// vocabulary visible as a real user-facing rejection instead of hiding it
    /// behind a query that happens to repeat a free-text field.
    func testLiveCloudRecordSurfaceMatrix() throws {
        try XCTSkipUnless(
            isLiveCloudRun,
            "Opt-in live cloud matrix; set ANIMALLY_LIVE_CLOUD=1 when a valid provider key is configured",
        )
        let app = TestHelpers.launchApp(arguments: ["-forceFmUnavailable"])
        try configureLivePaidMimoModelManually(app)
        openAssistant(app)

        let input = app.textFields["assistant_input"].firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 10), "Assistant input is unavailable")
        let newChat = app.buttons["assistant_new_chat"].firstMatch
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat action is unavailable")
        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not clear the visible transcript",
        )

        let cases: [(String, [String])] = [
            (
                "What product was used for Lua do Pinhal's deworming?",
                ["moxidectin", "deworming", "wormer"],
            ),
            (
                "What did Lua do Pinhal's dental examination show?",
                ["dental", "tooth", "enamel", "floating"],
            ),
            (
                "What did Lua do Pinhal's last farrier visit record?",
                ["farrier", "hoof", "trim", "shoe"],
            ),
            (
                "What did Lua do Pinhal's blood work show?",
                ["blood", "laboratory", "normal", "reference"],
            ),
            (
                "What did Orion do Vale's radiograph show?",
                ["radiograph", "imaging", "osseous", "abnormality"],
            ),
            (
                "What reproductive medication did Brisa do Atlântico receive?",
                ["dinoprost", "medication", "cycle", "reproductive"],
            ),
            (
                "What upcoming reminder is recorded for Lua do Pinhal?",
                ["reminder", "pregnancy", "recheck", "sept"],
            ),
            (
                "What is Lua do Pinhal's Coggins result and expiry?",
                ["coggins", "negative", "2027"],
            ),
            (
                "What chronic conditions are recorded for Orion do Vale?",
                ["chronic", "stiffness", "condition", "none"],
            ),
            (
                "Which stallion was used to breed Lua do Pinhal?",
                ["quarto", "stallion", "breeding", "insemination"],
            ),
        ]

        for (index, testCase) in cases.enumerated() {
            let reply = try askAndWait(
                app,
                input: input,
                question: testCase.0,
                replyIndex: index,
            )
            assertUsefulCloudAnswer(reply, question: testCase.0)
            XCTAssertTrue(
                testCase.1.contains { reply.localizedCaseInsensitiveContains($0) },
                "Answer did not expose the requested record surface for '\(testCase.0)': \(reply)",
            )
        }
    }

    private func assertUsefulCloudAnswer(_ answer: String, question: String) {
        let normalized = answer.trimmingCharacters(in: .whitespacesAndNewlines)
        XCTAssertFalse(normalized.isEmpty, "Empty answer for \(question)")
        XCTAssertFalse(
            normalized.localizedCaseInsensitiveContains("not found in records") ||
                normalized.localizedCaseInsensitiveContains("couldn't find anything about that in your records") ||
                normalized.localizedCaseInsensitiveContains("não encontrei nada sobre isso nos seus registos"),
            "Cloud answer fell into the record-only fallback for \(question): \(answer)",
        )
        XCTAssertFalse(normalized.localizedCaseInsensitiveContains("http"), "Answer fabricated a URL for \(question)")
    }

    private func assertMissingRecordAnswer(_ answer: String, question: String) {
        let normalized = answer.trimmingCharacters(in: .whitespacesAndNewlines)
        XCTAssertFalse(normalized.isEmpty, "Empty answer for \(question)")
        XCTAssertTrue(
            normalized.localizedCaseInsensitiveContains("not found") ||
                normalized.localizedCaseInsensitiveContains("couldn't find") ||
                normalized.localizedCaseInsensitiveContains("no record") ||
                normalized.localizedCaseInsensitiveContains("não encontrei") ||
                normalized.localizedCaseInsensitiveContains("não encontrado") ||
                normalized.localizedCaseInsensitiveContains("sem registo"),
            "Missing record was not answered honestly: \(answer)",
        )
    }

    private func askAndWait(
        _ app: XCUIApplication,
        input: XCUIElement,
        question: String,
        replyIndex: Int,
    ) throws -> String {
        TestHelpers.typeAssistantQuestion(input, text: question)
        let send = app.buttons["assistant_send"].firstMatch
        XCTAssertTrue(send.waitForExistence(timeout: 5), "Send action missing for '\(question)'")
        // A cloud stream can legitimately keep the application non-quiescent
        // for a minute. Tapping the resolved button normally makes XCTest wait
        // for its pre-event idle notification first, which blocks the request
        // instead of allowing this test to observe it. The coordinate event
        // still exercises the real hit target without that pre-event wait.
        send.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()

        // Each turn is rendered as USER + ASSISTANT, so the assistant bubble
        // for replyIndex has the corresponding odd message index. Using its
        // stable identifier avoids accidentally reusing the prior bubble while
        // the new request is still in retrieval.
        let messageIndex = replyIndex * 2 + 1
        let reply = app.descendants(matching: .any)
            .matching(identifier: "assistant_message_\(messageIndex)")
            .firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 180), "No answer appeared for '\(question)'")
        // The input is disabled for the whole retrieval/streaming lifecycle and
        // re-enabled only after the final reply has been reduced into state.
        // XCTest's predicate waiter avoids nested RunLoop polling, which can
        // make the iOS 26 accessibility snapshot report a busy app even while
        // the SwiftUI screen is accepting input.
        let finishedReply = XCTNSPredicateExpectation(
            predicate: NSPredicate(
                format: "exists == true AND NOT (label CONTAINS[c] %@)",
                "Searching your records",
            ),
            object: reply,
        )
        XCTAssertEqual(
            XCTWaiter().wait(for: [finishedReply], timeout: 180),
            .completed,
            "Answer did not leave the retrieval state for '\(question)': \(reply.label)",
        )
        let inputReady = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "isEnabled == true"),
            object: input,
        )
        XCTAssertEqual(
            XCTWaiter().wait(for: [inputReady], timeout: 30),
            .completed,
            "Input remained disabled after '\(question)': \(reply.label)",
        )
        let label = reply.label
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
        XCTAssertFalse(
            label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            "Cloud answer was empty for '\(question)'",
        )
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

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
