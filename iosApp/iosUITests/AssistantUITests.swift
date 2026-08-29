import XCTest

/// Assistant chat coverage through the real UI.
///
/// On Apple-Intelligence devices (physical iPhone 16 Pro) these exercise the
/// full Foundation Models path end-to-end; on the simulator FM reports
/// unavailable and the tests skip gracefully.
final class AssistantUITests: AnimallyTestCase {
    /// Opens the Assistant tab (collapsed under "More" once the tab bar caps at 5).
    @discardableResult
    private func openAssistant(_ app: XCUIApplication) -> XCUIElement {
        let assistantTab = app.tabBars.buttons["Assistant"]
        if assistantTab.exists {
            assistantTab.tap()
        } else {
            app.tabBars.buttons["More"].tap()
            let row = app.buttons["Assistant"].firstMatch
            XCTAssertTrue(row.waitForExistence(timeout: 5))
            row.tap()
        }
        return app
    }

    /// Skips the test when the on-device model is unavailable (simulator).
    private func requireAvailableModel(_ app: XCUIApplication) throws {
        let unavailable = app.staticTexts["On-device AI not available here"]
        if unavailable.waitForExistence(timeout: 5) {
            throw XCTSkip("Foundation Models unavailable on this device")
        }
        XCTAssertTrue(app.textFields["assistant_input"].waitForExistence(timeout: 10))

        // Chat history is intentionally restored on launch. Clear only the
        // visible conversation so this test reads the reply it just created,
        // while the persisted archive remains available to the product.
        let newChat = app.buttons["assistant_new_chat"]
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat control missing")
        let ready = NSPredicate(format: "isEnabled == true")
        expectation(for: ready, evaluatedWith: newChat)
        waitForExpectations(timeout: 10)
        newChat.tap()
        XCTAssertTrue(app.textFields["assistant_input"].exists)
    }

    /// Types a question into the chat input without polling the live
    /// accessibility value after every character.
    private func ask(
        _ app: XCUIApplication,
        _ question: String,
    ) {
        let input = app.textFields["assistant_input"]
        TestHelpers.typeAssistantQuestion(input, text: question)
        let send = app.buttons["assistant_send"]
        XCTAssertTrue(send.waitForExistence(timeout: 5))
        send.tap()
    }

    /// Waits for the assistant's reply bubble to appear.
    private func waitForReply(_ app: XCUIApplication) {
        let reply = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Assistant:")
        ).firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 180), "Assistant reply never appeared")
    }

    func testChatHistoryCanBeOpened() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)

        let history = app.buttons["assistant_chat_history_button"]
        XCTAssertTrue(history.waitForExistence(timeout: 10), "Chat history entry point missing")
        history.tap()

        XCTAssertTrue(app.staticTexts["Chat history"].waitForExistence(timeout: 10))
        XCTAssertTrue(
            app.buttons["assistant_history_done"].waitForExistence(timeout: 10),
            "Chat history sheet did not expose a close action"
        )
        app.buttons["assistant_history_done"].tap()
        XCTAssertTrue(app.textFields["assistant_input"].waitForExistence(timeout: 10))
    }

    func testNewChatActionClearsTheVisibleConversation() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        let newChat = app.buttons["assistant_new_chat"]
        XCTAssertTrue(newChat.waitForExistence(timeout: 10), "New chat entry point missing")
        let ready = NSPredicate(format: "isEnabled == true")
        expectation(for: ready, evaluatedWith: newChat)
        waitForExpectations(timeout: 10)

        newChat.tap()
        XCTAssertTrue(
            app.staticTexts["What would you like to know?"].waitForExistence(timeout: 10),
            "New chat did not return to the blank conversation state"
        )
        XCTAssertTrue(app.textFields["assistant_input"].exists)
    }

    func testChangingDictationLanguageKeepsTheSheetOpen() throws {
        let app = TestHelpers.launchApp(arguments: ["-animally-ui-test-dictation"])
        openAssistant(app)

        let dictate = app.buttons["assistant_dictate"]
        XCTAssertTrue(dictate.waitForExistence(timeout: 10), "Dictation entry point missing")
        dictate.tap()

        let start = app.buttons["dictation_start"]
        XCTAssertTrue(start.waitForExistence(timeout: 10), "Dictation sheet did not open")
        XCTAssertTrue(start.isEnabled, "Dictation engine did not become ready")

        let languagePicker = app.segmentedControls["dictation_language"]
        XCTAssertTrue(languagePicker.waitForExistence(timeout: 5), "Language picker missing")
        XCTAssertEqual(languagePicker.buttons.count, 2)
        languagePicker.buttons.element(boundBy: 1).tap()

        XCTAssertTrue(app.staticTexts["Dictate records"].exists, "Language change dismissed the dictation sheet")
        XCTAssertTrue(start.waitForExistence(timeout: 10), "Start control disappeared after language change")
        XCTAssertTrue(start.isEnabled, "Dictation engine did not recover after language change")
    }

    func testDictationSheetCanBeReopenedWithTheSameAccent() throws {
        let app = TestHelpers.launchApp(arguments: ["-animally-ui-test-dictation"])
        selectPlumAccent(app)
        openAssistant(app)

        let dictate = app.buttons["assistant_dictate"]
        XCTAssertTrue(dictate.waitForExistence(timeout: 10), "Dictation entry point missing")
        dictate.tap()

        let firstStart = app.buttons["dictation_start"]
        XCTAssertTrue(firstStart.waitForExistence(timeout: 10), "Dictation sheet did not open")
        XCTAssertTrue(firstStart.isEnabled, "Dictation engine did not become ready")
        let firstColor = sampledColor(from: firstStart.screenshot())
        firstStart.tap()

        XCTAssertTrue(app.buttons["dictation_stop"].waitForExistence(timeout: 10))
        let cancel = app.buttons["Cancel"].firstMatch
        XCTAssertTrue(cancel.waitForExistence(timeout: 5), "Dictation cancel action missing")
        cancel.tap()
        XCTAssertTrue(dictate.waitForExistence(timeout: 10), "Dictation sheet did not dismiss")

        dictate.tap()
        let secondStart = app.buttons["dictation_start"]
        XCTAssertTrue(secondStart.waitForExistence(timeout: 10), "Dictation sheet did not reopen")
        XCTAssertTrue(secondStart.isEnabled, "Reopened dictation engine did not become ready")
        let secondColor = sampledColor(from: secondStart.screenshot())

        let channelDifference = zip(firstColor, secondColor)
            .map { abs(Int($0.0) - Int($0.1)) }
            .reduce(0, +)
        XCTAssertLessThan(channelDifference, 32, "Dictation accent changed after sheet re-entry")
    }

    /// Polls until the reply label stops growing across consecutive reads so
    /// streaming has settled before the label is asserted on. Returns the
    /// settled label (or the last read when the deadline expires).
    private func settledReplyLabel(_ app: XCUIApplication, maxWait: TimeInterval = 30) -> String {
        let reply = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Assistant:")
        ).firstMatch
        var last = reply.label
        let deadline = Date().addingTimeInterval(maxWait)
        while Date() < deadline {
            Thread.sleep(forTimeInterval: 1.5)
            let current = reply.label
            if current == last { break }
            last = current
        }
        return last
    }

    func testSendQuestionReceivesAnswer() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "How many patients do I have?")
        waitForReply(app)
        let replyLabel = settledReplyLabel(app)

        // The reply must be substantive, not an empty or error bubble.
        XCTAssertGreaterThan(replyLabel.count, 20, "Assistant reply suspiciously short: \(replyLabel)")
    }

    func testKeyboardDismissesAfterSendAndNavigationUnblocked() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "List my patients")
        waitForReply(app)

        // Focus must resign on send so the keyboard drops without manual action.
        let keyboardGone = NSPredicate(format: "count == 0")
        let keyboardExpectation = expectation(for: keyboardGone, evaluatedWith: app.keyboards)
        wait(for: [keyboardExpectation], timeout: 10)

        // With the keyboard gone the tab bar must be reachable again.
        let searchTab = app.tabBars.buttons["Search"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "Tab bar unreachable after sending")
        searchTab.tap()
        XCTAssertTrue(app.staticTexts["Search"].waitForExistence(timeout: 10), "Navigation blocked after send")
    }

    func testSwipeDownDismissesKeyboardMidComposition() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        let input = app.textFields["assistant_input"]
        input.tap()
        TestHelpers.typeAssistantQuestion(input, text: "draft only")
        XCTAssertEqual(app.keyboards.count, 1, "Keyboard should be visible while typing")

        // No messages yet -> transcript is absent; the empty-chat area is the
        // tap-to-dismiss surface in this state. Tap near the top of it.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()

        let keyboardGone = NSPredicate(format: "count == 0")
        let keyboardExpectation = expectation(for: keyboardGone, evaluatedWith: app.keyboards)
        wait(for: [keyboardExpectation], timeout: 10)
    }

    /// Exercises the complete native dictation orchestration without relying
    /// on simulator microphone routing, speech assets, or Foundation Models.
    /// The launch argument swaps only the two native edges for deterministic
    /// fakes; Kotlin validation, patient resolution, review and save controls
    /// remain the real production path.
    func testDictationFlowCanBeExercisedDeterministically() throws {
        let app = TestHelpers.launchApp(arguments: ["-animally-ui-test-dictation"])
        openAssistant(app)

        let dictate = app.buttons["assistant_dictate"]
        XCTAssertTrue(dictate.waitForExistence(timeout: 10), "Dictation entry point missing")
        dictate.tap()

        let start = app.buttons["dictation_start"]
        XCTAssertTrue(start.waitForExistence(timeout: 10), "Dictation sheet did not open")
        XCTAssertTrue(start.waitForExistence(timeout: 10) && start.isEnabled, "Dictation engine did not become ready")
        start.tap()

        let stop = app.buttons["dictation_stop"]
        XCTAssertTrue(stop.waitForExistence(timeout: 10), "Recording state did not appear")
        let liveTranscript = app.descendants(matching: .any)
            .matching(identifier: "dictation_live_transcript").firstMatch
        XCTAssertTrue(liveTranscript.waitForExistence(timeout: 5))
        stop.tap()

        let editor = app.textViews["dictation_transcript_editor"]
        XCTAssertTrue(editor.waitForExistence(timeout: 10), "Transcript review did not appear")
        XCTAssertFalse((editor.value as? String ?? "").isEmpty, "Mock transcript was not delivered")

        // The reviewed text is the source of truth for both extraction and
        // the archive. Add a marker that cannot come from the mock speech
        // result so this catches accidental use of the original transcript.
        let editedMarker = "Edited during review"
        editor.tap()
        editor.typeText(" \(editedMarker)")
        XCTAssertTrue(
            (editor.value as? String ?? "").contains(editedMarker),
            "Transcript editor did not retain the review edit"
        )

        app.buttons["dictation_extract"].tap()
        let review = app.descendants(matching: .any)
            .matching(identifier: "dictation_review").firstMatch
        XCTAssertTrue(review.waitForExistence(timeout: 15), "Suggestion review did not appear")
        XCTAssertTrue(app.staticTexts["Needs attention"].waitForExistence(timeout: 5))

        let accept = app.buttons["dictation_accept_0"]
        XCTAssertTrue(accept.waitForExistence(timeout: 5), "Suggestion decisions did not appear")
        XCTAssertTrue(accept.isEnabled, "The resolved suggestion should be acceptable")
        accept.tap()
        XCTAssertTrue(app.buttons["Save 1 record"].waitForExistence(timeout: 5), "Save control did not become available")

        // Saving the structured record must not discard the original dictation
        // note. Confirm the review, open the archive, and verify that the
        // transcript is available independently of the record insertion.
        app.buttons["Save 1 record"].tap()
        let history = app.buttons["assistant_dictation_history"]
        XCTAssertTrue(history.waitForExistence(timeout: 10), "Dictation history entry point missing")
        history.tap()

        XCTAssertTrue(app.staticTexts["Dictation history"].waitForExistence(timeout: 10))
        XCTAssertTrue(
            app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", editedMarker)).firstMatch
                .waitForExistence(timeout: 10),
            "Edited dictation transcript was not retained"
        )

        let firstCapture = app.buttons.matching(
            NSPredicate(format: "label == %@", "Play recording")
        ).firstMatch
        XCTAssertTrue(firstCapture.waitForExistence(timeout: 10), "Saved dictation audio was not playable")
        XCTAssertTrue(firstCapture.isEnabled, "Saved dictation audio play action was disabled")
        firstCapture.tap()

        let stopPlayback = app.buttons.matching(
            NSPredicate(format: "label == %@", "Stop recording playback")
        ).firstMatch
        XCTAssertTrue(
            stopPlayback.waitForExistence(timeout: 5),
            "Playback did not enter the playing state"
        )
        let progress = app.sliders.firstMatch
        XCTAssertTrue(progress.waitForExistence(timeout: 5), "Playback progress slider was not shown")
        progress.adjust(toNormalizedSliderPosition: 0.1)

        let speed = app.buttons["Playback speed"]
        XCTAssertTrue(speed.waitForExistence(timeout: 5), "Playback speed control was not shown")
        speed.tap()
        let faster = app.buttons["1.5x"].firstMatch
        XCTAssertTrue(faster.waitForExistence(timeout: 5), "1.5x playback option was not shown")
        faster.tap()

        stopPlayback.tap()
        XCTAssertTrue(
            app.buttons.matching(NSPredicate(format: "label == %@", "Play recording"))
                .firstMatch
                .waitForExistence(timeout: 5)
        )
    }

    private func selectPlumAccent(_ app: XCUIApplication) {
        let settings = app.buttons["Settings"].firstMatch
        XCTAssertTrue(settings.waitForExistence(timeout: 10), "Settings entry point missing")
        settings.tap()

        let plum = app.buttons["settings_accent_plum"].firstMatch
        XCTAssertTrue(plum.waitForExistence(timeout: 10), "Plum accent is missing")
        plum.tap()

        let done = app.buttons["Done"].firstMatch
        XCTAssertTrue(done.waitForExistence(timeout: 10), "Settings close action missing")
        done.tap()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 10))
    }

    private func sampledColor(from screenshot: XCUIScreenshot) -> [UInt8] {
        guard
            let image = screenshot.image.cgImage,
            let data = image.dataProvider?.data,
            let bytes = CFDataGetBytePtr(data)
        else {
            XCTFail("Could not inspect the dictation button screenshot")
            return []
        }

        let bytesPerPixel = max(1, image.bitsPerPixel / 8)
        let x = max(0, min(image.width - 1, image.width / 8))
        let y = max(0, min(image.height - 1, image.height / 2))
        let offset = y * image.bytesPerRow + x * bytesPerPixel
        return (0..<min(bytesPerPixel, 4)).map { bytes[offset + $0] }
    }
}

/// Real-FM behavioral coverage.
///
/// On macOS 26 hosts the simulator proxies the host Foundation Model, so
/// these run against the LIVE model. Assertions are deliberately loose -
/// generation is nondeterministic - asserting routing correctness (greeting
/// vs retrieval vs too-short), citation presence, scaffold-free output, and
/// no fabricated sources rather than exact wording.
final class AssistantRealFmUITests: AnimallyTestCase {
    @discardableResult
    private func openAssistant(_ app: XCUIApplication) -> XCUIElement {
        let assistantTab = app.tabBars.buttons["Assistant"]
        if assistantTab.exists {
            assistantTab.tap()
        } else {
            app.tabBars.buttons["More"].tap()
            let row = app.buttons["Assistant"].firstMatch
            XCTAssertTrue(row.waitForExistence(timeout: 5))
            row.tap()
        }
        return app
    }

    private func requireAvailableModel(_ app: XCUIApplication) throws {
        let unavailable = app.staticTexts["On-device AI not available here"]
        if unavailable.waitForExistence(timeout: 5) {
            throw XCTSkip("Foundation Models unavailable on this device")
        }
        XCTAssertTrue(app.textFields["assistant_input"].waitForExistence(timeout: 10))
    }

    private func ask(
        _ app: XCUIApplication,
        _ question: String,
    ) {
        let input = app.textFields["assistant_input"]
        TestHelpers.typeAssistantQuestion(input, text: question)
        let send = app.buttons["assistant_send"]
        XCTAssertTrue(send.waitForExistence(timeout: 5))
        send.tap()
    }

    /// Waits for the first reply chunk, then settles so streaming finishes
    /// before the label is read.
    private func completedReplyLabel(_ app: XCUIApplication) -> String {
        let reply = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Assistant:")
        ).firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 180), "Assistant reply never appeared")
        Thread.sleep(forTimeInterval: 6)
        return reply.label
    }

    func testGreetingGetsFriendlyReplyNotFallback() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Hi")
        let label = completedReplyLabel(app)

        XCTAssertFalse(label.contains("couldn't find"), "Greeting routed to retrieval fallback: \(label)")
        XCTAssertTrue(label.lowercased().contains("hello") || label.lowercased().contains("ask"), "Greeting reply unfriendly: \(label)")
    }

    func testSingleLetterQueryAsksForMoreInsteadOfMatching() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "A")
        let label = completedReplyLabel(app)

        XCTAssertFalse(label.contains("[PATIENT #"), "One-letter query fuzzy-matched a record: \(label)")
        XCTAssertTrue(label.contains("more to go on"), "Too-short guard did not fire: \(label)")
    }

    func testPatientQuestionAnswersWithCitationAndNoScaffold() throws {
        let app = TestHelpers.launchApp()
        let patientName = TestHelpers.firstPatientName(app)
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Tell me about \(patientName)")
        let label = completedReplyLabel(app)

        XCTAssertTrue(label.localizedCaseInsensitiveContains(patientName), "Answer lost the subject: \(label)")
        let sourceChip = app.buttons["assistant_source_chip"].firstMatch
        XCTAssertTrue(
            sourceChip.waitForExistence(timeout: 10),
            "No tappable source card in answer: \(label)"
        )
        XCTAssertFalse(label.contains("---"), "Scaffold separator leaked: \(label)")
        XCTAssertFalse(label.contains("Question:"), "Prompt echo leaked: \(label)")
        XCTAssertFalse(label.lowercased().contains("http"), "External URL fabricated: \(label)")
    }

    func testUnknownThingDoesNotFabricateSources() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "How many giraffes are in my records?")
        let label = completedReplyLabel(app)

        XCTAssertFalse(label.lowercased().contains("http"), "Fabricated external source: \(label)")
        XCTAssertFalse(label.contains("[Giraffe"), "Fabricated record type: \(label)")
    }

    func testCompletedAnswerShowsFollowUpChips() throws {
        // Every completed answer carries at least the default suggestion set,
        // so the chips must exist regardless of what the model cited.
        let app = TestHelpers.launchApp()
        let patientName = TestHelpers.firstPatientName(app)
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Tell me about \(patientName)")
        _ = completedReplyLabel(app)

        let chip = app.buttons["assistant_followup_chip"].firstMatch
        XCTAssertTrue(chip.waitForExistence(timeout: 10), "Follow-up chips missing after completed answer")
    }
}
