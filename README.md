# Sports Field Booking System

A Spring Boot 3.x REST API for managing sports fields and reservations.
JWT-authenticated, MySQL-backed, with weather-based booking validation for outdoor fields.

---

## Tech stack

| Layer | Choice |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Build | Maven |
| Database | MySQL 8.3 (Dockerized) |
| Auth | Spring Security + JWT (JJWT 0.12.x) |
| HTTP client | Spring RestClient (replaces RestTemplate) |
| Docs | Springdoc OpenAPI 2.x / Swagger UI |
| Tests | JUnit 5 + Mockito |

---

## Prerequisites

- Docker & Docker Compose
- JDK 17+
- Maven 3.8+

---

## 1. Start MySQL with Docker

```bash
docker-compose up -d
```

This starts a MySQL 8.3 container on port **3306** with:
- Database: `sportsbooking`
- User: `sportsuser` / Password: `sportspass`

Verify it's healthy:

```bash
docker-compose ps
```

---

## 2. Run the application

```bash
mvn spring-boot:run
```

Or build a jar and run it:

```bash
mvn clean package -DskipTests
java -jar target/sports-field-booking-1.0.0.jar
```

On first startup a default admin user is created automatically:
- **username**: `admin`
- **password**: `admin123`

> Change this password before deploying to any non-local environment.

---

## 3. Explore the API

Swagger UI: **http://localhost:8080/swagger-ui.html**

1. Use the login endpoint to get a token.
2. Click **Authorize** in Swagger UI, paste `<token>` (not `Bearer <token>` — Swagger adds the prefix).
3. All protected endpoints are now unlocked.

---

## 4. Sample curl requests

### 4.1 Login → get JWT

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq .
```

Copy the `token` value from the response. Use it as `TOKEN` below.

```bash
TOKEN="<paste token here>"
```

---

### 4.2 Create a field (outdoor soccer)

```bash
curl -s -X POST http://localhost:8080/api/fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Stade Anfa",
    "sportType": "SOCCER",
    "indoor": false,
    "pricePerHour": 150.00,
    "latitude": 33.5731,
    "longitude": -7.5898
  }' | jq .
```

### 4.3 Create an indoor basketball court

```bash
curl -s -X POST http://localhost:8080/api/fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Salle Omnisports",
    "sportType": "BASKETBALL",
    "indoor": true,
    "pricePerHour": 200.00
  }' | jq .
```

### 4.4 List all fields

```bash
curl -s http://localhost:8080/api/fields \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

### 4.5 Create a reservation

Replace `FIELD_ID` and `DATE` with real values.

```bash
curl -s -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fieldId": 1,
    "clientName": "Youssef Alami",
    "clientPhone": "+212 6 12 34 56 78",
    "clientEmail": "youssef@example.com",
    "date": "2026-09-20",
    "startHour": 9,
    "endHour": 11
  }' | jq .
```

**Expected on success**: HTTP 201 with the confirmed reservation.

**Expected on overlap**: HTTP 409 — `"Time slot 9:00-11:00 on 2026-09-20 conflicts with an existing reservation..."`

**Expected on cold weather** (outdoor field, temp ≤ 10°C): HTTP 409 — `"Outdoor field booking rejected: forecast temperature..."`

---

### 4.6 Deliberately trigger a double-booking (debugging step 3)

Run the same reservation request from 4.5 a second time — you should get:

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Time slot 9:00-11:00 on 2026-09-20 conflicts with an existing reservation (9:00-11:00)."
}
```

---

### 4.7 List reservations for a field on a date

```bash
curl -s "http://localhost:8080/api/reservations?fieldId=1&date=2026-09-20" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 4.8 Get a single reservation

```bash
curl -s http://localhost:8080/api/reservations/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

### 4.9 Test unauthenticated access (should 401)

```bash
curl -s http://localhost:8080/api/fields
# → {"status":401,"error":"UNAUTHORIZED",...}
```

---

## 5. Business rules reference

### Overlap detection

A new reservation [newStart, newEnd) on the same field+date conflicts with an existing
CONFIRMED reservation [existStart, existEnd) when:

```
existStart < newEnd  AND  existEnd > newStart
```

Adjacent slots (newEnd == existStart, or newStart == existEnd) are **allowed**.

### Weather check (outdoor fields only)

Before confirming a reservation on an outdoor field, the API calls
[Open-Meteo](https://open-meteo.com) for the field's latitude/longitude on the
reservation date. If the forecast maximum temperature is **≤ 10°C**, the booking
is rejected with HTTP 409.

If the weather service is unreachable, the booking is also rejected (fail-safe).

---

## 6. Project structure

```
src/main/java/com/sportsbooking/
├── SportsFieldBookingApplication.java
├── config/
│   ├── DataInitializer.java      # Seeds default admin on first boot
│   ├── OpenApiConfig.java        # Swagger bearerAuth scheme
│   └── SecurityConfig.java       # JWT stateless security chain
├── controller/
│   ├── AuthController.java
│   ├── FieldController.java
│   └── ReservationController.java
├── dto/                          # Java records (request + response)
├── entity/                       # JPA entities (User, Field, Reservation)
├── exception/                    # Custom exceptions + GlobalExceptionHandler
├── repository/                   # Spring Data JPA repositories
├── security/                     # JwtUtils, JwtAuthenticationFilter
├── service/                      # Business logic
└── weather/                      # WeatherClient + OpenMeteoWeatherClient
```

---

## 7. Running the tests

```bash
mvn test
```

Key test classes:
- `ReservationOverlapTest` — 9 cases covering all overlap/adjacent scenarios
- `ReservationWeatherTest` — 7 cases covering temperature thresholds + fail-safe

---

## 8. Debugging guide

1. **DB first** — get `docker-compose up -d` healthy before touching the app.
2. **Comment out security** — in `SecurityConfig`, temporarily change `.anyRequest().hasRole("ADMIN")` to `.anyRequest().permitAll()` and test all CRUD without auth.
3. **Double-book a field** — two identical `POST /api/reservations` requests. Confirm the second returns 409.
4. **Weather check** — pick a future date, check Open-Meteo directly: `curl "https://api.open-meteo.com/v1/forecast?latitude=33.5731&longitude=-7.5898&daily=temperature_2m_max&start_date=2026-09-20&end_date=2026-09-20&timezone=auto"` — compare temp against what the API rejects.
5. **Re-enable security** — trace `JwtAuthenticationFilter` line by line: request without token → filter passes, Security chain rejects → 401. Request with token → filter sets auth → passes.
