# DAK Backend (Spring Boot)

DAK (Discover Adelaide Korea) MVP backend. Implements the API defined in
`05_API_Specification_DAK.docx` against the schema defined in `04_Database_Design_DAK.docx`.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL + Flyway (schema is migration-owned, not Hibernate-generated)
- JWT (jjwt) for access/refresh tokens
- Lombok

## Package structure

```
com.dak.backend
├── config       # security, CORS, JWT filter, bean config
├── controller   # REST endpoints — one per 05 API Spec resource group
├── service      # business logic
├── repository   # Spring Data JPA repositories
├── domain       # JPA entities — mirrors 04 Database Design tables
├── dto          # request/response models (never expose entities directly)
├── exception    # ApiException + GlobalExceptionHandler
└── common       # ApiResponse / ApiError — the standard envelope from 05 API Spec §11.2
```

## Response format

Every endpoint returns the standard envelope from `05_API_Specification_DAK.docx` §2.4/§11.2:

```json
{ "success": true, "data": { } }
{ "success": false, "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [] } }
```

`GlobalExceptionHandler` enforces this for all thrown exceptions, and strips stack
traces / SQL / internal paths from responses per §2.6.

## Running locally

Requires a local PostgreSQL instance and Maven with normal internet access
(this project could **not** be built or dependency-resolved inside the current
sandboxed environment — outbound access to Maven Central isn't on the allowed
domain list here, so `mvn` was not run against this code; it hasn't been
compiled or tested yet).

```bash
createdb dak_dev
export DAK_DB_USERNAME=dak DAK_DB_PASSWORD=dak
./mvnw spring-boot:run
```

Then:

```bash
curl http://localhost:8080/api/v1/health
# { "success": true, "data": { "status": "UP", "service": "dak-backend" } }
```

## Status

See `07_Backend_Development_Log.docx` in the project files for step-by-step progress.

**Current step:** 1. Initial project setup (package structure, config, standard
response/error envelope, health check). Not yet compiled/verified locally — see note above.

**Next step:** Flyway migration scripts from `04_Database_Design_DAK.docx`, then JPA entities.
