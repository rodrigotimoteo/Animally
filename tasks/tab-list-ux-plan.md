# Patient-tab list UX plan

## Objective

Keep patient-detail tabs readable on iPhone-sized screens while making records easy to find.

## Acceptance criteria

- Every patient-detail record list shows at most five records by default when more exist.
- A clearly labelled action expands and collapses the full list.
- Each list has a search button that reveals an inline search field.
- Search runs in shared Kotlin state, matches meaningful record fields, and reveals all matches.
- Closing search clears the query without changing persisted data.
- Existing add, edit, loading, error, and empty-state behavior remains intact.

## Verification

- Unit-test the reusable filtering/collapse rules.
- Add shared ViewModel coverage for query and expansion behavior.
- Exercise the patient-detail UI on the desktop Compose test target.
- Compile the shared JVM/desktop and iOS simulator targets, then inspect the iOS simulator build.
