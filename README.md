# MedXSolution - Doctor Appointment Booking System

## Project Overview

MedXSolution is a production-grade distributed doctor appointment booking platform built with a microservices architecture.
It demonstrates real-world engineering patterns including the SAGA pattern for distributed transactions,
optimistic locking for concurrent slot booking, event-driven communication via Kafka, and strategy pattern
for multi-payment gateway support (Razorpay + Stripe).

**Three Roles:** Patient · Doctor · Admin

## Tech Stack

### Backend
- Java 21, Spring Boot 3.x
- Spring Cloud (Config Server, Eureka, OpenFeign, Gateway)
- Apache Kafka (async messaging)
- Keycloak (authentication + JWT)
- Resilience4j (circuit breaker, retry, rate limiting)
- WebSocket (real-time notifications)
- PostgreSQL (relational data)
- MongoDB (unstructured / document data)
- Redis (caching + idempotency keys)
- Cloudinary (image/media storage)
- Razorpay + Stripe (payment gateways — strategy pattern)
- Zipkin (distributed tracing)
- Prometheus + Grafana (monitoring)
- Docker + Kubernetes

### Frontend
- TypeScript, ReactJS, TailwindCSS, Zustand

### Infrastructure
- Docker Compose (local)
- Kubernetes (production)
- GitHub Actions (CI/CD)

## Microservices Architecture

| Service            | Port | Database        | Description                                                                 |
|--------------------|------|-----------------|-----------------------------------------------------------------------------|
| config-server      | 8888 | —               | Centralized configuration for all microservices                            |
| eureka-server      | 8761 | —               | Service registry and discovery                                              |
| api-gateway        | 8080 | —               | Single entry point, JWT validation, routing, rate limiting                 |
| keycloak           | 8081 | PostgreSQL      | Identity provider — handles login, logout, refresh token, JWT issuance     |
| user-service       | 8082 | PostgreSQL      | Patient registration, profile management, profile photo upload             |
| doctor-service     | 8083 | PostgreSQL      | Doctor profiles, clinic photos, onboarding documents, department/fees      |
| appointment-service| 8084 | PostgreSQL      | Slot management, booking with concurrency control, SAGA orchestration      |
| payment-service    | 8085 | PostgreSQL      | Razorpay + Stripe via strategy pattern, payment history, cash automation   |
| notification-service| 8086| MongoDB         | WebSocket real-time notifications, Kafka consumer, notification history    |
| feedback-service   | 8087 | MongoDB         | Patient and doctor feedback, ratings, category-based reviews               |
| admin-service      | 8088 | PostgreSQL      | Doctor verification, platform-wide search, analytics, fraud monitoring     |

**Total Services: 11** (3 infrastructure + 8 business)

---

## Database Decisions

### PostgreSQL (Relational — strong consistency required)
- user-service — user accounts, roles
- doctor-service — doctor profiles, documents, availability slots
- appointment-service — bookings, slot locks
- payment-service — transactions, payment records
- admin-service — admin actions, audit logs

### MongoDB (Document — flexible/unstructured data)
- notification-service — notification events, read/unread state, history
- feedback-service — feedback documents with nested categories, ratings, tags

### Redis
- Slot lock during booking (TTL-based, prevents double booking)
- Idempotency keys for payment API calls
- Doctor list caching for search/filter APIs
- OTP storage (if added later)

---

## Complete API List

### USER SERVICE (Port: 8082)
*Database: PostgreSQL | Entities: User, UserProfile, Address*

| Method | Endpoint                          | Description                          | Auth |
|--------|-----------------------------------|--------------------------------------|------|
| POST   | /api/v1/users/register            | Register new patient account         | No   |
| GET    | /api/v1/users/profile             | Get own profile                      | Yes  |
| PUT    | /api/v1/users/profile             | Update profile info                  | Yes  |
| POST   | /api/v1/users/profile/photo       | Upload profile photo (Cloudinary)    | Yes  |
| DELETE | /api/v1/users/profile/photo       | Remove profile photo                 | Yes  |
| GET    | /api/v1/users/{userId}            | Get patient by ID (internal use)     | Yes  |
| PUT    | /api/v1/users/password            | Change password                      | Yes  |
| DELETE | /api/v1/users/account             | Deactivate account                   | Yes  |

**Total: 8 APIs**

---

### DOCTOR SERVICE (Port: 8083)
*Database: PostgreSQL | Entities: Doctor, DoctorDocument, ClinicPhoto, Availability, Department*

| Method | Endpoint                                  | Description                                          | Auth        |
|--------|-------------------------------------------|------------------------------------------------------|-------------|
| POST   | /api/v1/doctors/register                  | Doctor self-registration                             | No          |
| POST   | /api/v1/doctors/onboarding/documents      | Upload verification documents (Cloudinary)           | Doctor      |
| GET    | /api/v1/doctors/profile                   | Get own profile                                      | Doctor      |
| PUT    | /api/v1/doctors/profile                   | Update fees, experience, bio, department             | Doctor      |
| POST   | /api/v1/doctors/profile/photo             | Upload profile photo                                 | Doctor      |
| POST   | /api/v1/doctors/clinic/photos             | Upload clinic photos (multiple)                      | Doctor      |
| DELETE | /api/v1/doctors/clinic/photos/{photoId}   | Remove a clinic photo                                | Doctor      |
| POST   | /api/v1/doctors/availability              | Set available slots for a date                       | Doctor      |
| GET    | /api/v1/doctors/availability              | View own availability slots                          | Doctor      |
| DELETE | /api/v1/doctors/availability/{slotId}     | Remove a slot                                        | Doctor      |
| GET    | /api/v1/doctors                           | List all approved doctors (with filters)             | Public      |
| GET    | /api/v1/doctors/{doctorId}                | Get doctor public profile                            | Public      |
| GET    | /api/v1/doctors/{doctorId}/slots          | Get available slots for a doctor on a date           | Public      |
| GET    | /api/v1/doctors/departments               | List all departments/specializations                 | Public      |

**Total: 14 APIs**

**Doctor Search Filters:** department, fees range (min/max), experience (min years), rating, gender, availability date, sort by (fees, rating, experience)

---

### APPOINTMENT SERVICE (Port: 8084)
*Database: PostgreSQL | Entities: Appointment, SlotLock, AppointmentSaga*
*Redis: Slot lock TTL keys*

| Method | Endpoint                                       | Description                                                   | Auth    |
|--------|------------------------------------------------|---------------------------------------------------------------|---------|
| POST   | /api/v1/appointments                           | Book appointment (acquires Redis lock, starts SAGA)           | Patient |
| GET    | /api/v1/appointments                           | Patient's booking history (filter: status, date range)        | Patient |
| GET    | /api/v1/appointments/{appointmentId}           | Get single appointment detail                                 | Yes     |
| PUT    | /api/v1/appointments/{appointmentId}/cancel    | Cancel appointment (triggers SAGA compensation)               | Patient |
| GET    | /api/v1/doctors/appointments                   | Doctor's bookings (default: today, filter: status/date)       | Doctor  |
| PUT    | /api/v1/doctors/appointments/{id}/complete     | Mark appointment as completed                                 | Doctor  |
| PUT    | /api/v1/doctors/appointments/{id}/no-show      | Mark patient as no-show                                       | Doctor  |
| GET    | /api/v1/appointments/{appointmentId}/status    | Poll appointment/SAGA status                                  | Yes     |
| POST   | /api/v1/appointments/internal/confirm          | Internal: SAGA step — confirm slot after payment success      | Internal|
| POST   | /api/v1/appointments/internal/compensate       | Internal: SAGA step — release slot on payment failure         | Internal|

**Total: 10 APIs**

**Concurrency Handling:** Redis distributed lock acquired before booking. If lock exists → 409 Conflict. Lock TTL = 10 minutes (payment window). On payment success → slot permanently marked booked. On payment failure → lock released, slot available again.

---

### PAYMENT SERVICE (Port: 8085)
*Database: PostgreSQL | Entities: Payment, PaymentTransaction, RefundRecord*
*Redis: Idempotency keys*

| Method | Endpoint                                        | Description                                               | Auth    |
|--------|-------------------------------------------------|-----------------------------------------------------------|---------|
| POST   | /api/v1/payments/initiate                       | Initiate payment (Razorpay or Stripe, strategy pattern)   | Patient |
| POST   | /api/v1/payments/razorpay/webhook               | Razorpay webhook — payment success/failure callback       | No      |
| POST   | /api/v1/payments/stripe/webhook                 | Stripe webhook — payment success/failure callback         | No      |
| GET    | /api/v1/payments                                | Patient's payment history                                 | Patient |
| GET    | /api/v1/payments/{paymentId}                    | Get payment detail                                        | Yes     |
| POST   | /api/v1/payments/{paymentId}/refund             | Initiate refund on cancellation                           | Patient |
| GET    | /api/v1/doctors/payments                        | Doctor's earnings history                                 | Doctor  |
| PUT    | /api/v1/payments/internal/cash/mark-complete    | Internal: scheduled job marks cash payment complete       | Internal|

**Total: 8 APIs**

**Cash Payment Automation:** Spring `@Scheduled` job runs every hour. Finds appointments where payment_mode=CASH and appointment end time has passed and status=COMPLETED. Auto-marks those payments as COMPLETED_CASH. Doctor can also manually mark via their dashboard.

**Strategy Pattern:** `PaymentGateway` interface → `RazorpayGatewayImpl` and `StripeGatewayImpl`. Patient selects gateway at initiation. Factory resolves correct implementation.

---

### NOTIFICATION SERVICE (Port: 8086)
*Database: MongoDB | Collections: Notification, NotificationPreference*

| Method | Endpoint                                         | Description                                       | Auth |
|--------|--------------------------------------------------|---------------------------------------------------|------|
| GET    | /api/v1/notifications                            | Get all notifications for user (paginated)        | Yes  |
| PUT    | /api/v1/notifications/{id}/read                  | Mark single notification as read                  | Yes  |
| PUT    | /api/v1/notifications/read-all                   | Mark all notifications as read                    | Yes  |
| GET    | /api/v1/notifications/unread-count               | Get unread notification count                     | Yes  |
| DELETE | /api/v1/notifications/{id}                       | Delete a notification                             | Yes  |
| WS     | /ws/notifications                                | WebSocket endpoint for real-time notifications    | Yes  |

**Total: 5 REST + 1 WebSocket**

**Kafka Topics Consumed:**
- `appointment.booked` → notify patient + doctor
- `appointment.cancelled` → notify patient + doctor
- `payment.completed` → notify patient
- `payment.failed` → notify patient
- `doctor.approved` / `doctor.rejected` → notify doctor

---

### FEEDBACK SERVICE (Port: 8087)
*Database: MongoDB | Collections: Feedback, FeedbackCategory*

| Method | Endpoint                                      | Description                                          | Auth    |
|--------|-----------------------------------------------|------------------------------------------------------|---------|
| POST   | /api/v1/feedback                              | Submit feedback (doctor or platform category)        | Patient |
| GET    | /api/v1/feedback/doctor/{doctorId}            | Get all feedback for a doctor (public)               | Public  |
| GET    | /api/v1/feedback/platform                     | Get platform-level feedback                          | Public  |
| GET    | /api/v1/feedback/my                           | Get patient's own submitted feedback                 | Patient |
| PUT    | /api/v1/feedback/{feedbackId}                 | Edit own feedback (within 24hrs)                     | Patient |
| DELETE | /api/v1/feedback/{feedbackId}                 | Delete own feedback                                  | Patient |
| GET    | /api/v1/feedback/doctor/{doctorId}/summary    | Avg rating, total reviews, breakdown by category     | Public  |
| POST   | /api/v1/feedback/doctor                       | Doctor submits feedback about platform               | Doctor  |

**Total: 8 APIs**

**Feedback Categories:** Doctor Behavior, Wait Time, Clinic Cleanliness, Treatment Quality, Platform Experience, Appointment Process

---

### ADMIN SERVICE (Port: 8088)
*Database: PostgreSQL | Entities: AdminAction, DoctorVerification, AuditLog*

| Method | Endpoint                                           | Description                                              | Auth  |
|--------|----------------------------------------------------|----------------------------------------------------------|-------|
| GET    | /api/v1/admin/doctors/pending                      | List doctors pending verification                        | Admin |
| GET    | /api/v1/admin/doctors/pending/{doctorId}/documents | View submitted documents                                 | Admin |
| PUT    | /api/v1/admin/doctors/{doctorId}/approve           | Approve doctor registration                              | Admin |
| PUT    | /api/v1/admin/doctors/{doctorId}/reject            | Reject with reason                                       | Admin |
| GET    | /api/v1/admin/doctors                              | List all doctors (search, filter by status/department)   | Admin |
| GET    | /api/v1/admin/patients                             | List all patients (search by name/email)                 | Admin |
| GET    | /api/v1/admin/appointments                         | All appointments (filter: status, date, doctor, patient) | Admin |
| GET    | /api/v1/admin/payments                             | All payment records (filter: gateway, status, date)      | Admin |
| GET    | /api/v1/admin/feedback                             | All feedback (filter: category, rating)                  | Admin |
| DELETE | /api/v1/admin/feedback/{feedbackId}                | Remove inappropriate feedback                            | Admin |
| GET    | /api/v1/admin/dashboard                            | Platform stats — total users, bookings, revenue today    | Admin |

**Total: 11 APIs**

---

## API Count Summary

| Service              | API Count |
|----------------------|-----------|
| user-service         | 8         |
| doctor-service       | 14        |
| appointment-service  | 10        |
| payment-service      | 8         |
| notification-service | 6 (5+WS)  |
| feedback-service     | 8         |
| admin-service        | 11        |
| **TOTAL**            | **~65 APIs** |

*Note: Keycloak handles login, logout, refresh token — no custom auth APIs needed.*

---

## SAGA Pattern — Booking + Payment Flow

### Happy Path
```
Patient clicks Book
    → appointment-service acquires Redis slot lock (TTL: 10 min)
    → appointment created with status: PENDING
    → Kafka event: appointment.payment.pending
        → payment-service picks up event
        → payment initiated with Razorpay/Stripe
        → patient completes payment
        → webhook received by payment-service
        → Kafka event: payment.completed
            → appointment-service confirms slot (status: CONFIRMED)
            → notification-service notifies patient + doctor
```

### Compensation (Failure Path)
```
Payment fails or times out
    → payment-service publishes: payment.failed
        → appointment-service receives event
        → appointment status: CANCELLED
        → Redis slot lock released
        → slot available for next patient
        → notification-service notifies patient: "Payment failed, slot released"
```

---

## Docker Compose Services

```yaml
services:

  config-server:
    port: 8888
    description: Centralized config server — all services fetch config from here on startup
    depends_on: []

  eureka-server:
    port: 8761
    description: Service registry — all microservices register and discover each other here
    depends_on: [config-server]

  api-gateway:
    port: 8080
    description: Single entry point — JWT validation, routing, rate limiting via Resilience4j
    depends_on: [eureka-server, keycloak]

  keycloak:
    port: 8081
    description: Identity provider — login, logout, JWT, refresh token, role management
    depends_on: [postgres-keycloak]

  user-service:
    port: 8082
    description: Patient accounts, profile management, photo upload via Cloudinary
    depends_on: [config-server, eureka-server, postgres-user, keycloak]

  doctor-service:
    port: 8083
    description: Doctor profiles, onboarding docs, availability slots, clinic photos
    depends_on: [config-server, eureka-server, postgres-doctor, cloudinary]

  appointment-service:
    port: 8084
    description: Slot booking, concurrency via Redis lock, SAGA orchestration via Kafka
    depends_on: [config-server, eureka-server, postgres-appointment, redis, kafka]

  payment-service:
    port: 8085
    description: Razorpay + Stripe strategy pattern, webhooks, refunds, cash automation
    depends_on: [config-server, eureka-server, postgres-payment, redis, kafka]

  notification-service:
    port: 8086
    description: WebSocket real-time notifications, Kafka consumer, MongoDB history
    depends_on: [config-server, eureka-server, mongodb, kafka]

  feedback-service:
    port: 8087
    description: Patient and doctor feedback, ratings, category-based reviews in MongoDB
    depends_on: [config-server, eureka-server, mongodb]

  admin-service:
    port: 8088
    description: Doctor verification, platform analytics, audit logs
    depends_on: [config-server, eureka-server, postgres-admin]

  postgres:
    port: 5432
    description: Primary PostgreSQL instance (separate databases per service)

  mongodb:
    port: 27017
    description: MongoDB for notification and feedback services

  redis:
    port: 6379
    description: Slot locking, idempotency keys, doctor list caching

  kafka:
    port: 9092
    description: Async event broker between appointment, payment, and notification services

  zookeeper:
    port: 2181
    description: Kafka coordination

  zipkin:
    port: 9411
    description: Distributed tracing across all microservices

  prometheus:
    port: 9090
    description: Metrics scraping from all Spring Boot actuator endpoints

  grafana:
    port: 3001
    description: Metrics dashboards — request rates, error rates, booking throughput

  frontend:
    port: 3000
    description: React + TypeScript + TailwindCSS — patient, doctor, and admin UIs
```

---

## Key Engineering Problems Solved

### 1. Concurrent Slot Booking (Double Booking Prevention)
Redis distributed lock with TTL. When patient A and patient B try to book the same slot simultaneously, only one acquires the lock. The other gets 409 Conflict immediately. Lock is released on payment failure or expiry.

### 2. SAGA Pattern — Distributed Transaction
Booking spans appointment-service and payment-service across two databases. SAGA ensures if payment fails, the slot is compensated (released). No two-phase commit. No data inconsistency.

### 3. Idempotency in Payments
Every payment initiation call includes an idempotency key stored in Redis. If the same request is retried (network failure), the same response is returned without double-charging the patient.

### 4. Strategy Pattern — Payment Gateways
`PaymentGateway` interface with `RazorpayGatewayImpl` and `StripeGatewayImpl`. New gateways can be added without touching existing code. Open/Closed Principle.

### 5. Cash Payment Automation
Spring `@Scheduled` cron job. Every hour, finds appointments where mode=CASH, status=COMPLETED, and appointment time has passed. Auto-transitions payment to COMPLETED_CASH. No manual intervention needed.

### 6. Circuit Breaker
Resilience4j circuit breakers on all OpenFeign calls between services. Prevents cascade failures. Fallback responses defined for each critical path.

---

## Project Structure (Monorepo)

```
medislot/
├── infrastructure/
│   ├── config-server/
│   ├── eureka-server/
│   └── api-gateway/
├── services/
│   ├── user-service/
│   ├── doctor-service/
│   ├── appointment-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── feedback-service/
│   └── admin-service/
├── frontend/
│   └── medislot-ui/
├── docker-compose.yml
├── docker-compose.infra.yml
└── README.md
```

---

## Interview Talking Points

**Q: How did you handle two patients booking the same slot simultaneously?**
Redis distributed lock. One acquires it, the other gets 409. Lock has TTL — auto-releases if payment doesn't complete within 10 minutes.

**Q: What happens if payment fails after the slot was locked?**
SAGA compensation. Payment service publishes `payment.failed` to Kafka. Appointment service consumes it, cancels the appointment, releases the Redis lock, slot becomes available again.

**Q: How did you prevent double-charging on retries?**
Idempotency key per payment request, stored in Redis with TTL. Same key returns cached response. Never hits payment gateway twice.

**Q: How did you support multiple payment gateways?**
Strategy pattern. `PaymentGateway` interface. Two implementations: `RazorpayGatewayImpl`, `StripeGatewayImpl`. Factory resolves by gateway type string. Adding a third gateway requires zero changes to existing code.

**Q: Why MongoDB for notifications and feedback?**
Notifications are event documents — schema varies by type (booking notification vs payment notification). MongoDB's flexible schema fits better than forcing a rigid table. Same for feedback — categories, nested ratings, and tags are better as documents.

**Q: How does your API Gateway validate tokens?**
Keycloak issues JWT. Gateway validates signature and expiry on every request using Keycloak's public key. Role claims extracted from JWT and passed downstream as headers. Services trust gateway-validated headers.

---

*This document serves as the complete project specification and context for MedXSolution.*
*Last updated: 31st March 2026*