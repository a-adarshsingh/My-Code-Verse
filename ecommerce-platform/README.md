# Scalable E-Commerce Platform (Java / Spring Boot microservices)

A microservices e-commerce backend based on the [roadmap.sh Scalable E-Commerce Platform](https://roadmap.sh/projects/scalable-ecommerce-platform) project idea.

**Stack:** Java 17, Spring Boot 3.3, Spring Cloud (Gateway + Eureka), Spring Data JPA, PostgreSQL, JWT, Docker Compose, Prometheus + Grafana, GitHub Actions.

## Architecture

```
                      +------------------+
 client ───────────►  |   api-gateway    |  :8080  (JWT check, routing, admin-only catalog writes)
                      +--------+---------+
                               │ lb:// (Eureka)
   ┌───────────┬───────────────┼───────────────┬─────────────┐
   ▼           ▼               ▼               ▼
user-service product-service cart-service  order-service ──► payment-service (mock gateway)
 :8081        :8082          :8083          :8084        └──► notification-service (logs)
   │           │               │              │
 userdb     productdb        cartdb        orderdb   (database per service, Postgres)

 discovery-server (Eureka) :8761      prometheus :9090 → grafana :3000
```

| Service | Responsibility |
|---|---|
| discovery-server | Eureka service registry |
| api-gateway | Single entry point; validates JWT, forwards identity as `X-User-*` headers, blocks `/internal/**` |
| user-service | Register / login (BCrypt + JWT), profile |
| product-service | Catalog + inventory; atomic stock reserve/release (no overselling) |
| cart-service | Per-user cart |
| order-service | Checkout orchestration (saga with compensation), order history |
| payment-service | Mock payment gateway (declines above a configurable amount) |
| notification-service | Order notifications (logs; plug in SendGrid/Twilio) |

**Checkout flow** (`POST /api/orders`): read cart → reserve stock per item → save PENDING order → charge payment →
success: mark PAID + clear cart / failure: mark FAILED + release stock → send notification.

## Run it

**With Docker (everything, incl. Postgres + monitoring):**
```bash
docker compose up --build
```

**Locally without Docker** (each service uses in-memory H2). Start discovery first, then the rest, each in its own terminal:
```bash
mvn -pl discovery-server spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl user-service spring-boot:run      # likewise: product-, cart-, order-, payment-, notification-service
```

Eureka dashboard: http://localhost:8761 · Prometheus: http://localhost:9090 · Grafana: http://localhost:3000

## Try the API

```bash
# 1. register + login
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"email":"me@example.com","password":"secret123","name":"Me"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"me@example.com","password":"secret123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 2. browse (public)
curl localhost:8080/api/products

# 3. add to cart, check out
curl -X POST localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
curl localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"

# 4. admin: add a product (seeded admin: admin@shop.com / admin123)
```

| Method & path | Auth |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | public |
| `GET /api/users/me` | user |
| `GET /api/products`, `GET /api/products/{id}` | public |
| `POST/PUT/DELETE /api/products` | ADMIN |
| `GET/POST/DELETE /api/cart...` | user |
| `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}` | user |

## Design notes

- **Database per service**; services never share tables.
- **Auth at the edge:** only the gateway validates JWTs. Downstream services trust `X-User-Id` / `X-User-Email` / `X-User-Role`, so they must **not** be exposed publicly (in Compose only the gateway publishes a port).
- **Internal endpoints** live under `/internal/**` and have no gateway route.
- **Concurrency-safe stock:** `UPDATE ... SET stock = stock - :qty WHERE stock >= :qty` in a single statement.
- **Change before production:** `JWT_SECRET`, the seeded admin in `UserServiceApplication`, and DB credentials. Set `spring.jpa.hibernate.ddl-auto` to `validate` and use Flyway migrations.

## Next steps (from the roadmap.sh list)

1. Centralized logging with the ELK stack.
2. Kubernetes manifests / Helm chart, with autoscaling.
3. Redis for the cart and product cache; Kafka/RabbitMQ events instead of synchronous calls (orders → payment → notification).
4. Resilience4j (circuit breaker, retry, timeouts) around inter-service calls; idempotency keys for payments.
5. Real Stripe/PayPal integration in `payment-service`; SendGrid/Twilio in `notification-service`.
6. Integration tests with Testcontainers; OpenAPI docs (springdoc).
