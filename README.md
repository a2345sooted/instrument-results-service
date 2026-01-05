# Instrument Results Service

Spring Boot service that manages instrument runs and measurement submissions, with asynchronous processing and a REST API.

### Tech Stack
- Java 17
- Spring Boot 3.5.9
- Gradle
- PostgreSQL (Docker Compose)
- Flyway

### Prerequisites
- Java 17
- Docker

### Start PostgreSQL database before running app
```bash
docker compose up -d
