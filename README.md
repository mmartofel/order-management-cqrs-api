# order-management-cqrs-api

REST API for managing orders: **Person, Address, Item, Order, Payment, Delivery**, all persisted in PostgreSQL.
Version 1.0.0.

## Repository layout

```
.
├── pom.xml                              # Maven parent (Spring Boot 4.1.2, Axon BOM 5.4.1), lists the modules
├── mvnw, .mvn/                          # Maven wrapper; no local Maven needed
├── order-management-events/             # event records shared by the command and query services
├── order-management-command-service/    # writes: Axon entities, command handlers, Kafka relay
├── order-management-query-service/      # reads: Kafka listeners, projections, read models, GET endpoints
├── order-management-reference-service/  # Person, Address, Item CRUD
├── docker-compose.yml                   # local PostgreSQL x3 and Kafka
├── deploy/, skaffold.yaml               # OpenShift / Kubernetes manifests (Kustomize) and image build + deploy
├── docs/images/                         # pictures used in this README
├── .github/workflows/ci.yml             # build and test on every push and pull request
└── CLAUDE.md                            # architecture decisions (style, scope, topology, event store)
```

Inside each service the code is organised by aggregate (`order`, `payment`, `delivery`, `person`, ...). The packages
are `domain` (event-sourced entities), `command` (commands, handlers, command controller), `projection` (Kafka
listeners) and `query` (read models, query handlers, query controller). Database migrations are under
`src/main/resources/db/migration`.

Order, Payment and Delivery use **CQRS + Event Sourcing** with [Axon Framework](https://www.axoniq.io) 5.4.1. Commands go to a
command service, which stores events. The events reach a separate query service through Kafka, and the query
service serves the reads. Person, Address and Item are reference data, kept as plain layered CRUD in their own service.

```
            POST                       events (Kafka)                    GET
client ───────────► command-service ────────────────────► query-service ◄──────── client
                    :8080   │  Axon, event store            :8081  read models
                            │  in PostgreSQL (orders_command)      in PostgreSQL (orders_query)
                            │ validates person/address/item
                            ▼
                    reference-service :8082  Person, Address, Item (CRUD) in PostgreSQL (orders_reference)
```

| Module | Port | Database | Role |
|---|---|---|---|
| `order-management-events` | - | - | Event records; the contract between command and query services. No dependencies |
| `order-management-command-service` | 8080 | `orders_command` (5432) | Axon event-sourced Order, Payment, Delivery; command endpoints; relays events to Kafka |
| `order-management-query-service` | 8081 | `orders_query` (5433) | Kafka consumers, read models, `GET` endpoints |
| `order-management-reference-service` | 8082 | `orders_reference` (5434) | CRUD for Person, Address, Item |

Stack: Java 21, Spring Boot 4.1.2, Axon Framework 5.4.1, PostgreSQL 18, Apache Kafka 4.1, Flyway, Testcontainers.

## Run locally

Prerequisites: Java 21, and Docker or Podman. Maven comes with the wrapper.

**1. Start the dependencies.** This starts three PostgreSQL databases (ports 5432, 5433, 5434) and Kafka (9092):

```bash
docker compose up -d --wait          # or: podman compose up -d --wait
```

**2. Build and test.** The tests start their own PostgreSQL and Kafka containers through Testcontainers:

```bash
./mvnw clean verify
```

With Podman, point Testcontainers at the Podman socket first:

```bash
export DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
export TESTCONTAINERS_RYUK_DISABLED=true
```

**3. Start the services**, each in its own terminal. Flyway creates the tables on first start:

```bash
java -jar order-management-reference-service/target/order-management-reference-service-1.0.0.jar   # :8082
java -jar order-management-command-service/target/order-management-command-service-1.0.0.jar       # :8080
java -jar order-management-query-service/target/order-management-query-service-1.0.0.jar           # :8081
```

Each service is ready when `curl localhost:<port>/actuator/health/readiness` returns `UP`. Then try the
[example](#example) below.

**4. Stop.** Press `Ctrl+C` in each service's terminal, then remove the dependencies (`-v` also deletes the data):

```bash
docker compose down -v
```

### Configuration

All settings have local defaults that match `docker-compose.yml`. Override them with environment variables:

| Variable | Services | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | all | `jdbc:postgresql://localhost:5432/orders_command` (command), `:5433/orders_query` (query), `:5434/orders_reference` (reference) |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | all | `orders` / `orders` (local only) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | command, query | `localhost:9092` |
| `SERVER_PORT` | all | 8080 command, 8081 query, 8082 reference |
| `REFERENCE_BASE_URL` | command | `http://localhost:8082` |
| `QUERY_CONSUMER_GROUP` | query | `order-management-query-service` |

## Run on OpenShift

Everything runs in one OpenShift project. The manifests are in `deploy/` (Kustomize), and `skaffold.yaml` builds the
three service images with Jib and deploys them:

| Component | Resources | Notes |
|---|---|---|
| `reference-service`, `command-service`, `query-service` | Deployment, Service, ConfigMap, Route | Image on `ubi9/openjdk-21-runtime`; probes on `/actuator/health/{liveness,readiness}` |
| `postgres-command`, `postgres-query`, `postgres-reference` | Deployment, Service, PVC (1Gi) | `postgres:18`; credentials in the Secrets of the same name |
| `kafka` | Deployment, Service, PVC (2Gi) | Single-node `apache/kafka:4.1.0` in KRaft mode; topics are created by the services |

This is how the deployed stack looks in the Topology view of the OpenShift console. An arrow points from each service
to what it calls, and a Route link sits on each service:

![The order-management application in the OpenShift Topology view: command, query and reference services, Kafka, and three PostgreSQL databases](docs/images/openshift-topology.png)

```
deploy/
  base/                   plain Kubernetes: Deployments, Services, ConfigMaps, PVCs, probes, resources
  overlays/
    local/                local cluster (kind, minikube); Skaffold forwards ports 8080-8082
    openshift/            + Routes (edge TLS, host generated by OpenShift) and Topology runtime icons
  environments/
    example/              how an environment overlay looks; real ones (deploy/environments/<env>) are git-ignored
  ensure-secrets.sh       Skaffold before-deploy hook: creates the missing database Secrets
skaffold.yaml
```

Prerequisites: `oc`, `kubectl`, `skaffold` v2, Java 21, and a registry the cluster can pull from.

```bash
oc login --server=<api-url> ...                       # or a token from the web console
oc new-project order-management                       # or: oc project <existing>
./mvnw install -DskipTests                            # Skaffold builds the images from the installed modules
skaffold run -p openshift --default-repo=<registry>/<org> -n order-management
oc get route                                          # the generated hosts of the three services
# optional: the version label, set at deploy time and never in a file
skaffold run -p openshift --default-repo=quay.io/<org> -n order-management --label app.kubernetes.io/version=$(git rev-parse --short HEAD)
```

- **Registry:** `--default-repo` is where Jib pushes the images, as `<default-repo>/<image>`. With Quay.io that is
  `--default-repo=quay.io/<org>`: the registry path, not the web page `quay.io/repository/<org>`. Jib needs no Docker or
  Podman, and reads the registry credentials from `~/.docker/config.json`, not from Podman's `auth.json`. With a
  Quay.io robot account:
  ```bash
  echo "$QUAY_TOKEN" | podman login quay.io -u "$QUAY_USER" --password-stdin --authfile ~/.docker/config.json
  ```
  A robot account usually cannot create repositories. Create them in Quay first, and make them public or give the
  namespace a pull secret, otherwise the pods stay in `ImagePullBackOff`:
  `order-management-reference-service`, `order-management-command-service`, `order-management-query-service`.
  Skaffold's warning `Could not get docker context` is harmless.
- **Secrets:** on the first deploy, `deploy/ensure-secrets.sh` creates `postgres-command`, `postgres-query` and
  `postgres-reference`, with user `orders` and a random password. It never changes an existing Secret, because
  PostgreSQL keeps the password of its first start. No secret value is in git.
- **Security:** every container runs under OpenShift's `restricted-v2` SCC, with a random UID and no root. The Kafka
  image needs a writable config folder for that: an init container copies it into an `emptyDir`.
- **Topology:** in the Developer console the stack is the `order-management` group (see the picture above). It shows
  Spring, PostgreSQL and AMQ (Kafka) icons, arrows from each service to what it calls, and a link to this repository.
- **Environment settings:** copy `deploy/environments/example` to `deploy/environments/<env>`, adjust replicas and
  volume sizes, and deploy with `DEPLOY_OVERLAY=deploy/environments/<env> skaffold run -p openshift ...`.
- **Remove:** `skaffold delete -p openshift -n order-management` removes everything in the manifests, **including the
  PVCs and so all data**. The Secrets stay, because the hook created them outside the manifests. Delete them with
  `oc delete secret -l app.kubernetes.io/part-of=order-management`. To redeploy and keep the data, run
  `skaffold run` again instead.

Then use the [example](#example) below with the Route hosts instead of `localhost:<port>`.

## API

Base path `/api/v1` on every service. **Writes go to the command service (8080), reads to the query service (8081)**,
on the same resource paths. Errors are RFC 9457 Problem Details (`application/problem+json`). Validation errors list
the fields in `errors[]` with a stable `code` (`required`, `email`, `unique`, `invalid`).

### Reference data - `:8082`

| Method | Path | Notes |
|---|---|---|
| `GET` | `/persons`, `/addresses?personId=`, `/items` | Paged: `?page=&size=&sort=` (max size 100) |
| `GET` | `/persons/{id}`, `/addresses/{id}`, `/items/{id}` | 404 if unknown |
| `POST` | `/persons`, `/addresses`, `/items` | 201 + `Location`; 409 for a duplicate email or SKU; 422 for an address of an unknown person |
| `PUT` | `/persons/{id}`, `/addresses/{id}`, `/items/{id}` | Full replace with the `version` last read; 409 if stale |
| `DELETE` | `/persons/{id}`, `/addresses/{id}`, `/items/{id}` | 204; 409 while a person still has addresses |

### Commands - `:8080`

Every command returns `{"id": "<uuid>"}`. A creating command returns **201**; the others return **202**.

| Method | Path | Body | Rule |
|---|---|---|---|
| `POST` | `/orders` | `{personId, addressId, lines: [{itemId, quantity}]}` | Person, address (of that person) and items must exist (else **422**); item name and price are copied into the order |
| `POST` | `/orders/{id}/cancel` | `{reason}` | Only PLACED or PAID orders |
| `POST` | `/payments` | `{orderId, amount, method}` | Order PLACED, amount = order total; method `CARD`, `TRANSFER`, `BLIK` or `CASH_ON_DELIVERY` |
| `POST` | `/payments/{id}/complete` | `{transactionReference}` | Also marks the order **PAID**, in the same transaction |
| `POST` | `/payments/{id}/fail` | `{reason}` | Order stays PLACED; a new payment can be started |
| `POST` | `/payments/{id}/refund` | `{reason}` | Only a completed payment of a cancelled order |
| `POST` | `/deliveries` | `{orderId, carrier, scheduledFor}` | Order PAID; delivered to the order's address |
| `POST` | `/deliveries/{id}/ship` | `{trackingNumber}` | Also marks the order **SHIPPED** |
| `POST` | `/deliveries/{id}/complete` | - | Also marks the order **DELIVERED** |
| `POST` | `/deliveries/{id}/cancel` | `{reason}` | Only a scheduled delivery; the order stays PAID |

Status codes: **400** validation, **404** unknown order/payment/delivery, **409** a rule of the current state is
broken (for example shipping an unpaid order), or another request changed the same aggregate at the same time (retry),
**422** an unknown person, address, item or order is referenced, **503** the reference service is unreachable.

State machines:

```
Order     PLACED ─► PAID ─► SHIPPED ─► DELIVERED          PLACED|PAID ─► CANCELLED
Payment   INITIATED ─► COMPLETED ─► REFUNDED              INITIATED ─► FAILED
Delivery  SCHEDULED ─► SHIPPED ─► DELIVERED               SCHEDULED ─► CANCELLED
```

### Queries - `:8081`

| Method | Path | Notes |
|---|---|---|
| `GET` | `/orders?personId=&status=` | Paged order summaries |
| `GET` | `/orders/{id}` | Order with lines, payments and deliveries |
| `GET` | `/payments?orderId=`, `/payments/{id}` | |
| `GET` | `/deliveries?orderId=`, `/deliveries/{id}` | |

Every response carries `version`: the sequence number of the aggregate's last applied event (0 = created).

### Example

```bash
curl -XPOST localhost:8082/api/v1/persons -H 'Content-Type: application/json' \
     -d '{"firstName":"Anna","lastName":"Nowak","email":"anna@example.com"}'
curl -XPOST localhost:8082/api/v1/addresses -H 'Content-Type: application/json' \
     -d '{"personId":1,"street":"Marszalkowska 1","city":"Warsaw","postalCode":"00-001","country":"PL"}'
curl -XPOST localhost:8082/api/v1/items -H 'Content-Type: application/json' -d '{"sku":"MUG-1","name":"Mug","price":19.99}'

curl -XPOST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
     -d '{"personId":1,"addressId":1,"lines":[{"itemId":1,"quantity":2}]}'          # -> {"id":"<order>"}
curl localhost:8081/api/v1/orders/<order>                                            # PLACED, total 39.98
```

## Eventual consistency

**Queries are eventually consistent.** A `GET` on the query service right after a command may still show the old
state for a short time, typically well under a second, while the event travels through Kafka. A client that
needs its own write should poll until `version` has moved or the expected `status` shows up. The command service
itself is strongly consistent: rules are always checked against the full event history.

## How it works

**Event store.** The events are the source of truth for Order, Payment and Delivery. They are stored in PostgreSQL
by Axon's open-source `AggregateBasedJpaEventStorageEngine`, in the table `aggregate_event_entry` (append-only, one
row per event). Axon Server is not used (`axon.axonserver.enabled=false`), and neither is the commercial
`axoniq-postgresql` add-on. Each aggregate is its own consistency boundary. Two commands that append the same
sequence number of an aggregate conflict on a unique constraint, and the second one gets 409.

**Commands.** There is one handler class per command (`*Handler` in `<aggregate>.command`). A handler loads the
entity, calls a decision method on it, and appends the resulting events. The entities (`<aggregate>.domain`) have no
JPA mapping. They are rebuilt from their events, and their `@EventSourcingHandler`s only assign state. Commands
that change two aggregates, for example completing a payment and marking the order paid, append both events in
one transaction.

**Relay to Kafka.** `KafkaEventRelay` is the only class that publishes. It is an Axon event handler in its own
pooled streaming processor (`kafka-relay`, one segment). It reads committed events from the event store, sends each
one to its topic, and waits for the broker's acknowledgement. Its position is in Axon's `token_entry` table, so a
failed send is retried and nothing is lost. A retry can publish an event twice, and the query side ignores the
duplicate.

| Topic | Key | Headers |
|---|---|---|
| `order-events`, `payment-events`, `delivery-events` (3 partitions, no retention limit) | aggregate id | `event-type`, `event-id`, `sequence-number`, `occurred-at` |

Never raise the partition count of these topics. It changes which partition a key goes to and breaks the order of
an aggregate's events.

**Projections.** One listener per topic feeds a projection. A projection applies an event only if its
`sequence-number` is newer than the row's `version`, so redelivered and duplicate events change nothing. Unknown
event types are skipped with a warning, so an older query service keeps running when new event types appear.
Unknown fields in a payload are ignored. A record that still fails after three retries goes to `<topic>-dlt`.
**Alert on the dead-letter topics**: a record there means a read model is missing a change.

**Rebuilding the read models.** The topics keep every event, so the read models can be rebuilt at any time:

1. Stop the query service.
2. `TRUNCATE order_line_view, order_view, payment_view, delivery_view;` in `orders_query`.
3. Start it with a new consumer group (`QUERY_CONSUMER_GROUP=rebuild-2`), or reset the group:
   `kafka-consumer-groups.sh --reset-offsets --to-earliest --execute --group order-management-query-service --all-topics`.

**Evolving events.** A stored event is never changed. Adding a field is safe, because consumers ignore unknown
fields; give the new field a sensible default when it is missing from older events. A renamed or restructured
event becomes a new event type, and the old one stays readable. A change to an event record is a change to the
contract between the services, so it bumps the version.

## Personal data and erasure

Events hold **no personal data**. They reference people and addresses by id only (`personId`, `addressId`).
Names, e-mail addresses, phone numbers and street addresses exist only in the reference service, where they can
be updated or deleted. **To erase a person:** delete their addresses and then the person in the reference service.
The immutable order history keeps only ids that no longer resolve. Payment events carry the payment method name
and a transaction reference, never card or account numbers.

## Schema

Flyway owns every table in every service (`src/main/resources/db/migration`). Hibernate only validates
(`ddl-auto: validate`). Axon's event and token tables are created by `V1__create_axon_event_store.sql` in the
command service, from the DDL of Axon 5.4.1's JPA entities. Axon stores payloads as `@Lob byte[]`, which Hibernate
maps to PostgreSQL large objects (`oid`), so read event payloads through Axon rather than with plain SQL. Check
this migration again when you upgrade Axon.

## Axon update checker

Axon's update checker reports JVM and OS details to Axoniq. It is **off** in this project: `CommandServiceApplication`
sets `axoniq.update-checker.disabled=true` unless the property is already set, and so do the tests and CI. To turn
it on, start with `-Daxoniq.update-checker.disabled=false` or `AXONIQ_UPDATE_CHECKER_DISABLED=false`.

## Tests

`./mvnw clean verify` runs 57 tests against real PostgreSQL and Kafka containers:

- **reference service (18)**: CRUD, validation (400), not found (404), duplicate email or SKU (409), stale `version`
  (409), delete while referenced (409), bad sort (400), all migrations applied.
- **command service (19 + 10)**: Axon test fixture, given events → when command → then events, for every handler,
  including rejected transitions that append nothing. Also an integration test of the REST endpoints with the
  records on the Kafka topics, 400/404/409/422, and that a rejected command stores nothing. The reference service
  is mocked.
- **query service (10)**: events published to Kafka → read models and `GET` endpoints, redelivered and older events
  skipped, unknown event type skipped, unknown payload field tolerated, a broken record dead-lettered, 400/404.

CI: `.github/workflows/ci.yml` runs `./mvnw -B clean verify` on every push and pull request.

## Versioning

Semantic versioning, starting at `1.0.0`. All modules share the parent's version. A change to an event record or to
an endpoint is a contract change and bumps it.
