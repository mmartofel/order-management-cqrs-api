# order-management

REST API for managing orders: Person, Address, Item, Order, Payment, Delivery, persisted in PostgreSQL.

**Built for a UI:** a crud-ui-blueprint UI is generated from `contract/openapi.json`. Follow `docs/API-CONTRACT.md`
(copied unchanged from the UI template; it wins over everything else here) and `docs/DEPLOY-CONVENTION.md`. Orders,
payments and deliveries are command resources (section 11 of the contract), persons, addresses and items CRUD
resources (sections 2-9). In the spring-boot-template plugin: `spring-boot` skill, `references/ui-contract.md`,
section 12 for the command resources.

## Architecture
style: cqrs-event-sourcing
scope: Order, Payment, Delivery
topology: separate-services
transport: kafka
read-store: postgresql
event-store: axon
axon-version: 5.4.1
axon-event-store: postgresql
axon-storage-engine: AggregateBasedJpaEventStorageEngine (open source, Apache 2; not the commercial axoniq-postgresql add-on)
gateway: Spring Cloud Gateway Server Web MVC, Spring Cloud 2025.1.4 (the GA train that accepts Spring Boot 4.1)

Person, Address and Item are outside the scope and live as layered CRUD in `order-management-reference-service`.

## Modules

| Module | Port (local) | Role |
|--------|------|------|
| `order-management-events` | - | Event records, status enums and the actions rules (`OrderActions`, ...); no Spring; the contract between command and query services |
| `order-management-gateway` | 8081 | The one origin of the API: routes by method, health per service, serves the merged contract; built last |
| `order-management-reference-service` | 8082 | Layered CRUD for Person, Address, Item |
| `order-management-command-service` | 8083 | Axon 5 event-sourced Order, Payment, Delivery; command endpoints; relays events to Kafka |
| `order-management-query-service` | 8084 | Kafka consumers, read models, `GET` endpoints, labels from the reference service |

Keycloak (realm `orders`, roles `orders-viewer` and `orders-editor`) runs on 8080. In a cluster the services keep
8080, 8081, 8082 and the gateway is the Service `order-management` on 8080.

## Rules for changes

- An endpoint or response change regenerates `contract/openapi.json` (`./mvnw clean verify`) and goes into the same
  commit; CI fails otherwise. A breaking change bumps the major version of all modules.
- A new command: an action endpoint `POST /api/<resource>/{id}/<action>` returning 202 `CommandResult`, a request
  with `version`, the action in the aggregate's `*Actions` class, and a case in `ActionsAgreementTest`.
- Events never carry personal data; the query service shows names as labels read from the reference service.
- The services' clients of the reference service are mocked in the integration tests: keep
  `ReferenceClientContractTest` and `ReferenceLabelsTest` in step with them.

## Build

- `./mvnw clean verify` builds and tests all modules. Tests use Testcontainers; with Podman set
  `DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')` and
  `TESTCONTAINERS_RYUK_DISABLED=true`.
- `docker compose up -d --wait` (or `podman compose`) starts the three databases, Kafka and Keycloak.
