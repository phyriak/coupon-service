# Coupon Service

REST API for creating and redeeming coupons with usage limits and country-based restrictions.

The application validates coupon eligibility based on:

- coupon existence
- usage limit
- user country resolved from IP address

---

# Running the Application

The application should be started using the `local` Spring profile.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

or

```bash
java -jar target/coupon-service-0.0.x-SNAPSHOT.jar --spring.profiles.active=local
```

Configuration for local development is stored in `application-local.yml`.

---

# Running PostgreSQL with Docker

Docker Compose is used only for running PostgreSQL.

The repository contains a `.env.example` file with dummy values that can be used as a template.

Create a local `.env` file:

```bash
cp .env.example .env
```

Start PostgreSQL:

```bash
docker compose up -d
```

Stop PostgreSQL:

```bash
docker compose down
```

After the database is running, start the Spring Boot application locally.

---

# API Documentation

Swagger UI is available at:

```text
http://localhost:8090/swagger-ui/index.html
```

---

## Local Testing and Geolocation

Coupon redemption requires successful country resolution based on the client IP address.

When running the application locally, requests executed from Swagger UI originate from localhost (`127.0.0.1` or `::1`). These addresses cannot be geolocated and coupon redemption requests will be rejected with `403 Forbidden (COUNTRY_UNKNOWN)`.

To test country-based validation locally, use a tool that allows custom HTTP headers (for example Postman or curl) and provide a public IP address in the `X-Forwarded-For` header.

Example:

```bash
curl -X POST http://localhost:8090/api/v1/coupons/use \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 8.8.8.8" \
  -d '{
        "code": "TESTCOUPON",
        "userId": "user-123"
      }'
```

In deployed environments the `X-Forwarded-For` header is expected to be supplied automatically by a reverse proxy or load balancer (for example Nginx, Kubernetes Ingress or AWS ALB).

---

# API Endpoints

## Create Coupon

```http
POST /api/v1/coupons
Content-Type: application/json
```

Request:

```json
{
  "code": "TESTCOUPON",
  "usageLimit": 100,
  "country": "PL"
}
```

Response:

```http
201 Created
```

```json
{
  "id": 1,
  "code": "TESTCOUPON",
  "usageLimit": 100,
  "usageCount": 0,
  "country": "PL",
  "createdAt": "2025-05-27T12:00:00Z"
}
```

---

## Redeem Coupon

```http
POST /api/v1/coupons/use
Content-Type: application/json
```

Request:

```json
{
  "code": "TESTCOUPON",
  "userId": "user-123"
}
```

Response:

```http
200 OK
```

```json
{
  "code": "TESTCOUPON",
  "userId": "user-123",
  "message": "Coupon applied successfully"
}
```

---

## Error Handling

The application uses centralized exception handling implemented with `@RestControllerAdvice`.

All business and validation errors are translated into consistent HTTP responses and a unified error format:

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Coupon code already exists",
  "timestamp": "2025-05-27T12:00:00Z"
}
```

---

# Error Responses

## Create Coupon

| HTTP Status | Error | Description |
|------------|---------|-------------|
| 201 Created | - | Coupon created successfully |
| 400 Bad Request | VALIDATION_ERROR | Request validation failed |
| 409 Conflict | CONFLICT | Coupon code already exists |
| 500 Internal Server Error | INTERNAL_ERROR | Unexpected server error |

## Redeem Coupon

| HTTP Status | Error | Description |
|------------|---------|-------------|
| 200 OK | - | Coupon redeemed successfully |
| 400 Bad Request | VALIDATION_ERROR | Request validation failed |
| 403 Forbidden | COUNTRY_NOT_ALLOWED | User country is not eligible for the coupon |
| 403 Forbidden | COUNTRY_UNKNOWN | User country could not be determined |
| 404 Not Found | NOT_FOUND | Coupon not found |
| 409 Conflict | COUPON_ALREADY_USED | Coupon already used by this user |
| 410 Gone | COUPON_LIMIT_REACHED | Coupon usage limit reached |
| 500 Internal Server Error | INTERNAL_ERROR | Unexpected server error |

---

# Integration Tests

Integration tests use Testcontainers with PostgreSQL.

Each test execution starts an isolated PostgreSQL container, ensuring tests run against a real PostgreSQL instance instead of mocks or in-memory databases.

Benefits:

- production-like behavior
- deterministic execution
- environment-independent tests
- verification of JPA mappings and SQL queries
- testing against the same database engine used in production

Run tests:

```bash
mvn test
```

---

# Architecture Decisions

## Environment Configuration

The application uses Spring Profiles (`local`, `test`, `production`, etc.) to separate environment-specific configuration.

This allows the same application artifact to be deployed across multiple environments while keeping configuration externalized and production-ready.

---

## Database Versioning

Database schema creation and versioning are managed with Liquibase.

Benefits:

- version-controlled schema changes
- repeatable deployments
- migration history tracking
- consistent database structure across environments

---

## BaseEntity

A shared `BaseEntity` centralizes common persistence fields:

- `id`
- `version`
- `createdAt`
- `updatedAt`

This reduces duplication and guarantees consistent auditing behavior across entities.

The `version` field enables optimistic locking and allows detection of concurrent modifications.

---

## Country Representation

Each coupon is currently assigned to a single country represented by a Java enum.

Advantages:

- compile-time type safety
- prevention of invalid values
- simplified validation logic
- clean and explicit domain model

Current limitations:

- adding a new country requires a code change and redeployment
- a coupon can be assigned to only one country

Possible future alternatives:

- configurable country definitions loaded from application properties
- dedicated database table managed through API or administration tools
- support for multiple countries per coupon using a collection-based relationship

---

## Coupon Code Normalization

Coupon codes are normalized to uppercase before validation and persistence.

As a result:

- `testcoupon`
- `TESTCOUPON`
- `TestCoupon`

are treated as the same coupon.

This eliminates case-sensitivity issues and prevents duplicate coupon definitions.

---

## Coupon Uniqueness

Coupon code uniqueness is enforced at the database level using a unique constraint.

This guarantees consistency even under concurrent requests and prevents race conditions that could bypass application-level validation.

---

## Data Integrity

Business invariants are enforced both at the application and database level.

Examples:

- unique coupon code constraint
- unique coupon usage per user
- optimistic locking through entity versioning

Database constraints act as the final safety net and protect data integrity even under concurrent access.

---

## Coupon Redemption Rules

A coupon can be redeemed when:

- the coupon exists
- the usage limit has not been exceeded
- the user's resolved country matches the coupon country

If geolocation cannot determine the country, the service returns an empty result
and the request is rejected with `403 Forbidden`.

---

## Concurrency and Thread Safety

Coupon redemption is designed to be thread-safe.

Instead of pessimistic locking (`SELECT FOR UPDATE`), usage counters are updated using a single atomic SQL statement:

```sql
UPDATE coupon
SET usage_count = usage_count + 1
WHERE code = :code
  AND usage_count < usage_limit;
```

Benefits:

- no read-modify-write race condition
- database-level consistency guarantees
- minimal locking overhead
- better scalability under concurrent load

Because validation and increment occur within the same statement, there is no timing window where another transaction can invalidate the result.

---

## Transaction Management

Business operations are executed within transactional boundaries using `@Transactional`.

If any step fails during coupon redemption, the entire transaction is rolled back automatically.

This guarantees data consistency and prevents partial updates.

---

## Client IP Resolution

The client IP address is resolved from the incoming HTTP request and used for geolocation.

When the application runs behind a reverse proxy or load balancer, the real client IP is read
from the `X-Forwarded-For` header instead of the TCP connection address.

The `X-Forwarded-For` header can contain a comma-separated list of IPs added by each proxy in the chain:

```
X-Forwarded-For: <client>, <proxy1>, <proxy2>
```

The application always takes the **first entry**, which represents the original client IP.

If the header is absent or blank, the application falls back to `HttpServletRequest.getRemoteAddr()`,
which returns the direct TCP connection address.

---

## Geolocation Resilience

Country resolution depends on an external service (`ip-api.com`).

To improve resilience:

- retry is enabled for network-related failures and timeouts (`ResourceAccessException`)
- up to 3 attempts are made before giving up
- a short wait between attempts avoids hammering an unhealthy service
- after all attempts are exhausted the fallback returns an empty result, which rejects the request with `403 Forbidden`

This prevents temporary external service outages from silently bypassing country restrictions.

---

## Fail-Closed Strategy for Unresolvable Country

When geolocation cannot determine the country (for example because of service outage,
localhost requests or private IP addresses), the service returns an empty result
and the request is rejected with `403 Forbidden`.

Loopback (`127.0.0.1`, `::1`) and private IP addresses are detected before calling the external geolocation provider and are rejected without performing a GeoIP lookup.

Advantages:

- prevents users from bypassing country restrictions during outages
- enforces strict country-based access control
- consistent and predictable behavior regardless of geolocation availability

In environments where availability is prioritized over strict enforcement, this behavior
could be changed to fail-open and allow requests when the country cannot be determined.

---

## Future Improvements

Potential future enhancements:

- Circuit Breaker for geolocation integration
- support for multiple countries per coupon
- distributed caching for frequently resolved geolocation results
