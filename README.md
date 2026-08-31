# ECM Backend API

Spring Boot backend for the ECM e-commerce system.

## Prerequisites

- Java 21
- Maven 3.9+ (or Docker)
- PostgreSQL 16+
- Redis 7+ (required for OTP, token blacklist and rate limiting)

The canonical database schema is created by Flyway migrations, starting with
`server/src/main/resources/db/migration/V1__init.sql` and applying incremental
migrations such as `V2__add_shipping_method_fee.sql`. JPA runs with
`ddl-auto=validate`, so application startup detects schema drift instead of
silently changing the database.

## Local development

Start PostgreSQL and Redis (the repository staging compose intentionally
contains only PostgreSQL), then configure the variables from `.env.example`.
The important defaults are:

```text
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=ecm
DB_USERNAME=ecm
DB_PASSWORD=ecm
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
```

`JWT_SECRET_KEY` must be Base64-encoded and decode to at least 32 bytes. Never
commit real credentials or production secrets.

The repository `docker-compose.yml` uses the versioned PostgreSQL volume
`ecm-postgres-v2` by default. This prevents a legacy pre-canonical volume from
being attached accidentally; override `POSTGRES_VOLUME_NAME` only when the
volume has been migrated or intentionally reset. Redis uses a named AOF-backed
volume (`ecm-redis` by default) for session/revocation state.

Run the server:

```bash
cd server
./mvnw spring-boot:run
```

If Maven is already installed, `mvn spring-boot:run` is equivalent.

When Maven is not installed, run the same command through Docker from the
`backend-api` directory:

```bash
docker run --rm --network host -v "$PWD":/workspace -w /workspace/server \
  maven:3.9.9-eclipse-temurin-21 mvn spring-boot:run
```

## API documentation and health

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`

All API responses use the same envelope:

```json
{
  "data": {},
  "message": "STATIC_MESSAGE_KEY",
  "errors": []
}
```

`message` is a stable key for frontend mapping. Validation and business
details are returned in `errors`; dynamic prose must not be put in `message`.

`POST /api/v1/auth/logout` requires the current Bearer access token. The
endpoint revokes the access token and writes an account-level
`tokens-revoked-before` marker for the configured refresh-token lifetime, so
access/refresh tokens issued before the logout cutoff second are rejected even
when the client does not send the refresh token body. Sending
`{"refreshToken":"..."}` additionally stores
an explicit blacklist entry for that token. Revocation is fail-closed: if Redis
cannot persist the marker/blacklist, the endpoint returns `SERVICE_UNAVAILABLE`
instead of reporting a successful logout.

Redis is configured with AOF persistence in the repository compose file. If a
production Redis data loss is unrecoverable, rotate `JWT_SECRET_KEY` before
accepting traffic and restart the backend; this invalidates every outstanding
JWT while the revocation store is rebuilt.

## Tests and packaging

```bash
cd server
mvn test
mvn package
```

For an environment without local Maven, use the Docker command above and
replace the final goal with `mvn -q test` or `mvn -q -DskipTests package`.
