import XCTest

/// Shared launch/query helpers for the Animally UI suites.
enum TestHelpers {
    /// Launches the app and waits for the patient list.
    @discardableResult
    static func launchApp(arguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += arguments
        app.launch()
        XCTAssertTrue(app.staticTexts["Patients"].waitForExistence(timeout: 15))
        return app
    }

    /// Returns the first patient row currently rendered by the app.
    ///
    /// UI tests intentionally do not depend on a particular backup/demo name:
    /// the app can be launched with user data already present, and the seed
    /// backup is not part of the production contract.
    static func firstPatientRow(_ app: XCUIApplication) -> XCUIElement {
        let predicate = NSPredicate(format: "identifier BEGINSWITH %@", "patient_row_")
        let row = app.buttons.matching(predicate).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "No patient row was rendered")
        return row
    }

    /// Returns the first rendered patient name from its accessibility label.
    static func firstPatientName(_ app: XCUIApplication) -> String {
        let label = firstPatientRow(app).label
        let prefix = "Patient "
        return label.hasPrefix(prefix) ? String(label.dropFirst(prefix.count)) : label
    }

    /// Opens the first patient currently rendered by the app.
    @discardableResult
    static func openPatientDetail(_ app: XCUIApplication) -> String {
        let row = firstPatientRow(app)
        let label = row.label
        let prefix = "Patient "
        let name = label.hasPrefix(prefix) ? String(label.dropFirst(prefix.count)) : label
        row.tap()
        XCTAssertTrue(app.buttons["Add record"].waitForExistence(timeout: 10))
        return name
    }

    /// Returns the first owner row currently rendered by the app.
    static func firstOwnerRow(_ app: XCUIApplication) -> XCUIElement {
        let predicate = NSPredicate(format: "identifier BEGINSWITH %@", "owner_row_")
        let row = app.buttons.matching(predicate).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "No owner row was rendered")
        return row
    }

    /// Returns the first rendered owner name from its accessibility label.
    static func firstOwnerName(_ app: XCUIApplication) -> String {
        let label = firstOwnerRow(app).label
        let prefix = "Owner "
        return label.hasPrefix(prefix) ? String(label.dropFirst(prefix.count)) : label
    }

    /// Deterministic-per-run suffix so repeated runs never collide on names.
    static func uniqueSuffix() -> String {
        String(UUID().uuidString.prefix(6))
    }

    /// Finds a SwiftUI form input regardless of whether the current Xcode SDK
    /// exposes a single-line field as a TextField or a vertical field as a
    /// TextView. Matching all descendants avoids the iOS 26 automation-type
    /// mismatch between those two representations.
    static func formField(_ app: XCUIApplication, label: String) -> XCUIElement {
        let predicate = NSPredicate(
            format: "label == %@ OR identifier == %@ OR placeholderValue == %@",
            label,
            label,
            label
        )
        // Prefer actual editable controls. A broad `.any` query can otherwise
        // return a read-only FieldCell label still present in the navigation
        // stack before it reaches the frontmost editor's input control.
        let textField = app.textFields.matching(predicate).firstMatch
        if textField.exists { return textField }

        let textView = app.textViews.matching(predicate).firstMatch
        if textView.exists { return textView }

        return app.descendants(matching: .any).matching(predicate).firstMatch
    }
}

/// Base class providing consistent launch behavior.
class AnimallyTestCase: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }
}

extension TestHelpers {
    /// Enters text into a live-bound form field and retries when a SwiftUI/KMP
    /// state round-trip drops a keystroke. This is intentionally separate from
    /// search typing because form fields are expected to start blank and do
    /// not expose the searchable-field clear affordance.
    static func typeTextAndVerify(
        _ field: XCUIElement,
        text: String,
    ) {
        var attempts = 0
        while attempts < 3 {
            field.tap()
            usleep(300_000)

            let existing = (field.value as? String) ?? ""
            if !existing.isEmpty {
                field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: existing.count + 2))
                usleep(200_000)
            }

            field.typeText(text)
            usleep(400_000)
            if (field.value as? String) == text {
                return
            }
            attempts += 1
        }

        XCTFail("Field never received '\(text)'; landed '\(field.value ?? "nil")'")
    }

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

    /// Types an assistant question in one accessibility transaction.
    ///
    /// Assistant input is a normal SwiftUI TextField, not a searchable field.
    /// Its state is published while the assistant screen is streaming, so
    /// reading `value` after every character can block XCTest's accessibility
    /// snapshot while the app is otherwise responsive. The send action and the
    /// resulting assistant bubble provide the meaningful end-to-end assertion.
    static func typeAssistantQuestion(
        _ field: XCUIElement,
        text: String,
    ) {
        field.tap()
        usleep(300_000) // let focus settle - first keystroke otherwise drops
        field.typeText(text)
        usleep(500_000) // let the draft binding settle before tapping Send
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
