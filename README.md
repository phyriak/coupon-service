# Coupon Service

REST API for creating and redeeming coupons with usage limits and country-based restrictions.

The application validates coupon eligibility based on:

- coupon existence
- usage limit
- user country resolved from IP address

Access to the API is protected with API-key authentication, and outbound
geolocation calls are made resilient with retry, a circuit breaker, and a
short-lived cache.

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

Configuration for local development is stored in `application-local.yaml`.

## Required environment variables

The application reads the following from the environment (a `.env` template is
provided as `.env.example`). Note that Spring Boot does **not** read `.env`
automatically — when running from an IDE, add these to the run configuration's
environment variables; `docker compose` reads `.env` only for the database
container.

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `LOGGING_PEPPER` | secret pepper for log pseudonymization |
| `COUPON_ADMIN_KEY_HASH` | SHA-256 hash of the admin API key |
| `COUPON_REDEEM_KEY_HASH` | SHA-256 hash of the redeem API key |

The API-key variables must contain the **SHA-256 hash** of the key (a 64-character
hex string), never the raw key. See [Authentication](#authentication).

---

# Running PostgreSQL with Docker

Docker Compose is used only for running PostgreSQL.

```bash
cp .env.example .env
docker compose up -d      # start
docker compose down       # stop
```

After the database is running, start the Spring Boot application locally.

---

# API Documentation

Swagger UI is available at:

```text
http://localhost:8090/swagger-ui/index.html
```

---

# Authentication

All endpoints except `/actuator/health` require authentication via an API key
passed in the `X-Api-Key` request header. Authorization is role-based:

| Endpoint | Required authority |
|----------|--------------------|
| `POST /api/v1/coupons` (create) | `COUPON_ADMIN` |
| `POST /api/v1/coupons/use` (redeem) | `COUPON_REDEEM` |
| `GET /actuator/health` | none (public) |
| anything else | denied |

A request with no key, or a key whose hash matches no configured entry, is
rejected before reaching the controller. The default authorization rule is
deny-all, so any endpoint not explicitly permitted is closed by default.

## How keys are configured

Keys are defined as a list under `security.api-keys`. Each entry holds the
**SHA-256 hash** of a key plus the authorities it grants:

```yaml
security:
  api-keys:
    - hash: ${COUPON_ADMIN_KEY_HASH}
      roles:
        - COUPON_ADMIN
        - COUPON_REDEEM      # admin may also redeem
    - hash: ${COUPON_REDEEM_KEY_HASH}
      roles:
        - COUPON_REDEEM
```

At startup the application builds a `hash -> roles` lookup. On each request it
computes the SHA-256 of the presented `X-Api-Key` and looks it up; a match
establishes an authenticated principal carrying the configured authorities.

Design notes:

- **Only hashes are stored in configuration.** The raw key never appears on the
  server, so a leaked configuration file does not leak usable credentials.
- **Authorities use `hasAuthority`, not `hasRole`.** The authority strings in the
  `roles` list, the authority granted by the filter, and the rule in the security
  configuration are all the same literal (`COUPON_ADMIN`, `COUPON_REDEEM`) — there
  is no implicit `ROLE_` prefix.
- **Duplicate hashes fail fast.** Two entries with the same hash cause a clear
  startup failure rather than silently merging.

## Calling a secured endpoint

```bash
curl -X POST http://localhost:8090/api/v1/coupons/use \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: <raw-redeem-key>" \
  -H "X-Forwarded-For: 8.8.8.8" \
  -d '{ "code": "TESTCOUPON", "userId": "user-123" }'
```

## Generating keys

For anything beyond local testing, use strong random keys and store only their
hashes in the environment:

```bash
RAW_KEY=$(openssl rand -hex 32)        # give this to the client (secrets manager)
echo -n "$RAW_KEY" | sha256sum         # put this hash in COUPON_*_KEY_HASH
```

The weak placeholder keys used during local development must not be used in any
deployed environment.

---

# API Endpoints

## Create Coupon

```http
POST /api/v1/coupons
Content-Type: application/json
X-Api-Key: <admin-key>
```

```json
{ "code": "TESTCOUPON", "usageLimit": 100, "country": "PL" }
```

Response `201 Created`:

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

## Redeem Coupon

```http
POST /api/v1/coupons/use
Content-Type: application/json
X-Api-Key: <redeem-key>
```

```json
{ "code": "TESTCOUPON", "userId": "user-123" }
```

Response `200 OK`:

```json
{ "code": "TESTCOUPON", "userId": "user-123", "message": "Coupon applied successfully" }
```

---

## Error Handling

The application uses centralized exception handling implemented with
`@RestControllerAdvice`, producing a unified error format:

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Coupon code already exists",
  "timestamp": "2025-05-27T12:00:00Z"
}
```

# Error Responses

## Create Coupon

| HTTP Status | Error | Description |
|------------|---------|-------------|
| 201 Created | - | Coupon created successfully |
| 400 Bad Request | VALIDATION_ERROR | Request validation failed |
| 401 Unauthorized / 403 Forbidden | - | Missing or invalid API key, or insufficient authority |
| 409 Conflict | CONFLICT | Coupon code already exists |
| 500 Internal Server Error | INTERNAL_ERROR | Unexpected server error |

## Redeem Coupon

| HTTP Status | Error | Description |
|------------|---------|-------------|
| 200 OK | - | Coupon redeemed successfully |
| 400 Bad Request | VALIDATION_ERROR | Request validation failed |
| 401 Unauthorized / 403 Forbidden | - | Missing or invalid API key, or insufficient authority |
| 403 Forbidden | COUNTRY_NOT_ALLOWED | User country is not eligible for the coupon |
| 404 Not Found | NOT_FOUND | Coupon not found |
| 409 Conflict | COUPON_ALREADY_USED | Coupon already used by this user |
| 410 Gone | COUPON_LIMIT_REACHED | Coupon usage limit reached |
| 503 Service Unavailable | COUNTRY_RESOLUTION_FAILED | Country could not be determined (geolocation unavailable, private/loopback IP with no configured default, or circuit breaker open) |
| 500 Internal Server Error | INTERNAL_ERROR | Unexpected server error |

Note that an authentication/authorization 403 is produced by the security layer
and has an empty body, whereas a `COUNTRY_NOT_ALLOWED` 403 comes from the
application and carries the JSON error body above.

---

# Integration Tests

Integration tests use Testcontainers with PostgreSQL, so each run executes
against a real PostgreSQL instance rather than mocks or an in-memory database.

The external geolocation provider is simulated with **MockWebServer**, allowing
tests to cover timeout handling, retry behaviour, fallback invocation, circuit
breaker short-circuiting and cache behaviour without calling the real
`ip-api.com` service.

Stateful components held in the shared Spring context are reset before each test:

- circuit breakers (`CircuitBreakerRegistry.reset()`), so failures recorded in
  one test cannot open the circuit in another
- the geolocation cache, so a cached entry cannot make a later test pass for the
  wrong reason

Controller-slice tests (`@WebMvcTest`) run with security replaced by a permit-all
test configuration so they exercise controller and exception-handler behaviour in
isolation; authentication is covered separately by a full-context test.

Run tests:

```bash
mvn test
```

---

# Architecture Decisions

## Environment Configuration

The application uses Spring Profiles (`local`, `test`, `production`, etc.) to
separate environment-specific configuration, allowing one artifact to be deployed
across environments while keeping configuration externalized.

## Database Versioning

Schema creation and versioning are managed with Liquibase, giving
version-controlled, repeatable, history-tracked migrations.

## BaseEntity

A shared `BaseEntity` centralizes `id`, `version`, `createdAt`, and `updatedAt`.
The `version` field enables optimistic locking.

## Country Representation

Each coupon is assigned a single country represented by a Java enum, giving
compile-time safety and simple validation, at the cost of needing a code change
to add a country.

## Coupon Code Normalization

Coupon codes are normalized to uppercase before validation and persistence, so
`testcoupon`, `TESTCOUPON`, and `TestCoupon` are the same coupon.

## Coupon Uniqueness and Data Integrity

Uniqueness (coupon code; one usage per user) is enforced with database
constraints in addition to application checks, so invariants hold even under
concurrent access. Optimistic locking via entity versioning detects concurrent
modification.

## Concurrency and Thread Safety

Usage counters are updated with a single atomic SQL statement rather than
pessimistic locking:

```sql
UPDATE coupon
SET usage_count = usage_count + 1
WHERE code = :code
  AND usage_count < usage_limit;
```

Because validation and increment occur in the same statement, there is no timing
window in which another transaction can invalidate the result.

## Transaction Management

Business operations run within `@Transactional` boundaries; a failure rolls the
whole operation back. The external geolocation call is performed **outside** the
database transaction, so slow or failing HTTP calls never hold a database
connection.

---

## Authentication and Authorization

See [Authentication](#authentication) for configuration. The security model is
stateless (no sessions, CSRF disabled), uses a custom `X-Api-Key` filter that
establishes authorities from a hash lookup, and applies role-based rules with a
deny-all default. Only key hashes are stored server-side.

---

## Client IP Resolution Policy

The client IP used for geolocation is resolved from the incoming request. Because
the `X-Forwarded-For` header is client-controlled, it is only trusted under
specific conditions:

- The header is honoured **only when the direct connection comes from a trusted
  proxy** (`ip-resolution.trusted-proxies`, matched with proper CIDR semantics
  for both IPv4 and IPv6). A client connecting directly cannot spoof its country
  by setting the header — the header is ignored and the real connection address
  is used.
- When honoured, the chain is walked from the right, skipping trusted proxies, to
  find the first untrusted hop (the real client). The value must be a valid IP
  literal; a non-IP value (e.g. a hostname) is rejected so that attacker-supplied
  input is never passed to a DNS lookup.
- If there is no trusted proxy in front, or no header, the direct connection
  address (`getRemoteAddr()`) is used.

```yaml
ip-resolution:
  trust-forwarded-headers: true
  trusted-proxies:
    - 10.0.0.0/8        # load balancer / ingress range
    - 127.0.0.1/32      # IPv4 loopback (local testing)
    - ::1               # IPv6 loopback (local testing)
```

> **Operational requirement:** trust only proxy addresses you actually control.
> In production this list should contain your load balancer / ingress range. The
> loopback entries exist so that local testing — sending `X-Forwarded-For` from
> `curl`/Postman on the same host — works regardless of whether the connection
> uses IPv4 or IPv6. Never add a public range to this list.

### Private and loopback addresses

Loopback (`127.0.0.1`, `::1`) and private/site-local addresses are detected
**before** any external lookup and never sent to the geolocation provider. What
happens next depends on configuration:

- If `geolocation.default-country-for-private-ip` is **set**, that country is
  returned for private/loopback IPs. This is used **only in `local`** (set to
  `PL` in `application-local.yaml`) so that redemption can be exercised locally
  without a public IP.
- If it is **unset** (the default, and the case in all non-local environments),
  private/loopback resolution returns no country and the request is rejected with
  `503 COUNTRY_RESOLUTION_FAILED` — i.e. **fail-closed**.

This keeps the strict, fail-closed behaviour in deployed environments while
allowing a deliberate, profile-scoped convenience for local development. Because
the default is a typed `Country`, an invalid value in configuration fails at
startup.

---

## Geolocation Resilience

Country resolution depends on the external `ip-api.com` service and is wrapped
with two Resilience4j decorators plus a cache:

```java
@Cacheable(cacheNames = "geoByIp", unless = "#result == null")
@Retry(name = "geoLocation", fallbackMethod = "resolveFallback")
@CircuitBreaker(name = "geoLocation")
public Optional<Country> resolveCountry(String ipAddress) { ... }
```

With the default Resilience4j aspect order, Retry is the outermost of the two
resilience decorators (`Retry(CircuitBreaker(call))`), so the fallback is declared
on `@Retry` and is only invoked once all attempts are exhausted; each individual
attempt is recorded by the circuit breaker.

### Retry

- up to 3 attempts for network failures/timeouts (`ResourceAccessException`)
- short wait between attempts; 1-second connect/read timeouts bound each attempt

### Circuit Breaker

- a count-based sliding window tracks recent failures and slow calls
- when thresholds are exceeded the circuit opens and calls fail immediately
  (`CallNotPermittedException`) without an HTTP request or a blocked thread
- `CallNotPermittedException` is not retried and goes straight to the fallback
- the circuit transitions automatically to half-open after a wait period

```yaml
resilience4j:
  circuitbreaker:
    instances:
      geoLocation:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-duration-threshold: 800ms
        slow-call-rate-threshold: 80
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
  retry:
    instances:
      geoLocation:
        max-attempts: 3
        wait-duration: 200ms
        retry-exceptions:
          - org.springframework.web.client.ResourceAccessException
```

In all failure scenarios (exhausted retries or open circuit) the fallback returns
an empty result, and the request is rejected with `503` — preventing a transient
outage from silently bypassing country restrictions.

---

## Caching

Resolved countries are cached in-memory with **Caffeine** to reduce calls to
`ip-api.com` and stay within its rate limits. The cache key is the IP address.

```yaml
spring:
  cache:
    cache-names: geoByIp
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=1h
```

> Note: this block must be nested under `spring:` (i.e. `spring.cache.*`). If it
> is placed elsewhere, Spring Boot ignores it and falls back to an unbounded cache
> with no TTL — caching still appears to work, but `maximumSize`/`expireAfterWrite`
> are silently dropped.

Behaviour and rationale:

- **Successful resolutions are cached; failures are not.** The
  `unless = "#result == null"` guard keeps empty results out of the cache (Spring
  unwraps the `Optional`, so an empty result is `null` in the expression). Without
  this, a transient outage would be frozen for the cache TTL and turn a brief blip
  into sustained failures.
- **Bounded and expiring.** `maximumSize` caps memory and bounds the impact of
  many distinct keys; `expireAfterWrite` ensures geolocation data does not go
  stale indefinitely. (Caffeine must be on the classpath for these to take effect.)
- A cache hit short-circuits the external call entirely, so repeat redemptions
  from the same IP do not contact the provider.

---

## Log Anonymization

User identifiers and IP addresses are pseudonymized in logs using SHA-256 with an
application-specific secret (pepper). The geolocation client logs only the
anonymized IP, never the raw value or the full provider response.

---

## Future Improvements

- rate limiting on redemption to further blunt coupon-code enumeration
- circuit breaker state exposed via an Actuator health indicator
- distributed cache (e.g. Redis) if the service is scaled to multiple instances
- support for multiple countries per coupon
