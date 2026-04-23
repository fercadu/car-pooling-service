# ⚙️ Configuration Guide

## 1. Prerequisites

| Component | Version | Notes |
|---|---|---|
| **Java JDK** | 21+ | OpenJDK or Eclipse Temurin |
| **Maven** | 3.9+ | To build the project |
| **PostgreSQL** | 14+ | Production only (tests use H2) |
| **Docker** | 24+ | Optional, for containerized deployment |

## 2. Local Execution (Development)

### 2.1 With Local PostgreSQL

```bash
# 1. Create database
createdb carpooling

# 2. Build and run
mvn spring-boot:run
```

The app starts at `http://localhost:8080` with default configuration:
- Host: `localhost:5432`
- Database: `carpooling`
- User/password: `carpooling/carpooling`

### 2.2 Quick Development (without PostgreSQL)

To start without PostgreSQL, you can override properties at runtime to use H2:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --spring.datasource.url=jdbc:h2:mem:carpooling \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.jpa.hibernate.ddl-auto=create-drop"
```

> You will need to move H2 from `<scope>test</scope>` to `<scope>runtime</scope>` in `pom.xml`.

## 3. Docker Execution

### 3.1 Docker Compose (recommended)

```bash
docker compose up --build
```

This starts:
- **carpooling-db**: PostgreSQL 16 on port 5432
- **carpooling-app**: Spring Boot on port 8080

Data is persisted in the `pgdata` volume.

### 3.2 Stop and Clean Up

```bash
docker compose down          # Stop services
docker compose down -v       # Stop + delete PostgreSQL data
```

## 4. Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `carpooling` |
| `DB_USER` | Database user | `carpooling` |
| `DB_PASSWORD` | Database password | `carpooling` |
| `DB_URL` | Full JDBC URL (overrides HOST/PORT/NAME) | — |
| `JAVA_OPTS` | JVM options | — |

### Priority

If `DB_URL` is defined, it takes priority over `DB_HOST` + `DB_PORT` + `DB_NAME`:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:carpooling}}
```

## 5. application.properties Reference

```properties
# Server port
server.port=8080

# Database
spring.datasource.url=...          # See section 4
spring.datasource.username=...
spring.datasource.password=...
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection pool (optimized for free tier)
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1

# JPA
spring.jpa.hibernate.ddl-auto=update    # Auto-create/update tables
spring.jpa.open-in-view=false           # Prevent lazy loading in views

# Logging
logging.level.com.carpooling=INFO       # Change to DEBUG for more detail
```

## 6. Render Deployment (Free Cloud)

The project includes `render.yaml` (Blueprint) that configures everything automatically.

### Steps:

1. **Push code to GitHub** (already done: `fercadu/car-pooling-service`)
2. Go to [dashboard.render.com](https://dashboard.render.com)
3. **New → Blueprint** → select the repository
4. Render reads `render.yaml` and creates:
   - 🐘 PostgreSQL `carpooling-db` (free tier)
   - 🚀 Web Service `car-pooling-service` (free tier, Docker)
5. Environment variables are connected automatically
6. Click **Apply** → deploys in ~3-4 minutes

### Free Tier Limitations:
- The app **sleeps after 15 minutes** without traffic
- First request after sleeping takes ~30 seconds
- Free PostgreSQL for **90 days**

### Automatic Deployments:
Every `git push` to `main` triggers an automatic redeployment.

## 7. Test Configuration

Tests use H2 in-memory (`src/test/resources/application.properties`):

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
```

Run tests:
```bash
mvn test
```

## 8. Logging

### Configurable Levels

| Logger | Level | What it shows |
|---|---|---|
| `com.carpooling` | `INFO` | Main operations (load, journey, dropoff, reassign) |
| `com.carpooling` | `DEBUG` | + Fleet detail, seats, locate calls |
| `DispatcherServlet` | `DEBUG` | Method and URI of every HTTP request |

To change in production without recompiling:
```bash
# Via environment variable
LOGGING_LEVEL_COM_CARPOOLING=DEBUG

# Or in docker-compose.yml
environment:
  LOGGING_LEVEL_COM_CARPOOLING: DEBUG
```
