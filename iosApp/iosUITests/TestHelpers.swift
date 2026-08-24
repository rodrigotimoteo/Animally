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
    ///
    /// The `.searchable` field's text travels through an async KMP round trip
    /// on every keystroke (`set` -> store -> StateFlow -> `Task { @MainActor }`
    /// -> re-render). When the query change flips the body subtree
    /// (empty/no-results/results), the field churns and exactly one synthesized
    /// keystroke gets swallowed. Whole-pass typing loses the same character on
    /// every retry, so clear-and-retype loops never converge; instead each
    /// keystroke is verified immediately and healed (delete + retype) before
    /// the next one is sent.
    static func typeSearchText(
        _ app: XCUIApplication,
        field: XCUIElement,
        text: String,
    ) {
        let clearButton = app.buttons["Clear text"].firstMatch
        field.tap()
        usleep(300_000) // let focus settle - first keystroke otherwise drops
        typeAndHeal(field, text)

        var attempts = 0
        while (field.value as? String) != text, attempts < 2 {
            attempts += 1
            if clearButton.exists {
                clearButton.tap()
            }
            field.tap()
            usleep(300_000)
            typeAndHeal(field, text)
        }
        // Never give up silently: a mangled search string makes the test fail
        // later with a confusing "not found" instead of pointing here.
        if (field.value as? String) != text {
            XCTFail("Search field never received '\(text)'; landed '\(field.value ?? "nil")' after \(attempts) retries")
        }
    }

    /// Types one character at a time; after each keystroke, compares the
    /// field value with the expected prefix and repairs any divergence
    /// (swallowed or altered characters) before continuing.
    private static func typeAndHeal(_ field: XCUIElement, _ text: String) {
        var typed = ""
        for ch in text {
            field.typeText(String(ch))
            typed.append(ch)
            usleep(200_000) // let query -> store -> MainActor -> re-render settle

            var value = (field.value as? String) ?? ""
            if value == typed { continue }

            // Keep the longest common prefix, delete anything after it,
            // then retype what should follow.
            let common = zip(value, typed).prefix(while: ==).count
            let extra = value.count - common
            if extra > 0 {
                field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: extra))
            }
            let missing = String(typed.dropFirst(common))
            if !missing.isEmpty {
                field.typeText(missing)
            }
            usleep(200_000)
            value = (field.value as? String) ?? ""
            if value != typed {
                // Diverged beyond simple repair; restart from clean state.
                field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: value.count + 5))
                typed = ""
            }
        }
    }
}
