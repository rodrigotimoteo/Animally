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
    }

    /// Types a question into the chat input using chunked typing with
    /// verify-and-retry (XCUITest drops keystrokes on live-binding fields).
    private func ask(
        _ app: XCUIApplication,
        _ question: String,
    ) {
        let input = app.textFields["assistant_input"]
        TestHelpers.typeSearchText(app, field: input, text: question)
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
        TestHelpers.typeSearchText(app, field: input, text: "draft only")
        XCTAssertEqual(app.keyboards.count, 1, "Keyboard should be visible while typing")

        // No messages yet -> transcript is absent; the empty-chat area is the
        // tap-to-dismiss surface in this state. Tap near the top of it.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()

        let keyboardGone = NSPredicate(format: "count == 0")
        let keyboardExpectation = expectation(for: keyboardGone, evaluatedWith: app.keyboards)
        wait(for: [keyboardExpectation], timeout: 10)
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
        TestHelpers.typeSearchText(app, field: input, text: question)
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
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Tell me about Thunder")
        let label = completedReplyLabel(app)

        XCTAssertTrue(label.contains("Thunder"), "Answer lost the subject: \(label)")
        XCTAssertTrue(label.contains("[PATIENT #") || label.contains("["), "No citation in answer: \(label)")
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
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Tell me about Thunder")
        _ = completedReplyLabel(app)

        let chip = app.buttons["assistant_followup_chip"].firstMatch
        XCTAssertTrue(chip.waitForExistence(timeout: 10), "Follow-up chips missing after completed answer")
    }
}
