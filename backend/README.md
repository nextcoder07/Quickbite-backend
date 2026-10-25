# QuickBite Backend — Spring Boot Modular Monolith

## 1. Overview
QuickBite is an enterprise-grade desktop campus food marketplace backend built with **Java 17/21** and **Spring Boot 3.3.5**. It coordinates campus food shops, student buyers, delivery partners, and university administrators through a unified modular monolithic architecture.

## 2. Technology Stack & Integrations
- **Framework**: Spring Boot 3.3.5 (Spring Web, Spring Security, Spring Data JPA, Bean Validation)
- **Database**: Supabase PostgreSQL (Flyway versioned schema migrations, H2 compatibility mode for offline dev/tests)
- **Authentication**: Firebase Authentication ID Token verification via Firebase Admin SDK
- **Payments**: Razorpay in test mode with server-side HMAC-SHA256 signature verification & webhooks
- **Media Storage**: Supabase Storage adapter with dev fallback
- **Documentation**: Springdoc OpenAPI / Swagger UI (`/swagger-ui.html`)
- **Testing**: JUnit 5 & Spring Boot Test

## 3. Package & Feature Architecture
Located under `com.quickbite`:
- `identity`: Shared authentication identity (`AccountIdentity`), registration, profile exclusivity validation.
- `buyer`: Buyer profile (`User`), profile updates, buyer self-deactivation.
- `delivery`: Delivery partner profile (`DeliveryPartner`), assignments (`DeliveryAssignment`), pickup/drop status tracking.
- `shop`: Shop Owner profile (`ShopOwner`), campus shop entity (`Shop`), product catalog (`Product`), inventory management.
- `ordering`: Orders (`CustomerOrder`), snapshot line items (`OrderItem`), order lifecycle state machine, atomic stock decrement.
- `payment`: Razorpay test gateway (`RazorpayGateway`), payments (`Payment`), capped refunds (`Refund`), webhooks.
- `accountdeletion`: Deletion requests for shop owners & delivery partners with self-review prevention.
- `feedback`: Student order reviews (1 to 5 stars) strictly for delivered orders.
- `dispute`: Order dispute lodging and administrative resolution workflow.
- `administration`: Operational metrics, shop approvals/suspensions, audit log viewer.
- `audit`: Structured, append-only audit trail (`AuditLog`) omitting sensitive tokens and credentials.
- `storage`: `StorageService` interface and `SupabaseStorageAdapter`.
- `security`: `FirebaseTokenFilter`, `CurrentAccountPrincipal`, stateless `SecurityFilterChain`.
- `config`: Security, CORS, OpenAPI, Firebase, and development data seeding.

## 4. Database Schema (16 PostgreSQL Tables)
Migrated via `db/migration/V1__initial_schema.sql`:
1. `account_identities`: Shared auth mapping with lowercase unique email index.
2. `users`: Buyer-only profile records.
3. `delivery_partners`: Delivery fulfillment personnel profiles.
4. `shop_owners`: Food shop owner profiles.
5. `admins`: Provisioned administrator profiles.
6. `shops`: Campus shops with separate approval and open/closed operational status.
7. `products`: Menu catalog with nonnegative price and stock constraints.
8. `orders`: Authoritative backend pricing with `total = subtotal + fee - discount`.
9. `order_items`: Immutable snapshots of product name and unit price.
10. `payments`: Razorpay transaction attempts, idempotency keys, and verified timestamps.
11. `refunds`: Reversals capped against captured balances.
12. `delivery_assignments`: One assignment per order with partner authorization checks.
13. `account_deletion_requests`: Closure requests reviewed by admins.
14. `feedback`: Single review per delivered order.
15. `disputes`: Student issue tickets reviewed by administrators.
16. `audit_logs`: Append-only security and operational audit trail with JSON metadata.

## 5. Running the Backend
### Prerequisites
- JDK 17 or higher
- Apache Maven 3.9+

### Commands
From the `backend` folder:
```powershell
# Option 1: Using the provided wrapper (recommended)
.\mvnw spring-boot:run

# Option 2: Running the pre-built production fat JAR directly
java -jar target\quickbite-backend-1.0.0.jar

# Option 3: If you refresh your PowerShell session (Maven is installed at C:\Users\creat\tools\apache-maven-3.9.9\bin)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
mvn spring-boot:run

# Run automated tests
.\mvnw test
```

API Documentation will be accessible at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
