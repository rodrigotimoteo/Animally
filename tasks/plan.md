# Implementation Plan: Cloud assistant flexibility and stream normalization

## Overview

Make cloud-backed assistant turns feel natural for general and casual questions while keeping Apple Foundation Models on the existing strict, record-grounded path. Harden the cloud SSE parser so common reasoning fields and inline thinking blocks never reach the UI, including when tags are split across streamed chunks.

## Architecture Decisions

- Resolve a query policy before retrieval: Foundation Models keep the 4096-token, record-grounded policy; a cloud fallback receives a larger context budget and may answer general questions when the records do not contain the answer.
- Keep safety-critical dosage handling and deterministic patient-record answers unchanged for both engines.
- Normalize reasoning at the cloud transport boundary. Ignore structured reasoning fields and strip common inline thinking tags with state carried across chunks.
- Treat explicit protocol completion markers as authoritative. A bare EOF without a completion signal remains an interruption so a genuinely dropped stream is not silently presented as complete.

## Task List

### Phase 1: Stream contract

- [x] Add stateful filtering for structured and inline reasoning output.
- [x] Recognize standard and provider-specific terminal frames and cover split-tag cases with focused tests.

### Phase 2: Cloud query policy

- [x] Add an explicit strict/on-device versus flexible/cloud policy seam.
- [x] Relax only ungrounded-question gating and context limits for cloud fallback turns.
- [x] Add a cloud-specific system-prompt branch that distinguishes general answers from patient-record facts.

### Checkpoint: Core behavior

- [x] Focused parser and RAG tests pass.
- [x] Existing Foundation Models guardrail tests remain unchanged and pass.

### Phase 3: iOS verification

- [x] Compile the iOS simulator target.
- [x] Run focused simulator UI coverage for assistant/settings behavior.

### Checkpoint: Complete

- [x] Static analysis passes.
- [x] Working tree is clean after the requested commit.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Providers use different reasoning encodings | Reasoning leaks into the answer | Handle structured fields, common XML-like tags, and split tags; keep parser tolerant of unknown fields |
| Cloud context limits vary by model | Request can exceed a provider limit | Use a bounded cloud budget rather than the unbounded model maximum |
| Relaxing grounding could allow invented patient facts | High | Keep the cloud prompt explicit about record claims, retain citations when records are used, and keep dosage safety gates |
| EOF is ambiguous at the HTTP layer | Dropped answers could look complete | Require a terminal marker; surface the interruption and provider error |

## Open Questions

- An authenticated live provider request is still useful for confirming the exact terminal frame used by the selected OpenCode Go model; no API key is stored in the repository.
