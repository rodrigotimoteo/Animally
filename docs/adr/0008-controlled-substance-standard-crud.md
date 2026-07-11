# ADR 0008: Controlled Substance Log — Standard CRUD for Now

**Date**: 2026-07-11
**Status**: proposed

## Context
Controlled substances (sedatives, opioids) may have regulatory tracking requirements (tamper-proof records, audit trails). App is currently personal use for single vet. May be shared with third parties later.

## Decision
Treat ControlledSubstance as standard CRUD entity (create, edit, delete) for now. No append-only enforcement, no separate audit log. Revisit if app is shared with third parties or regulatory requirements emerge.

## Alternatives considered
1. Append-only — entries can never be edited/deleted. Audit trail integrity but inflexible.
2. Edit with audit log — separate AuditLog table tracking all changes. More flexible but complex.
3. Append-only + correction entries — never delete, add correction entries. Medical record standard but complex UI.

## Consequences
Easier: simple implementation, standard CRUD patterns, no audit infrastructure.
Harder: no tamper-proof guarantee, regulatory non-compliance risk if shared, will need migration if requirements change. Risk: acceptable for personal use, must revisit before third-party sharing.
