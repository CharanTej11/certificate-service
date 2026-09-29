# Certificate Service

A Spring Boot service that issues training certificates which **never change** after issue, even
when the underlying programme is renamed, re-designed, disabled, or pulled out of a track.

- **Java 21**, Spring Boot 3.3, Spring Web / Data JPA / Validation / Actuator
- **PostgreSQL** with **Flyway** migrations
- **Testcontainers** integration tests against a real PostgreSQL
- **Docker / Docker Compose** for one-command local run
- **OpenAPI / Swagger UI** at `/swagger-ui.html`

---

## Run it (clean machine)

You need **Docker** and **Docker Compose**. Nothing else.

```bash
docker compose up --build
```

This starts PostgreSQL and the app. When it is up:

- API base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Load the demo dataset (idempotent):

```bash
curl -X POST http://localhost:8080/api/v1/dev/seed
```

The seed creates: **two tracks sharing a programme** (Advanced SQL is in both Data Engineering
Track and Analytics Track), **all three rule kinds** (MINIMUM_SCORE, ALL_TASKS_DONE, ANYONE), and
**a few old certificates with no design**. The response returns the ids you need to try the API.

### Run without Docker

Start a PostgreSQL yourself and point the app at it:

```bash
# Postgres reachable at localhost:5432, db/user/password all "certificates"
./mvnw spring-boot:run       # or: mvn spring-boot:run
```

Requires a **JDK 21** on the machine. Flyway creates the schema on startup.

---

## Database

PostgreSQL 16. Schema is created and versioned by Flyway
(`src/main/resources/db/migration/V1__init.sql`). Timestamps are stored as `timestamptz` (UTC).
Public certificate ids are non-sequential, URL-safe codes (`CERT-XXXXXXXXXXXX`) so they are safe
to put in a public link and cannot be guessed or enumerated.

---

## Endpoints (with one example each)

Base path: `/api/v1`. Bodies are JSON. IDs are UUIDs unless noted.

| Method & path | Purpose |
|---|---|
| `POST /people` | Create a person |
| `GET /people/{id}` | Get a person |
| `POST /designs` | Create a design |
| `GET /designs` / `GET /designs/{id}` | List / get designs |
| `POST /designs/{id}/disable` | Disable a design |
| `POST /programmes` | Create a programme |
| `GET /programmes` / `GET /programmes/{id}` | List / get programmes |
| `PATCH /programmes/{id}` | Rename / re-describe |
| `POST /programmes/{id}/disable` | Disable a programme |
| `POST /programmes/{id}/design` | Attach a design (new version) |
| `PUT /programmes/{id}/rule` | Set the eligibility rule |
| `POST /tracks` | Create a track |
| `GET /tracks` / `GET /tracks/{id}` | List / get tracks |
| `POST /tracks/{id}/programmes` | Add a programme to a track |
| `DELETE /tracks/{id}/programmes/{programmeId}` | Remove a programme from a track |
| `POST /certificates` | Issue one certificate |
| `POST /certificates/bulk` | Issue to many people |
| `GET /certificates/{publicCode}` | Public lookup by code (forever) |
| `POST /certificates/{id}/cancel` | Cancel with a reason |
| `GET /people/{personId}/certificates?page=0&size=20` | A person's certificates, newest first |

### Examples

Create a programme:
```bash
curl -X POST localhost:8080/api/v1/programmes \
  -H 'Content-Type: application/json' \
  -d '{"name":"Advanced SQL","description":"SQL tuning"}'
```

Set its rule:
```bash
curl -X PUT localhost:8080/api/v1/programmes/<programmeId>/rule \
  -H 'Content-Type: application/json' \
  -d '{"ruleType":"MINIMUM_SCORE","threshold":70}'
```

Issue a certificate (on its own — no track):
```bash
curl -X POST localhost:8080/api/v1/certificates \
  -H 'Content-Type: application/json' \
  -d '{"personId":"<personId>","programmeId":"<programmeId>","score":85}'
```

Issue through a track:
```bash
curl -X POST localhost:8080/api/v1/certificates \
  -H 'Content-Type: application/json' \
  -d '{"personId":"<personId>","programmeId":"<programmeId>","trackId":"<trackId>","score":85}'
```

Bulk issue:
```bash
curl -X POST localhost:8080/api/v1/certificates/bulk \
  -H 'Content-Type: application/json' \
  -d '{"programmeId":"<id>","trackId":"<id>","people":[
        {"personId":"<id>","score":80},
        {"personId":"<id>","score":64}]}'
```

Public lookup:
```bash
curl localhost:8080/api/v1/certificates/CERT-XXXXXXXXXXXX
```

Error responses share one shape:
```json
{ "timestamp":"...", "status":409, "code":"duplicate_certificate",
  "message":"...", "path":"/api/v1/certificates", "details":null }
```

Status codes: `201` create/issue, `200` reads/updates, `204` no-content actions,
`400` validation, `404` not found, `409` duplicate certificate, `422` rule/eligibility violation.

---

## Tests

Tests run against a **real PostgreSQL** via Testcontainers (Docker must be running):

```bash
mvn test
```

Coverage of the hard parts:
- `CertificateImmutabilityIT` — survives rename + design change + disable + track removal
- `CertificateConcurrencyIT` — 16 simultaneous requests → exactly one certificate
- `CertificateUniquenessIT` — same track refused, **on-its-own refused**, two tracks allowed
- `RuleEvaluationIT` — all three rule kinds pass/fail; threshold change via API, no restart
- `BulkIssueIT` — good ones issued, bad ones reported with reasons
- `CertificateNoDesignIT` — no-design certificate issues, looks up and lists
- `ProgrammeDesignVersioningIT` — "which design on 5 March?"

---

## Running this for real (production)

- Put it behind a gateway with TLS; add authentication (JWT/OAuth2). Secure all admin write
  endpoints; **remove `/dev/seed`**.
- Managed PostgreSQL with backups and PITR; run Flyway on deploy.
- Horizontal scaling: the app is stateless; the DB unique index keeps correctness across replicas.
- Add read replicas and a cache for the public lookup path (heavy verify traffic).
- Move large bulk jobs to an async worker with the existing job tables and a status endpoint.
- Ship the JSON logs (the `docker` profile emits structured logs) plus metrics (`/actuator/prometheus`)
  to your observability stack.

## Assumptions made

- Cancelled certificates are still returned by lookup, clearly marked `CANCELLED`, so a verifier
  sees the truth rather than a misleading 404.
- A certificate may be re-issued after cancellation (partial unique index excludes cancelled rows);
  the new certificate gets a new id.
- Person name is snapshotted (see DESIGN.md §1).
- A programme must have a rule configured before it can issue certificates.

## Not finished

See DESIGN.md §9 — auth, async bulk, rate limiting, idempotency keys, tracing.
