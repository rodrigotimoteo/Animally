# Context

Glossary of domain terms used across Animally. Updated as terms resolve during grilling.

## Patient
A horse (equine) under veterinary care. Central entity — all medical records link to a Patient.

## Owner
Person who owns one or more Patients. Supports multi-animal owners (breeding farms, stables).

## Anamnese
1:1 record with Patient containing general medical history, chronic conditions, and allergies. Distinct from consultation-specific SOAP notes.

## Weight Entry
Historical weight measurement for a Patient. Tracked over time, not as a single value on Patient.

## Coggins
Equine Infectious Anemia test. Tracked on Patient: test date, result, expiry date. Expiry triggers reminder.

## UELN
Unique Equine Life Number. 15-digit ISO-standard identifier linked to microchip. Distinct from microchip ID and studbook registration number.

## Registration Number
Studbook or federation registration number. Separate identifier from UELN and microchip.

## SOAP Notes
Structured clinical documentation per consultation: Subjective (owner's description), Objective (exam findings), Assessment (diagnosis), Plan (treatment).

## Consultation
A veterinary visit documented via SOAP notes. One consultation = one visit.

## Vaccination
Record of vaccine administered with next-due-date calculation. Separate from Deworming.

## Deworming
Record of anthelmintic treatment. Separate entity from Vaccination — different drug class, different reminder cadence.

## Dentistry
Dental check and treatment (e.g., floating). Tracked on 3/6/9/12 month schedule.

## Lameness Evaluation
Structured assessment of gait abnormality. AAEP 1-5 grade, limb location, flexion test results.

## Controlled Substance
Drug subject to regulatory tracking (e.g., sedatives, opioids). Logged with dose, administering vet, and witness.

## Reproduction Event
Breeding-cycle event: Heat, Breeding, Pregnancy Check, Foaling.

## Ultrasound (Reproductive)
Reproductive ultrasound with structured fields: ovary status, uterine status, follicle size (mm). Distinct from diagnostic imaging.

## Gestation
Pregnancy tracking from breeding date. Includes expected due date, gestation day count (computed), status.

## Farrier Visit
Hoof care visit: trim, shoeing type, shoe application. Next-due tracking.

## FTS5
SQLite Full-Text Search version 5. Used for global search across patient records.

## Offline-First
Architecture where local storage is primary. All operations work without network. Cloud sync is secondary, deferred to Phase 6.

## Tabbed PatientDetail
Patient detail screen organized into top-level tabs: Overview, Medical, Preventive, Reproduction, Diagnostics/Files. Replaces flat scrollable list.

## Internal Storage
App-private file storage (`Context.filesDir` on Android, `NSDocumentDirectory` on iOS). Not user-accessible. Survives app updates, lost on uninstall. Backup/restore handles data safety.