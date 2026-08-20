# Sports Field Booking System — Project Overview

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT (Swagger UI / curl)                  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP + JWT Bearer Token
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     SPRING SECURITY FILTER CHAIN                    │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │  JwtAuthenticationFilter                                       │ │
│  │  • Extracts JWT from Authorization header                     │ │
│  │  • Validates signature + expiry                               │ │
│  │  • Sets SecurityContext for downstream authorization          │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
                ▼                   ▼                   ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │ AuthController   │ │ FieldController  │ │ReservationCtrl   │
    │                  │ │                  │ │                  │
    │ POST /login      │ │ GET    /fields   │ │ POST /reservations│
    │                  │ │ POST   /fields   │ │ GET  /reservations│
    │                  │ │ PUT    /fields/:id│ │ GET  /reservations/:id│
    │                  │ │ DELETE /fields/:id│ │                  │
    └──────────────────┘ └──────────────────┘ └──────────────────┘
            │                     │                      │
            │                     │                      │
            ▼                     ▼                      ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │ JwtUtils         │ │ FieldService     │ │ReservationService│
    │                  │ │                  │ │                  │
    │ • Generate token │ │ • CRUD ops       │ │ • Create + validate│
    │ • Validate token │ │ • Logging        │ │ • checkOverlap() │
    │                  │ │                  │ │ • checkWeather() │
    └──────────────────┘ └──────────────────┘ └──────────────────┘
            │                     │                      │
            │                     │           ┌──────────┴──────────┐
            │                     │           │                     │
            ▼                     ▼           ▼                     ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │ UserRepository   │ │ FieldRepository  │ │ReservationRepo   │ │ WeatherClient    │
    │                  │ │                  │ │                  │ │(OpenMeteo REST)  │
    │ JPA / Hibernate  │ │ JPA / Hibernate  │ │ JPQL overlap     │ │                  │
    └──────────────────┘ └──────────────────┘ └──────────────────┘ └──────────────────┘
            │                     │                      │                     │
            └─────────────────────┴──────────────────────┘                     │
                                  │                                            │
                                  ▼                                            ▼
                    ┌──────────────────────────┐              ┌──────────────────────────┐
                    │   MySQL 8.3 (Docker)     │              │ api.open-meteo.com       │
                    │                          │              │                          │
                    │  • users                 │              │ GET /v1/forecast         │
                    │  • fields                │              │   ?lat=&lon=&date=       │
                    │  • reservations          │              │   &daily=temperature_2m_max│
                    └──────────────────────────┘              └──────────────────────────┘
```

---

## 🔐 Security Flow

```
1. User → POST /api/auth/login {username, password}
              ↓
2. AuthenticationManager validates credentials (BCrypt hash check)
              ↓
3. JwtUtils.generateToken() → returns JWT (exp: 24h)
              ↓
4. User includes token in subsequent requests: Authorization: Bearer <token>
              ↓
5. JwtAuthenticationFilter intercepts:
   • Extracts token
   • Validates signature + expiry
   • Loads UserDetails from DB
   • Sets SecurityContext
              ↓
6. SecurityConfig authorizes: .anyRequest().hasRole("ADMIN")
              ↓
7. Request proceeds to controller
```

---

## 📋 Business Logic: Reservation Creation

```
POST /api/reservations
    {
      "fieldId": 1,
      "date": "2026-09-20",
      "startHour": 9,
      "endHour": 11,
      ...
    }
              ↓
┌───────────────────────────────────────────────────────────────┐
│  ReservationService.create()                                  │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Validate: startHour < endHour                            │
│     → IllegalArgumentException if not                         │
│                                                               │
│  2. Load Field from DB                                        │
│     → ResourceNotFoundException if missing                    │
│                                                               │
│  3. checkOverlap(fieldId, date, startHour, endHour)          │
│     ┌─────────────────────────────────────────────────────┐ │
│     │ JPQL: SELECT * FROM reservations WHERE               │ │
│     │   field_id = ?                                       │ │
│     │   AND date = ?                                       │ │
│     │   AND status = CONFIRMED                             │ │
│     │   AND startHour < ? [newEnd]                         │ │
│     │   AND endHour > ? [newStart]                         │ │
│     └─────────────────────────────────────────────────────┘ │
│     → If conflicts.isEmpty() → OK                            │
│     → Else → ReservationConflictException (HTTP 409)         │
│                                                               │
│  4. IF field.indoor == false:                                │
│       checkWeather(field, date)                              │
│     ┌─────────────────────────────────────────────────────┐ │
│     │ RestClient GET https://api.open-meteo.com/v1/forecast│ │
│     │   ?latitude={field.lat}                              │ │
│     │   &longitude={field.lon}                             │ │
│     │   &daily=temperature_2m_max                          │ │
│     │   &start_date={date}                                 │ │
│     │   &end_date={date}                                   │ │
│     │                                                       │ │
│     │ Parse response.daily.temperature_2m_max[0]           │ │
│     │                                                       │ │
│     │ IF temp <= 10.0°C:                                   │ │
│     │   → ReservationConflictException (HTTP 409)          │ │
│     │ ELSE:                                                │ │
│     │   → OK                                               │ │
│     └─────────────────────────────────────────────────────┘ │
│                                                               │
│  5. Save Reservation with status=CONFIRMED                   │
│     → Return HTTP 201 + ReservationResponse                  │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## 🧪 Test Coverage

### ReservationOverlapTest (9 cases)

| Case | Existing | New | Expected |
|------|----------|-----|----------|
| 1. Partial overlap start | `[9─11]` | `{10──13}` | ❌ 409 CONFLICT |
| 2. New wraps existing | `[10─11]` | `{9────12}` | ❌ 409 CONFLICT |
| 3. Partial overlap end | `[9───12]` | `{10───14}` | ❌ 409 CONFLICT |
| 4. Exact same slot | `[9─11]` | `{9─11}` | ❌ 409 CONFLICT |
| 5. New before existing | `[12─14]` | `{9─11}` | ✅ 201 CREATED |
| 6. New after existing | `[9─11]` | `{14─16}` | ✅ 201 CREATED |
| 7. Adjacent (new ends at existing start) | `[11─13]` | `{9─11}` | ✅ 201 CREATED |
| 8. Adjacent (new starts at existing end) | `[9─11]` | `{11─13}` | ✅ 201 CREATED |
| 9. Empty conflict list | `(none)` | `{8─10}` | ✅ 201 CREATED |

### ReservationWeatherTest (8 cases)

| Case | Forecast Temp | Expected |
|------|---------------|----------|
| 1. Exactly at threshold | `10.0°C` | ❌ 409 CONFLICT |
| 2. Below threshold | `2.5°C` | ❌ 409 CONFLICT |
| 3. Just below threshold | `9.9°C` | ❌ 409 CONFLICT |
| 4. Just above threshold | `10.1°C` | ✅ 201 CREATED |
| 5. Good weather | `25.0°C` | ✅ 201 CREATED |
| 6. Weather service unavailable | `(exception)` | ❌ 409 CONFLICT (fail-safe) |
| 7. Message includes date+temp | `5.0°C` | ✅ Message validated |
| 8. Correct coordinates sent | - | ✅ Coordinates verified |

---

## 📁 File Inventory

**Total: 38 files (34 .java + 4 config/docs)**

```
src/main/java/com/sportsbooking/
├── SportsFieldBookingApplication.java ········· Main entry point
│
├── config/ (4 files)
│   ├── DataInitializer.java ················· Seeds admin user on startup
│   ├── OpenApiConfig.java ··················· Swagger bearerAuth scheme
│   └── SecurityConfig.java ··················· JWT stateless security chain
│
├── controller/ (3 files)
│   ├── AuthController.java ··················· POST /api/auth/login
│   ├── FieldController.java ·················· CRUD /api/fields
│   └── ReservationController.java ············ POST /api/reservations, GET
│
├── dto/ (7 files) ····························· All Java records
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── FieldRequest.java
│   ├── FieldResponse.java
│   ├── ReservationRequest.java
│   ├── ReservationResponse.java
│   └── ErrorResponse.java ····················· Standard error envelope
│
├── entity/ (3 files)
│   ├── User.java ····························· BCrypt password, ADMIN role
│   ├── Field.java ···························· lat/lon for weather, SportType enum
│   └── Reservation.java ······················ startHour/endHour, Status enum
│
├── exception/ (3 files)
│   ├── GlobalExceptionHandler.java ············ @RestControllerAdvice
│   ├── ResourceNotFoundException.java ········· 404
│   └── ReservationConflictException.java ······ 409
│
├── repository/ (3 files)
│   ├── UserRepository.java
│   ├── FieldRepository.java
│   └── ReservationRepository.java ············· JPQL overlap query
│
├── security/ (2 files)
│   ├── JwtUtils.java ························· Token generation + validation
│   └── JwtAuthenticationFilter.java ··········· OncePerRequestFilter
│
├── service/ (3 files)
│   ├── FieldService.java ····················· CRUD + logging
│   ├── ReservationService.java ················ Overlap + weather checks
│   └── UserDetailsServiceImpl.java
│
└── weather/ (4 files)
    ├── WeatherClient.java ····················· Interface
    ├── OpenMeteoWeatherClient.java ············ RestClient implementation
    ├── WeatherResponse.java ··················· JSON mapping
    └── WeatherServiceException.java

src/test/java/com/sportsbooking/service/
├── ReservationOverlapTest.java ················ 9 overlap test cases
└── ReservationWeatherTest.java ················ 8 weather test cases

Configuration:
├── pom.xml ···································· Spring Boot 3.2.5, Java 17
├── docker-compose.yml ························· MySQL 8.3
├── README.md ·································· Setup + debugging guide
└── requests.http ·························· 22 ready-to-run HTTP requests
```

---

## 🚀 Quick Start Commands

```bash
# 1. Start database
docker-compose up -d

# 2. Run app
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run

# 3. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token

# 4. Use token (replace $TOKEN)
curl http://localhost:8080/api/fields \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🎯 Key Features Implemented

✅ **JWT stateless authentication** — no sessions, no cookies  
✅ **BCrypt password hashing** — never stores plaintext  
✅ **Overlap detection** — JPQL query in the database  
✅ **Weather integration** — Open-Meteo REST API via RestClient  
✅ **Fail-safe deny** — if weather service is down, reject outdoor bookings  
✅ **Comprehensive error handling** — 400, 401, 404, 409, 500 with JSON envelopes  
✅ **Swagger/OpenAPI docs** — interactive testing at `/swagger-ui.html`  
✅ **Unit tests** — 17 tests covering both core business rules  
✅ **Logging** — SLF4J at INFO level for field creation, reservations, login attempts  
✅ **Validation** — Bean Validation (JSR-380) on all DTOs  
✅ **Java records** — all DTOs are immutable records  

---

## 🐛 Debugging Checklist (from README)

1. ✅ **Docker MySQL** — `docker-compose ps` shows healthy
2. ✅ **Security off** — temporarily `.anyRequest().permitAll()` to test CRUD
3. ✅ **Double-booking** — POST same reservation twice → second returns 409
4. ✅ **Weather** — compare Open-Meteo direct curl vs. API rejection
5. ✅ **Security on** — trace JwtAuthenticationFilter line by line

---

## 📊 Database Schema

```sql
-- Auto-generated by Hibernate (ddl-auto=update)

users
  ├── id (PK, auto-increment)
  ├── username (unique, not null)
  ├── password (BCrypt hash, not null)
  └── role (VARCHAR, not null) -- always 'ADMIN'

fields
  ├── id (PK, auto-increment)
  ├── name (VARCHAR(100), not null)
  ├── sport_type (VARCHAR(20), not null) -- SOCCER/BASKETBALL/TENNIS
  ├── indoor (BOOLEAN, not null)
  ├── price_per_hour (DECIMAL(10,2), not null)
  ├── latitude (DOUBLE, not null)
  └── longitude (DOUBLE, not null)

reservations
  ├── id (PK, auto-increment)
  ├── field_id (FK → fields.id, not null)
  ├── client_name (VARCHAR(100), not null)
  ├── client_phone (VARCHAR(20), not null)
  ├── client_email (VARCHAR(100), not null)
  ├── date (DATE, not null)
  ├── start_hour (INT, not null) -- 0-23
  ├── end_hour (INT, not null)   -- 1-24
  ├── status (VARCHAR(20), not null) -- PENDING/CONFIRMED/REJECTED
  ├── created_at (TIMESTAMP, auto-generated)
  └── INDEX idx_field_date (field_id, date) -- for overlap query performance
```

---

## 🔧 Technology Choices Explained

| Choice | Why? |
|--------|------|
| **RestClient over HttpExchange** | Weather URL is assembled at runtime (lat/lon/date vary per field). HttpExchange requires fixed URL templates at compile-time. RestClient's fluent builder is cleaner for dynamic URLs. |
| **JPQL overlap query** | Runs the condition in the DB, not in-memory Java. Uses the composite index on (field_id, date) for fast filtering. |
| **Fail-safe weather deny** | If Open-Meteo is unreachable, reject outdoor bookings rather than silently allow them. This is safer and explicit. |
| **Java records for DTOs** | Immutable, concise, no Lombok needed. Built-in equals/hashCode/toString. |
| **Spring Security stateless** | No HttpSession. JWT in every request. Scales horizontally without sticky sessions. |
| **BCrypt** | Industry-standard password hashing. Adaptive cost factor. Secure against rainbow tables. |

---

**Project ready to run.** Open `README.md` for detailed setup instructions.
