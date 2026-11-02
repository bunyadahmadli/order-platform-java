# order-platform-java

Two Spring Boot services that handle orders and stock, talking to each other through RabbitMQ.
When an order is confirmed, the inventory service reserves the stock asynchronously.

I built this to apply the patterns I use day to day on .NET (DDD, transactional outbox,
idempotent consumers, integration tests against real infrastructure) in the Java / Spring ecosystem.

## Architecture

```
                ┌──────────────────────┐                           ┌───────────────────────┐
  REST  ──────► │    order-service     │                           │   inventory-service   │ ◄────── REST
                │                      │                           │                       │
                │  Order aggregate     │      order.events         │  StockItem            │
                │  outbox_events  ─────┼──► (topic exchange) ──────┼► OrderConfirmed       │
                │  OutboxPublisher     │    order.confirmed        │  listener (idempotent)│
                └──────────┬───────────┘                           └──────────┬────────────┘
                           │                                                  │
                     PostgreSQL (orders)                             PostgreSQL (inventory)
                                                                     failed events → DLQ
```

- **Database per service.** Each service owns its schema; they only share events.
- **Transactional outbox.** Confirming an order and storing the `OrderConfirmed` event happen in
  the same database transaction. A scheduled publisher sends pending events to RabbitMQ, so an
  event is never lost if the broker is down or the app crashes between the two steps.
- **Idempotent consumer.** The outbox gives at-least-once delivery, so the inventory service
  records every processed `eventId` and skips duplicates.
- **Dead letter queue.** Events that can't be processed (unknown product, insufficient stock) go to
  `inventory.order-confirmed.dlq` instead of being retried forever.
- **Optimistic locking** with `@Version` on both aggregates.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4, Spring Web, Spring Data JPA (Hibernate), Spring AMQP, Bean Validation |
| Storage / messaging | PostgreSQL 16, RabbitMQ 3.13 |
| Testing | JUnit 5, AssertJ, Testcontainers, Awaitility, MockMvc |
| Quality / CI | JaCoCo, SonarQube, Jenkins |
| Runtime | Docker, Docker Compose |

## Order lifecycle

```
PENDING ──confirm──► CONFIRMED ──ship──► SHIPPED ──deliver──► DELIVERED
   │                     │
   └──────cancel─────────┴──► CANCELLED
```

- Orders can be cancelled until they are shipped.
- Cancelling an already cancelled order is a no-op, so clients can safely retry.
- Invalid transitions return `409 Conflict`.

## Running it

**Everything in Docker**

```bash
docker compose --profile app up -d --build
```

**Infrastructure in Docker, services from the IDE**

```bash
docker compose up -d
cd order-service && ./mvnw spring-boot:run
cd inventory-service && ./mvnw spring-boot:run
```

| Service | URL |
|---|---|
| order-service | http://localhost:8081 |
| inventory-service | http://localhost:8082 |
| RabbitMQ management | http://localhost:15672 (guest / guest) |

## Try it

```bash
# add stock
curl -X PUT localhost:8082/api/stock/SKU-100 \
  -H 'Content-Type: application/json' -d '{"quantity": 50}'

# place an order
curl -X POST localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"c-1","lines":[{"productCode":"SKU-100","quantity":4,"unitPrice":12.50}]}'

# confirm it (use the id from the previous response)
curl -X POST localhost:8081/api/orders/{id}/confirm

# stock is now 46
curl localhost:8082/api/stock/SKU-100
```

## API

**order-service**

| Method | Endpoint | |
|---|---|---|
| `POST` | `/api/orders` | Place an order |
| `GET` | `/api/orders/{id}` | Get an order |
| `GET` | `/api/orders?customerId=` | Orders of a customer |
| `POST` | `/api/orders/{id}/confirm` | Confirm, publishes `OrderConfirmed` |
| `POST` | `/api/orders/{id}/ship` | Ship |
| `POST` | `/api/orders/{id}/cancel` | Cancel |

**inventory-service**

| Method | Endpoint | |
|---|---|---|
| `GET` | `/api/stock/{productCode}` | Current stock |
| `PUT` | `/api/stock/{productCode}` | Set stock |

Errors are returned as RFC 7807 problem details.

## Tests

```bash
cd order-service && ./mvnw verify
cd inventory-service && ./mvnw verify
```

Docker needs to be running: integration tests start real PostgreSQL and RabbitMQ containers
with Testcontainers. They cover the REST API, outbox publishing, duplicate events and the
dead letter queue. Coverage reports end up in `target/site/jacoco`.

## CI

The `Jenkinsfile` builds and tests both services in parallel, runs SonarQube analysis, waits for
the quality gate and builds Docker images on `main`.

The Jenkins agent needs Docker access for Testcontainers and a SonarQube server configured
under the name `sonarqube`. To run SonarQube locally:

```bash
docker compose --profile ci up -d sonarqube
```

## License

[MIT](LICENSE)
