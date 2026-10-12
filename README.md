# LMS - Spring Boot project series

A Learning Management System built as a project-based Spring Boot series. Each episode's code is tagged
in this repo so you can check out the exact state of the project at any
point in the series.

## Docs

Requirements, design, and decision records live in [`docs/`](./docs) —
see [`docs/README.md`](./docs/README.md) for how it's organized. These
aren't written for their own sake: each module's implementation chapters
are built directly from its PRD and design doc.

## Architecture

Modular monolith: one deployable Spring Boot application, split into
feature packages that only talk to each other through public service
interfaces (never by reaching into another module's repository directly).

```
com.hosiyar.lms
├── common          shared kernel: base entity, exception handling, API response wrapper
├── user            users & auth (roles, JWT)
├── course          course catalog, lessons, content
├── enrollment      student enrollment, progress tracking
├── assessment      quizzes, grading, certificates
├── payment         orders, invoices, Stripe integration
├── live            live session scheduling & streaming
└── notification    async email/SMS/push, driven by Kafka events
```

## Tech stack

- Java 21, Spring Boot 4.1 (Spring Framework 7)
- MySQL, Flyway migrations
- Spring Security 7 + JWT (added in the Users & auth episode)
- Redis, Kafka (added when the Notifications episode needs them)
- Stripe (added in the Payments episode)
- Testcontainers for integration tests (added in the Testing module)
- Docker + GitHub Actions (added in the Containerization & CI/CD module —
  see [ADR-005](./docs/adr/adr-005-defer-docker.md) for why it's not
  needed yet)

## Requirements

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8.x, installed and running locally

## Getting started

```sql
-- run once, in your local MySQL client
CREATE DATABASE lms CHARACTER SET utf8mb4;
CREATE USER 'lms'@'localhost' IDENTIFIED BY 'lms';
GRANT ALL PRIVILEGES ON lms.* TO 'lms'@'localhost';
FLUSH PRIVILEGES;
```

```bash
# run the app (dev profile is active by default, connects to
# localhost:3306 - see src/main/resources/application-dev.yml)
mvn spring-boot:run

# smoke test
curl http://localhost:8080/api/v1/ping
curl http://localhost:8080/actuator/health
```

## Following along

Each video's code is tagged when the episode is published. Clone the repo
and `git checkout <tag>` to see the project exactly as it stood at that
video.

| Episode | Topic | Tag | Video |
|---|---|---|---|
| 1 | Requirements & design — Users & Auth | `video1` | _link once published_ |
| 2 | Project bootstrap, shared kernel, local MySQL | `video2` | _link once published_ |
| 3 | Users module — data layer, registration | `video3` | _link once published_ |
| 4 | Spring Security 7 + JWT authentication | `video4` | _link once published_ |
| 5 | Role-based access, token refresh, first tests | `video5` | _link once published_ |


