# tracker-backend (Spring Boot + Postgres/PostGIS)

This is an MVP backend scaffold for a **mobile user tracking** system:

- Users start/stop tracking sessions
- Mobile sends location points (batch-friendly)
- Backend stores **sessions + points + summaries**
- Admin dashboard can read:
  - all users last known location
  - session history and draw polylines on map

## Requirements
- Java 21
- Docker (for Postgres/PostGIS + Redis)
- Gradle (or generate wrapper once)

## Run Postgres + Redis
```bash
docker compose up -d
```

## Run the app
```bash
./gradlew bootRun
# if you don't have wrapper yet:
gradle wrapper
./gradlew bootRun
```

## DB Migration
Flyway runs automatically on startup.

## Notes
- Security/JWT is intentionally left as a **thin placeholder** for you to implement step-by-step.
- PostGIS is enabled and geography columns are used for location points.
