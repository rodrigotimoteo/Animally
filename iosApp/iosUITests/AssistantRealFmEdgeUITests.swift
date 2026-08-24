import XCTest

/// Real-FM edge-case behavioral coverage.
///
/// Runs against the LIVE Apple Foundation Model on the macOS 26 host
/// (simulator proxies the host model). Assertions are deliberately loose -
/// generation is nondeterministic - asserting language mirroring, honest
/// not-found behavior, context retention, streaming liveness, and crash
/// resistance rather than exact wording.
final class AssistantRealFmEdgeUITests: AnimallyTestCase {
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

    private func replyQuery(_ app: XCUIApplication) -> XCUIElementQuery {
        app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Assistant:")
        )
    }

    /// Waits for the first reply chunk, then settles so streaming finishes
    /// before the label is read.
    @discardableResult
    private func waitForReply(_ app: XCUIApplication, minCount: Int = 1) -> [String] {
        let query = replyQuery(app)
        XCTAssertTrue(
            query.element(boundBy: minCount - 1).waitForExistence(timeout: 180),
            "Assistant reply never appeared"
        )
        Thread.sleep(forTimeInterval: 6)
        return query.allElementsBoundByIndex.map(\.label)
    }

    // MARK: - Diagnostics

    /// TEMPORARY: captures the error banner + transcript after an FM question.
    func testZZDiagnosticDump() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "How many patients do I have?")
        _ = waitForReply(app)
        Thread.sleep(forTimeInterval: 4)

        print("===== DUMP: FM diagnostic =====")
        print("BANNERS: \(app.staticTexts.allElementsBoundByIndex.map(\.label).filter { !$0.isEmpty })")
        for el in replyQuery(app).allElementsBoundByIndex {
            print("REPLY BUBBLE: [\(el.label)]")
        }
        print("===== END DUMP =====")
    }

    func testPortugueseQuestionAnsweredNotFallback() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Quantos pacientes tenho?")
        let labels = waitForReply(app)
        let reply = labels.last ?? ""

        XCTAssertGreaterThan(reply.count, 20, "Reply suspiciously short: \(reply)")
        XCTAssertFalse(
            reply.contains("couldn't find"),
            "PT question routed to retrieval fallback instead of answering: \(reply)"
        )
    }

    func testDiacriticNameVariantsNeverFabricate() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        // Both diacritic variants of the same name must be handled honestly:
        // a real answer or the no-results fallback is fine, fabrication is not.
        ask(app, "Tell me about Descarada")
        _ = waitForReply(app)
        ask(app, "Tell me about descarada")
        let labels = waitForReply(app, minCount: 2)

        for reply in labels.suffix(2) {
            XCTAssertFalse(reply.lowercased().contains("http"), "Fabricated external source: \(reply)")
            XCTAssertFalse(reply.contains("[Giraffe"), "Fabricated record type: \(reply)")
        }
    }

    func testFarrierQuestionCitesOrHonestNotFound() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "When was Thunder's last farrier visit?")
        let labels = waitForReply(app)
        let reply = labels.last ?? ""

        let cited = reply.contains("[")
        let honestNotFound =
            reply.lowercased().contains("couldn't find")
            || reply.lowercased().contains("don't have")
            || reply.lowercased().contains("do not have")
            || reply.lowercased().contains("no record")
            || reply.lowercased().contains("no farrier")
        XCTAssertTrue(cited || honestNotFound, "Neither citation nor honest not-found: \(reply)")
        XCTAssertFalse(reply.lowercased().contains("http"), "Fabricated external source: \(reply)")
    }

    func testFollowUpUsesContextOrAsksClarification() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "Tell me about Thunder")
        _ = waitForReply(app)
        ask(app, "How old is she?")
        let labels = waitForReply(app, minCount: 2)
        let followUp = labels.last ?? ""

        XCTAssertGreaterThan(followUp.count, 20, "Follow-up reply suspiciously short: \(followUp)")
        XCTAssertFalse(
            followUp.contains("couldn't find anything about that"),
            "Follow-up lost prior context and fell back: \(followUp)"
        )
        XCTAssertFalse(
            followUp.contains("more to go on"),
            "Follow-up treated as too-short query: \(followUp)"
        )
    }

    func testStreamingReplyGrowsOverTime() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        ask(app, "What vaccinations does Thunder need this year?")
        let query = replyQuery(app)
        let reply = query.firstMatch
        XCTAssertTrue(reply.waitForExistence(timeout: 180), "Assistant reply never appeared")

        let atZero = reply.label
        Thread.sleep(forTimeInterval: 8)
        let atEight = reply.label

        XCTAssertTrue(
            atEight != atZero || atEight.count > 100,
            "Reply static and short after 8s - streaming appears dead: '\(atZero)' -> '\(atEight)'"
        )
    }

    func testLongQuestionDoesNotCrash() throws {
        let app = TestHelpers.launchApp()
        openAssistant(app)
        try requireAvailableModel(app)

        let longQuestion = "I am taking care of a seventeen year old mare named Bella who has "
            + "been showing mild colic symptoms since yesterday evening after a change in hay, "
            + "she is still drinking water but eating less than half her normal ration, what "
            + "should I check first and when should I call the vet out for an emergency visit?"
        XCTAssertGreaterThan(longQuestion.count, 200)

        ask(app, longQuestion)
        let labels = waitForReply(app)
        let reply = labels.last ?? ""

        XCTAssertGreaterThan(reply.count, 20, "Long question got empty/short reply: \(reply)")
        XCTAssertTrue(app.textFields["assistant_input"].exists, "App crashed or navigated away after long question")
    }
}
