# ADR 0006: Tabbed PatientDetail Over Flat List

**Date**: 2026-07-11
**Status**: proposed

## Context
PatientDetail aggregates 18+ entity types (Consultations, Vaccinations, Deworming, Dentistry, Lameness, Surgeries, Medications, Lab Results, Imaging, Farrier, Reproduction Events, Ultrasounds, Gestation, Repro Meds, Controlled Substances, Anamnese, Weight History, Files). Flat scrollable list would be 18+ sections — excessive scrolling, poor discoverability.

## Decision
Organize PatientDetail into 5 top-level tabs: Overview (Profile, Anamnese, Weight History), Medical (Consultations, Surgeries, Lameness, Medications, Controlled Substances), Preventive (Vaccinations, Deworming, Dentistry, Coggins, Farrier), Reproduction (Events, Ultrasound, Gestation, Repro Meds), Diagnostics/Files (Lab Results, Imaging, Files, Export).

## Alternatives considered
1. Flat scrollable list — simple, consistent, but 18+ items = poor UX.
2. Grouped sections with headers — better than flat but still long scroll.
3. Bottom navigation — limited to 5 items, doesn't fit domain grouping.

## Consequences
Easier: better UX with many entity types, domain-aligned grouping, reduced scroll.
Harder: tab state management, more complex screen, each tab needs its own lazy list. Reversible — UI change only.
