# Implementation Plan: Read-only cloud analysis tools

## Overview

Add a bounded, read-only tool-calling path for cloud assistant turns. Cloud
models may request typed analysis operations, while shared Kotlin validates
arguments, reads repositories, computes bounded summaries, and returns
structured results. Swift remains presentation-only.

## Architecture decisions

- Keep the existing text-only `RagLlmEngine` contract for local/Foundation
  Models and simple answers.
- Add an optional shared tool-calling seam implemented by the cloud engine and
  selected by the existing fallback router.
- Expose only read-only analysis tools initially: patient census, weight
  summaries, care summaries, and gestation summaries.
- Keep arithmetic, filtering, patient matching, date validation, row limits,
  and cancellation in Kotlin.
- Bound each turn to a small number of tool rounds and never expose database
  credentials, SQL, mutation operations, or an arbitrary code interpreter.

## Acceptance criteria

- Native OpenAI-compatible `tools` requests are serialized correctly.
- Streaming tool-call fragments are accumulated and returned as typed calls;
  hidden reasoning remains excluded from visible output.
- Tool arguments are validated and malformed/unknown calls fail safely.
- A cloud-capable analysis question can execute a tool, continue the same
  conversation, and produce a normal cited assistant answer.
- Existing local/simple RAG behavior and cloud text-only behavior remain intact.
- Focused common and host tests pass, followed by shared compilation and static
  checks.

## Main risks

| Risk | Mitigation |
| --- | --- |
| Provider accepts chat completions but not tools | Keep tool path capability-gated and fall back to normal RAG with a clear result |
| Model requests unbounded or invalid data | Strict schemas, argument limits, result caps, and a bounded tool loop |
| Tool output leaks unnecessary personal data | Return only analysis fields; never owners, credentials, or raw SQL |
| Tool calls break existing SSE handling | Add parser contract tests for fragmented ids/names/arguments and terminal frames |
