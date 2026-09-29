# Certificate Service — Design Document

## 1. Tables and what is copied vs read live

| Table | Purpose |
|-------|---------|
| `people` | People who can receive certificates |
| `programmes` | Training programmes (name, description, active) |
| `designs` | Named design content (opaque, never rendered) |
| `programme_design_versions` | Versioned programme→design links with `effective_from`/`effective_to` |
| `tracks` | A programme made of other programmes |
| `track_programmes` | Membership of a programme in a track, with `position`; `UNIQUE(track_id, programme_id)` |
| `programme_rules` | One eligibility rule per programme (`rule_type`, `threshold`) |
| `certificates` | Issued certificates, all display values stored as snapshots |
| `bulk_certificate_jobs` / `bulk_certificate_job_items` | Bulk-issue bookkeeping |

**Copied onto the certificate at issue time (immutable):** person name, programme name,
programme description, design name, design content, track name, route, issued-at, and the
`programme_design_version_id` that was applied. These live in the `*_snapshot` columns.

**Read live:** nothing that appears on the certificate. Only `status`, `cancellation_reason`
and `cancelled_at` change after issue, and only through the explicit cancel operation.

**Decision — person name is snapshotted.** A certificate is a historical record of what was
true on the day. If Priya changes her name next year, the old certificate an employer already
saw must not silently change. New behaviour would come from issuing a new certificate.

## 2. Walkthrough: 1 March → 20 March

- **1 Mar** — `programmes`: Advanced SQL. `designs`: Gold Border v1. `programme_design_versions`:
  (v1, design=Gold Border v1, effective_from=1 Mar, effective_to=NULL).
- **5 Mar** — Priya finishes. One row in `certificates` with snapshots
  {Priya Sharma, Advanced SQL, Gold Border v1, its content, issued_at=5 Mar} and
  `programme_design_version_id` = v1.
- **10 Mar** — programme renamed → only the `programmes.name` row changes. Certificate untouched.
- **12 Mar** — Gold Border v2 attached → v1 row gets `effective_to=12 Mar`; new row v2
  (effective_from=12 Mar, effective_to=NULL). Certificate untouched.
- **15 Mar** — programme disabled → `programmes.active=false`. Certificate untouched.
- **20 Mar** — lookup reads only the certificate's snapshot columns → identical to 5 Mar.

## 3. Stopping two certificates for the same route

A partial unique index:

```sql
CREATE UNIQUE INDEX uq_certificate_live_route
  ON certificates (person_id, programme_id, route_type, route_key)
  WHERE status = 'LIVE';
```

`route_type`/`route_key` are a **normalized route key** so nullable `track_id` never weakens the
constraint. For a track, `route_key` = the track UUID as text. For "on its own", `route_type=OWN`
and `route_key='OWN'` — an explicit literal, never NULL. This is the awkward case the brief warns
about: because `NULL != NULL` in SQL, a plain `UNIQUE(person, programme, track_id)` would silently
allow unlimited "on its own" duplicates. Using a literal `'OWN'` closes that hole, and it is tested
directly (`CertificateUniquenessIT.secondCertificateOnItsOwnWithNoTrackIsRefused`).

Cancelled rows are excluded from the index, so a certificate can be re-issued after cancellation.

## 4. Two requests in the same millisecond

Each issue runs in one `@Transactional` unit. Before insert there is a fast `exists` check (nice
error for the common case), but that is **not** the guarantee. The guarantee is the unique index
above plus `saveAndFlush`: the losing thread's insert throws `DataIntegrityViolationException`,
which is caught and turned into a `409 duplicate_certificate`. Exactly one row, one id.
Proven by `CertificateConcurrencyIT` firing 16 threads through a `CountDownLatch`.

## 5. "Which design on 5 March?"

`programme_design_versions` keeps every link with effective dates. The query is:

```sql
effective_from <= :at AND (effective_to IS NULL OR effective_to > :at)
```

At issue time we resolve this and store the resulting design as a snapshot **and** keep the
`programme_design_version_id`, so the answer is durable even after the design changes again.

## 6. Eligibility rules and adding a fourth kind

Rules are rows in `programme_rules` (`rule_type` + `threshold`). Evaluation uses a strategy
pattern: `EligibilityRuleEvaluator` with one implementation per kind
(`AnyoneRuleEvaluator`, `MinimumScoreRuleEvaluator`, `AllTasksDoneRuleEvaluator`), resolved by
`EligibilityRuleEvaluatorRegistry`. Changing 70→65 is a `PUT /programmes/{id}/rule` call — no
restart (proven by `RuleEvaluationIT.changingThresholdThroughApiTakesEffectWithoutRestart`).
**Adding a fourth kind:** add an enum value and one `EligibilityRuleEvaluator` bean. No existing
code changes — Spring auto-registers it.

## 7. Bulk issuing — decision and crash behaviour

**Decision: many small transactions, not one big one.** Each person is issued in its own
transaction. Successes commit immediately and independently. If the process dies after 300 of 500,
those 300 certificates are durable; re-running the same request simply skips them as duplicates
(the route uniqueness makes bulk idempotent). The response lists each person with a status
(`ISSUED`, `SKIPPED_DUPLICATE`, `FAILED_ELIGIBILITY`, `FAILED_VALIDATION`) and a reason.
Rejected alternative: one transaction — a single bad row would roll back all 500, and a crash
would lose everything.

## 8. 5 million certificates, 10,000 lookups/minute

- Lookups by `public_code` hit a unique index → O(log n), fine at 5M rows.
- Person listing uses `idx_certificates_person_issued (person_id, issued_at DESC)` and is paginated.
- Snapshots mean lookups touch a single table — no joins to live programme/design data.
- What would strain: bulk issue is synchronous and single-threaded per request; at very large
  batches it should become an async job with a status endpoint (job tables already exist).
  Read replicas and a cache in front of public lookups would absorb heavy verify traffic.

## 9. Not finished / would change before real certificates

- **AuthN/AuthZ** — admin vs public endpoints are not secured; add OAuth2/JWT and lock down
  create/disable/cancel and the `/dev/seed` endpoint (remove it in prod).
- **Async bulk** — move large bulk jobs off the request thread.
- **Rate limiting** and abuse protection on the public lookup.
- **Idempotency keys** on issue for at-most-once client retries.
- **Observability** — tracing and richer metrics; structured logs are in place.
- **Optimistic locking** is present on mutable aggregates; certificates are effectively
  append-only by design.
