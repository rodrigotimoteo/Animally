# Assistant and dictation retention checklist

- [x] Add `AssistantChatHistory` and `DictationCapture` SQLDelight schemas plus
      migration 10.
- [x] Add shared domain models/repositories and data mappers.
- [x] Bind repositories and queries in Koin without adding Swift database logic.
- [x] Hydrate and persist the assistant’s bounded 15-turn history.
- [x] Persist dictation transcript/audio metadata at recording stop.
- [x] Add iOS audio-file recording/playback bridge with safe missing-file
      behavior.
- [x] Add assistant history and dictation archive UI with search and playback.
- [x] Add retention, repository, ViewModel, and UI coverage.
- [x] Run formatting, lint/detekt, focused Gradle tests, iOS build, simulator
      UI tests, and review the diff.
- [x] Commit the completed feature and report the commit hash and any platform
      limitations.
