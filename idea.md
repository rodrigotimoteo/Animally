# Animally — Equine Medical Journal

A phone app for equine veterinarians to manage patient records on the go. Everything stored locally on the phone — no internet required, no cloud dependency, no waiting for pages to load between barn calls.

## What it does

**Patient records** — Keep a profile for every horse: name, microchip, breed, owner, weight history, stable location, Coggins status. Quick access to all the basics in one place.

**Medical history** — Log every vet visit with structured SOAP notes (Subjective, Objective, Assessment, Plan). Track chronic conditions, allergies, and general history over time.

**Vaccinations & deworming** — Record every vaccine and deworming treatment. The app calculates when the next one is due and sends a reminder before it expires.

**Dentistry** — Track dental checks on a 3/6/9/12 month schedule. Never miss a routine float.

**Lameness evaluations** — Grade lameness (1–5), log the location, record flexion test results. Track how a horse improves or worsens over time.

**Surgery & procedures** — Log every surgery with date, procedure details, and notes.

**Medication tracking** — Log every drug administered with dose, date, and witness (for controlled substances). Full audit trail.

**Lab results & imaging** — Attach bloodwork results, X-rays, ultrasound images, and any other files directly to a horse's record.

**Farrier visits** — Log hoof care, shoeing type, and trim schedule.

**Reproduction** — Full breeding management: insemination dates with auto-calculated follow-up visits, ultrasound tracking (ovary status, uterine status, follicle size), gestation monitoring with expected due date and day count, reproductive medication logging. Everything needed to manage broodmares from cycle to foaling.

**Search** — Find any horse, condition, treatment, or diagnosis instantly. Search across all records, not just names.

**Export** — Generate a full PDF report of a horse's history for referrals, second opinions, or personal records. Export to CSV for spreadsheet analysis or feeding data to AI tools for trend analysis.

**Backup** — Full local backup and restore. Never lose a record.

**Reminders** — Automatic alerts for vaccination due dates, dental checks, Coggins expiry, and any custom follow-ups you set.

## Why this exists

Most equine practice management software is cloud-based, expensive, and built for large clinics. This is a simple, offline-first tool that works in the field — in the barn, at a show, anywhere with or without internet. One vet, one phone, all records.

---

## Data Model

```mermaid
erDiagram
    OWNER ||--o{ PATIENT : has
    PATIENT ||--o| ANAMNESE : has
    PATIENT ||--o{ VACCINATION : receives
    PATIENT ||--o{ DENTISTRY : has
    PATIENT ||--o{ CONSULTATION : has
    PATIENT ||--o{ SURGERY : has
    PATIENT ||--o{ MEDICATION_LOG : receives
    PATIENT ||--o{ LAB_RESULT : has
    PATIENT ||--o{ IMAGING : has
    PATIENT ||--o{ LAMENESS_EVAL : has
    PATIENT ||--o{ REPRODUCTION : has
    PATIENT ||--o{ REPRODUCTION_MED : receives
    PATIENT ||--o{ ULTRASOUND : has
    PATIENT ||--o{ GESTATION : has
    PATIENT ||--o{ CONTROLLED_SUBSTANCE : receives

    OWNER {
        long id PK
        string name
        string phone
        string address
    }

    PATIENT {
        long id PK
        long owner_id FK
        string name
        string microchip
        stringueln
        string breed
        int age
        string sex
        string color
        float current_weight
        string stable_location
        date coggins_test_date
        string coggins_result
        date coggins_expiry
    }

    ANAMNESE {
        long id PK
        long patient_id FK
        text general_history
        text chronic_conditions
        text allergies
    }

    VACCINATION {
        long id PK
        long patient_id FK
        string drug_name
        date date_given
        date next_reminder
    }

    DENTISTRY {
        long id PK
        long patient_id FK
        string condition
        string interval
        date next_due
    }

    CONSULTATION {
        long id PK
        long patient_id FK
        date date
        text subjective
        text objective
        text assessment
        text plan
    }

    SURGERY {
        long id PK
        long patient_id FK
        date date
        string procedure
        text notes
    }

    MEDICATION_LOG {
        long id PK
        long patient_id FK
        string drug_name
        float dose
        string unit
        date date_administered
    }

    LAB_RESULT {
        long id PK
        long patient_id FK
        date date
        string test_type
        text results
        string file_path
    }

    IMAGING {
        long id PK
        long patient_id FK
        date date
        string type
        string file_path
        text notes
    }

    LAMENESS_EVAL {
        long id PK
        long patient_id FK
        date date
        int grade
        string location
        text flexion_tests
    }

    REPRODUCTION {
        long id PK
        long patient_id FK
        string event_type
        date date
        text details
    }

    REPRODUCTION_MED {
        long id PK
        long patient_id FK
        string drug_name
        float dose
        date date_given
        text notes
    }

    ULTRASOUND {
        long id PK
        long patient_id FK
        date date
        string ovary_status
        string uterine_status
        float follicle_size
        text notes
        string file_path
    }

    GESTATION {
        long id PK
        long patient_id FK
        date insemination_date
        date expected_due_date
        int gestation_days
        text notes
    }

    CONTROLLED_SUBSTANCE {
        long id PK
        long patient_id FK
        string drug_name
        float dose
        date date_administered
        string witness
    }
```

# Phase 1: Local Data Models & CRUD
## Establish offline-first storage and basic patient management.
### Phase 1a: Core Workflow (Patient + Owner + Anamnese + Consultation + Vaccination)
Prove architecture end-to-end with core entities. Deliverable + testable.
### Step 1.1: Initialize KMP Project
Set up Compose Multiplatform project. Configure commonMain for shared logic, androidMain/iosMain as targets.
### Step 1.2: Define Data Models + SQLDelight Schema
Create tables: Patient, Owner, Anamnese, WeightEntry, Consultation, Vaccination, Deworming, Dentistry, Surgery, MedicationLog, LabResult, Imaging, LamenessEvaluation, FarrierVisit, ReproductionEvent, Ultrasound, Gestation, ReproMed, ControlledSubstance.
Owner = separate entity (name, phone, address, linked horses). Supports multi-animal owners.
Weight = tracked as WeightEntry history, not single value on Patient.
Anamnese = separate 1:1 entity (General History + Chronic Conditions + Allergies).
SOAP notes = structured clinical documentation per consultation.
Coggins status = tracked on Patient record (test date, result, expiry).
Deworming = separate entity from Vaccination (different drug class, different reminder cadence).
Ultrasound = reproductive-specific (ovary status, uterine status, follicle size mm).
Gestation = includes gestationDays computed field (days since breedingDate, ~340 standard).
Controlled substance tracking = log all controlled drug admin with date, dose, witness.
Patient identifiers: microchipId, ueln (15-digit ISO), registrationNumber (studbook).
Patient soft delete via isActive flag — never hard-delete, preserve medical history.
### Step 1.3: Build Core CRUD Screens (Phase 1a)
Patient List → Patient Detail (tabbed: Overview, Medical, Preventive, Reproduction, Diagnostics/Files) → Add/Edit forms for each record type. Functional before pretty.
Owner management screen — view all horses under one owner.
### Phase 1b: Remaining Entities
WeightEntry, Deworming, Dentistry, LamenessEvaluation, Surgery, MedicationLog, LabResult, Imaging, FarrierVisit, ReproductionEvent, Ultrasound, Gestation, ReproMed, ControlledSubstance.
Mechanical pattern-following using architecture proven in 1a.
# Phase 2: Search & File Attachments (Week 2)
## Make records findable and attach media locally.
### Step 2.1: Full-Text Search via SQLDelight FTS5
Index patient names, diagnoses, treatments, notes. Filter by date range and record type.
### Step 2.2: Local File Attachments
Store photos, PDFs, X-rays in app's local storage. Link files to patient records. Thumbnail generation for images.
### Step 2.3: Farrier Visit Logging
Track farrier visits, hoof condition, shoeing type, trim schedule. Link to patient timeline.
# Phase 3: Export & Backup (Week 3)
## Enable patient history export and data safety.
### Step 3.1: PDF Patient Report Export
Generate complete patient history as PDF — demographics, all records, attached file references. Date-range filtering.
### Step 3.2: CSV Export for Analysis
Export patient records as CSV for spreadsheet/LLM analysis.
### Step 3.3: Backup/Restore
Full local backup to device storage. Import from backup. Foundation for later iCloud/Google Drive integration.
# Phase 4: Reminders & Notifications (Week 4)
## Proactive care alerts.
### Step 4.1: Vaccination & Dentistry Reminders
Auto-calculate next due dates from records. Schedule local notifications.
### Step 4.2: Coggins Expiry Alerts
Track Coggins test expiry. Alert before test expires. Flag horses with overdue tests.
### Step 4.3: Custom Reminder Support
User-defined reminders for any record type (follow-ups, re-checks, farrier visits, breeding cycles).
# Phase 5: Visual Polish (Week 5, Optional)
## UI/UX refinement after core function works.
### Step 5.1: Haze Glass Effects
Add hardware blur via Haze library. GlassScaffold wrapper. Liquid glass on key screens.
### Step 5.2: Timeline & Dashboard
Unified chronological timeline feed. Dashboard with upcoming alerts and recent activity.
# Phase 6: Postgres Backend & Cloud Sync (Week 6+, Future)
## Server component for multi-device sync and web access. CV keyword: Postgres.
### Step 6.1: Ktor + Postgres Backend
Set up Ktor server with Postgres database. Expose REST API for patient CRUD.
### Step 6.2: Sync Protocol
Local-first with sync: client pushes SQLDelight mutations to server when online. Conflict resolution via ID maps.
### Step 6.3: iCloud/Google Drive Backup
Replace custom sync with platform-native cloud backup if simpler.
# Future: LLM Analysis
## Analyze patient history paths with LLM.
### Future Step: Export → LLM Pipeline
Feed exported patient history (PDF/CSV) to LLM for treatment path analysis, outcome prediction, or trend detection.
