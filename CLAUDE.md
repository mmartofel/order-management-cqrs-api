# order-management

REST API for managing orders: Person, Address, Item, Order, Payment, Delivery, persisted in PostgreSQL.

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

Person, Address and Item are outside the scope and live as layered CRUD in `order-management-reference-service`.

## Modules

| Module | Port | Role |
|--------|------|------|
| `order-management-events` | - | Event records only, no dependencies; the contract between command and query services |
| `order-management-command-service` | 8080 | Axon 5 event-sourced Order, Payment, Delivery; command endpoints; relays events to Kafka |
| `order-management-query-service` | 8081 | Kafka consumers, read models, `GET` endpoints |
| `order-management-reference-service` | 8082 | Layered CRUD for Person, Address, Item |

## Build

- `./mvnw clean verify` builds and tests all modules. Tests use Testcontainers; with Podman set
  `DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')` and
  `TESTCONTAINERS_RYUK_DISABLED=true`.
- `docker compose up -d --wait` (or `podman compose`) starts the three databases and Kafka.
