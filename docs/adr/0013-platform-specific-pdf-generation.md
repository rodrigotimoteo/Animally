# ADR 0013: Platform-Specific PDF Generation via Expect/Actual

**Date**: 2026-07-11
**Status**: proposed

## Context
Phase 3 requires PDF patient report export. iText/OpenPDF are JVM-oriented, no KMP/iOS support. Need PDF generation on both Android and iOS.

## Decision
expect/actual PDF generator. Android: iText or OpenPDF (JVM). iOS: PDFKit (native). Common interface defines generatePatientReport(patient, records): ByteArray. Platform implementations handle rendering.

## Alternatives considered
1. KMP PDF library — if one existed, single codebase, but no mature KMP PDF lib found.
2. HTML-to-PDF — generate HTML in commonMain, platform WebView renders to PDF. No PDF lib but less layout control, WebView overhead.
3. Server-side PDF — requires network, conflicts with offline-first.

## Consequences
Easier: full platform control, native PDF quality, no cross-platform lib risk.
Harder: two implementations, platform-specific layout differences, more code.
