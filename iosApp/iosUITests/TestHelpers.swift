import XCTest

/// Shared launch/query helpers for the Animally UI suites.
enum TestHelpers {
    /// Launches the app and waits for the patient list.
    @discardableResult
    static func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 15))
        return app
    }

    /// Opens the patient detail for Thunder (seed data present on the sim).
    static func openThunderDetail(_ app: XCUIApplication) {
        app.buttons["Patient Thunder"].firstMatch.tap()
        XCTAssertTrue(app.buttons["Add record"].waitForExistence(timeout: 10))
    }

    /// Deterministic-per-run suffix so repeated runs never collide on names.
    static func uniqueSuffix() -> String {
        String(UUID().uuidString.prefix(6))
    }
}

/// Base class providing consistent launch behavior.
class AnimallyTestCase: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }
}

extension TestHelpers {
    /// Types text into a search field, verifying what actually landed.
    /// XCUITest's typeText drops characters in `.searchable` fields while the
    /// binding re-renders, so typing happens in small chunks with settle
    /// pauses, followed by a clear-and-retype pass if anything was mangled.
    static func typeSearchText(
        _ app: XCUIApplication,
        field: XCUIElement,
        text: String,
    ) {
        let clearButton = app.buttons["Clear text"].firstMatch
        field.tap()
        typeInChunks(field, text)

        // The searchable field's live binding re-renders mid-typing and can
        // swallow keystrokes; verify what landed and retry up to 3 times.
        var attempts = 0
        while (field.value as? String) != text, attempts < 3 {
            attempts += 1
            if clearButton.exists {
                clearButton.tap()
            }
            field.tap()
            typeInChunks(field, text)
        }
    }

    private static func typeInChunks(_ field: XCUIElement, _ text: String) {
        var start = text.startIndex
        while start < text.endIndex {
            let end = text.index(start, offsetBy: 3, limitedBy: text.endIndex) ?? text.endIndex
            field.typeText(String(text[start..<end]))
            usleep(150_000)
            start = end
        }
    }
}
