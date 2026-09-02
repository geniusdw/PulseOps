# PulseOps — Requirements & Architecture

Design notes captured before implementation: the problem, the domain model, the
correlation approach, and the technology choices with their rationale.

## 1. Problem statement

Cloud systems emit large volumes of logs, metrics, alerts, and deployment
events. During an outage, engineers manually stitch these signals together to
answer two questions:

1. **Which events belong to the same incident?**
2. **What is the most likely root cause?**

This correlation work is slow, error-prone, and depends on tribal knowledge of
service dependencies. PulseOps automates it with a deterministic, explainable
correlation engine. An LLM is an *optional* layer that explains an incident in
prose — it never decides the grouping.

## 2. What PulseOps does (pipeline)

```
ingest -> validate/normalize -> store -> detect anomalies -> correlate
      -> group into incidents -> score severity -> rank root causes
      -> build timeline -> expose via REST -> render in React
```

## 3. Scope for v1 (deliberately constrained)

**In:** single Spring Boot app (modular, not micro), MySQL, React dashboard,
in-process producer/consumer pipeline, deterministic correlation, heuristic
root-cause scoring, event simulator, Docker Compose, tests, benchmark harness,
mock LLM explainer behind an interface.

**Out (with production-path notes in README):** Kafka, Redis, Elasticsearch,
Prometheus/Grafana, Kubernetes, service mesh, real cloud providers, auth. Each
is discussed as "how this becomes production" — not built.

## 4. Core domain model

### Event
Observation from infrastructure. Immutable once stored.

| field | type | notes |
|---|---|---|
| eventId | string | server-assigned, `EVT-<n>` |
| timestamp | Instant (UTC) | required, not future-dated beyond small skew |
| service | string | must exist in service topology |
| host | string | optional |
| eventType | enum | see below |
| severity | enum | LOW / MEDIUM / HIGH / CRITICAL |
| metric | string | optional (e.g. `error_rate`) |
| value | double | optional |
| message | string | required, human text |
| deploymentId | string | optional, links to a DEPLOYMENT |

**Event types:** HTTP_500, HTTP_503, HIGH_LATENCY, CPU_SPIKE, MEMORY_SPIKE,
DB_CONNECTION_EXHAUSTION, NETWORK_ERROR, DEPLOYMENT, SERVICE_RESTART,
PAYMENT_FAILURE, QUEUE_BACKLOG.

### Service topology (seed data)

```
api-gateway
   -> payment-api
        -> transaction-service
             -> database
   -> user-service
        -> database
   -> notification-service
```

Dependency edges are directed (`A -> B` means "A depends on B"). The
correlation engine uses **dependency distance** between two services.

### Incident

| field | notes |
|---|---|
| incidentId | `INC-<n>` |
| title | generated from dominant service + event types |
| severity | LOW / MEDIUM / HIGH / CRITICAL (derived) |
| status | OPEN -> INVESTIGATING -> RESOLVED |
| startedAt / resolvedAt | from earliest event / on resolve |
| affectedServices | set |
| correlatedEvents | many-to-many |
| probableRootCause | top-ranked candidate |
| confidenceScore | 0..1, heuristic (labelled as such) |

## 5. Correlation engine (the core)

Pairwise **correlation score** between two events, weighted sum of normalized
sub-scores in `[0,1]`:

```
score(e1,e2) =
    wT * temporal(e1,e2)          // closeness in time, decays over window
  + wS * serviceDependency(e1,e2) // 1.0 same svc, decays with dep-graph distance
  + wE * eventTypePair(e1,e2)     // lookup table of known co-occurring pairs
  + wD * deploymentProximity(e1,e2) // recent shared/preceding DEPLOYMENT
  + wV * severity(e1,e2)          // both-critical pushes score up
```

Default weights (configurable in `application.yml`):

```
temporalWeight: 0.30
serviceDependencyWeight: 0.25
eventTypeWeight: 0.20
deploymentWeight: 0.15
severityWeight: 0.10
```

Grouping: events whose pairwise score exceeds `correlationThreshold` are linked;
connected components (union-find) within a sliding time window become one
incident. Every incident stores a **human-readable explanation** built from the
sub-scores that fired ("grouped because within 2m, dependent services, followed
DEP-482").

## 6. Root-cause ranking

After grouping, score candidate causes for the incident:
deployment, database failure, service failure, resource exhaustion,
network problem, dependency failure.

Each candidate gets a heuristic **root-cause score** from rules like:
"a DEPLOYMENT within N minutes before the first anomaly" (+deployment),
"DB_CONNECTION_EXHAUSTION on `database` upstream of affected services"
(+database failure / dependency failure). Output is a ranked list with a short
evidence string per candidate. Explicitly documented as heuristic, not
probability.

## 7. Concurrency model

```
Simulator / POST /api/events
        |
        v
   BlockingQueue<Event>   (bounded -> backpressure)
        |
   +----+----+----+
   | worker  ... |   (ThreadPoolExecutor, N configurable)
   +----+----+----+
        |
   normalize + persist + hand window to CorrelationEngine
```

- **BlockingQueue (bounded):** decouples ingest rate from processing rate;
  full queue blocks/duplicates-429 the producer = backpressure.
- **Thread pool:** bounded parallelism sized to cores / DB connection pool.
- **Race avoidance:** correlation window state guarded (per-window lock /
  `ConcurrentHashMap` + single-writer per window); counters via `AtomicLong`.
- **Scale-out path:** replace queue with Kafka, workers become consumer group.

## 8. Component / package layout (backend)

```
com.pulseops
  api/          controllers + DTOs
  ingest/       queue, workers, validation, normalization
  correlation/  scoring, weights config, grouping (union-find)
  incident/     incident lifecycle, severity, root-cause ranking
  topology/     service graph + dependency distance
  simulator/    scenario generators
  explain/      LLM explainer interface + mock impl
  persistence/  JPA entities, repositories
  common/       exceptions, error model, time
```

Correlation engine has **no Spring-web dependency** — pure domain logic, unit
testable without a container.

## 9. Data model (MySQL)

Tables: `services`, `service_dependencies` (self-join M:N), `deployments`,
`events`, `incidents`, `incident_events` (M:N join with per-link correlation
score), plus indexes on `events(timestamp)`, `events(service)`,
`events(event_type)`.

## 10. REST surface (v1)

```
POST /api/events                     GET /api/events?service=&severity=&type=&from=&to=
GET  /api/events/{id}
GET  /api/incidents                  GET /api/incidents/{id}
POST /api/incidents/{id}/acknowledge POST /api/incidents/{id}/resolve
GET  /api/incidents/{id}/events       /timeline   /root-cause
POST /api/incidents/{id}/explain     (mock LLM)
GET  /api/services  /{name}  /{name}/dependencies
POST /api/simulator/scenarios/{name} (database-failure|bad-deployment|cpu-saturation|network-failure|normal)
GET  /actuator/health
```

Central `@RestControllerAdvice` returns `{timestamp,status,error,message}`,
never stack traces.

## 11. React dashboard pages

Dashboard, Incidents (filterable table), Incident Details (timeline + explanation
+ related events), Event Explorer, Service Map (graph, active-incident
highlight), Simulator (scenario buttons).

Structure: `components/ pages/ services/ hooks/ charts/ utils/`.

## 12. Technology choices & justification

| tech | why | why not alternative |
|---|---|---|
| Spring Boot | REST + JPA + DI + test slices in one stack | — |
| MySQL | relational: events↔incidents joins, M:N | NoSQL adds no value at this scale |
| Spring Data JPA | removes boilerplate DAO code | raw JDBC = more code, no gain |
| React + Router | SPA with a few routes | Next.js SSR unnecessary |
| Recharts | small, declarative charts | D3 too low-level for this |
| Docker Compose | one-command run of 3 services | K8s is operational overhead |
| in-proc queue | demonstrates concurrency honestly | Kafka is infra we can't justify yet |

## 13. Build order

Roughly the order the pieces were put together: event model + database → ingestion
REST API → simulator → concurrent processing pipeline → correlation engine →
incident management → root-cause ranking → React dashboard → service-map
visualization → Docker → tests → performance benchmark → optional AI explanation
layer → documentation.

## 14. Design rationale — the decisions worth defending

- Why the correlation engine is deterministic and the LLM is not the source of
  truth (reproducibility, testability, cost, trust).
- Why a bounded queue gives backpressure and an unbounded one hides failure.
- Why service-dependency distance is a stronger correlation signal than
  same-service grouping.
- Why root-cause outputs are called "scores" not "probabilities".
- The monolith-first decision and where the real microservice seams are.
