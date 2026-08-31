# Animally Feature Ideas

This is a product-idea backlog, not a promise that every item will be built. Ideas remain here until they are promoted into a scoped implementation plan with explicit definitions, privacy boundaries, acceptance criteria, and verification.

## Planned

### Internship Insights dashboard

Turn existing records into an auditable overview of workload, case mix, reproduction activity, current gestations, due-soon care, and research readiness. Dashboard calculations remain deterministic in shared Kotlin, and every metric links back to its source records.

The implementation specification is maintained in [`tasks/plan.md`](../tasks/plan.md).

## High-value candidates

### Internship sessions and hours

Record placement sessions explicitly: date, start/end time, location, supervisor, role, procedures observed or performed, competencies, and reflection. This would let Animally report official placement hours instead of estimating work from clinical records.

### Competency portfolio

Track progress by procedure or competency, distinguishing observed, assisted, and independently performed work. Each entry should link to evidence and optionally support supervisor review or sign-off.

### Saved thesis cohorts

Save reusable inclusion and exclusion filters, freeze a cohort at a point in time, and record later changes. A frozen cohort prevents new or edited records from silently changing previously reported results.

### Reproduction cycle and outcome linkage

Link breeding attempts, pregnancy checks, gestations, embryo recipients, and foaling outcomes. This is the prerequisite for defensible conception, embryo-transfer, and live-foal rates; the application must not infer those links from dates or prose.

### Reproduction pipeline

Provide an operational view grouping mares into monitoring, ready to breed, bred and awaiting check, pregnant, due soon, and follow-up-needed stages. Every stage should be derived from explicit structured records and allow source drill-down.

### Thesis study workspace

Offer cohort comparison, variable selection, missingness summaries, long/wide CSV exports, a data dictionary, and reproducible analysis snapshots. Keep statistical output descriptive unless the selected method and assumptions are explicit.

### Supervisor report

Generate a monthly PDF containing placement hours, case mix, competency progress, selected reflections, and source evidence for supervisor review or sign-off.

### Follow-up and review inbox

Bring due gestation checks, expiring preventive care, incomplete records, failed dictation extraction, unsourced assistant output, and research-readiness issues into one actionable queue.

### Privacy-aware geographic caseload

Use owner/stable locations for regional caseload and travel-day summaries while avoiding disclosure of exact private addresses in charts or exports.

### Assistant explanation mode for insights

Let the assistant explain a frozen, deterministic dashboard snapshot. The model may paraphrase or contextualise supplied metrics, but it must cite the metric/source records and must never recompute totals or invent clinical conclusions.

## Additional candidates

### Record templates and favourites

Reusable consultation, treatment, reproduction, and follow-up templates could reduce repetitive entry while preserving an explicit review step before saving.

### Backup health

Show the last successful backup, media coverage, CloudKit state, and a prominent warning when irreplaceable local records are not protected.

### Data-quality audit

List concrete issues such as missing identifiers, owner assignments, breeding dates, structured outcomes, or patient links. Prefer issue counts and fix actions over a vague aggregate score.

### Repro calendar

Combine checks, medication, breeding windows, expected foaling dates, and follow-up reminders into a calendar whose entries deep-link to the patient and source record.

## Promotion criteria

Before moving an idea into implementation:

1. Identify the user decision or workflow it improves.
2. Define every calculated value and its denominator.
3. Separate existing-data functionality from schema changes.
4. Specify privacy and export behaviour.
5. Keep business logic in shared Kotlin and platform integrations at platform boundaries.
6. Add a vertical task plan with focused tests and device/simulator verification.
