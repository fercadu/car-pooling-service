# ⚙️ Guía de Configuración

## 1. Requisitos previos

| Componente | Versión | Notas |
|---|---|---|
| **Java JDK** | 21+ | OpenJDK o Eclipse Temurin |
| **Maven** | 3.9+ | Para compilar el proyecto |
| **PostgreSQL** | 14+ | Solo en producción (los tests usan H2) |
| **Docker** | 24+ | Opcional, para despliegue contenerizado |

## 2. Ejecución local (desarrollo)

### 2.1 Con PostgreSQL local

```bash
# 1. Crear base de datos
createdb carpooling

# 2. Compilar y ejecutar
mvn spring-boot:run
```

La app arranca en `http://localhost:8080` con la configuración por defecto:
- Host: `localhost:5432`
- Base de datos: `carpooling`
- Usuario/contraseña: `carpooling/carpooling`

### 2.2 Solo para desarrollo rápido (sin PostgreSQL)

Si quieres arrancar sin PostgreSQL, puedes sobreescribir propiedades para usar H2 en tiempo de ejecución:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --spring.datasource.url=jdbc:h2:mem:carpooling \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.jpa.hibernate.ddl-auto=create-drop"
```

> Necesitarás mover H2 de `<scope>test</scope>` a `<scope>runtime</scope>` en `pom.xml`.

## 3. Ejecución con Docker

### 3.1 Docker Compose (recomendado)

```bash
docker compose up --build
```

Esto levanta:
- **carpooling-db**: PostgreSQL 16 en puerto 5432
- **carpooling-app**: Spring Boot en puerto 8080

Los datos se persisten en el volumen `pgdata`.

### 3.2 Parar y limpiar

```bash
docker compose down          # Para los servicios
docker compose down -v       # Para + elimina datos de PostgreSQL
```

## 4. Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `carpooling` |
| `DB_USER` | Usuario de base de datos | `carpooling` |
| `DB_PASSWORD` | Contraseña de base de datos | `carpooling` |
| `DB_URL` | URL JDBC completa (sobreescribe HOST/PORT/NAME) | — |
| `JAVA_OPTS` | Opciones JVM | — |

### Prioridad

Si se define `DB_URL`, tiene prioridad sobre `DB_HOST` + `DB_PORT` + `DB_NAME`:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:carpooling}}
```

## 5. Configuración de application.properties

```properties
# Puerto del servidor
server.port=8080

# Base de datos
spring.datasource.url=...          # Ver sección 4
spring.datasource.username=...
spring.datasource.password=...
spring.datasource.driver-class-name=org.postgresql.Driver

# Pool de conexiones (optimizado para free tier)
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1

# JPA
spring.jpa.hibernate.ddl-auto=update    # Crea/actualiza tablas automáticamente
spring.jpa.open-in-view=false           # Evita lazy loading en la vista

# Logging
logging.level.com.carpooling=INFO       # Cambiar a DEBUG para más detalle
```

## 6. Despliegue en Render (cloud gratuito)

El proyecto incluye `render.yaml` (Blueprint) que configura todo automáticamente.

### Pasos:

1. **Sube el código a GitHub** (ya hecho: `fercadu/car-pooling-service`)
2. Ve a [dashboard.render.com](https://dashboard.render.com)
3. **New → Blueprint** → selecciona el repositorio
4. Render lee `render.yaml` y crea:
   - 🐘 PostgreSQL `carpooling-db` (free tier)
   - 🚀 Web Service `car-pooling-service` (free tier, Docker)
5. Las variables de entorno se conectan automáticamente
6. Click **Apply** → despliega en ~3-4 minutos

### Limitaciones del free tier:
- La app se **duerme tras 15 minutos** sin tráfico
- La primera petición tras dormir tarda ~30 segundos
- PostgreSQL gratuito durante **90 días**

### Despliegues automáticos:
Cada `git push` a `main` lanza un redespliegue automático.

## 7. Configuración de tests

Los tests usan H2 en memoria (`src/test/resources/application.properties`):

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
```

Ejecutar tests:
```bash
mvn test
```

## 8. Logging

### Niveles configurables

| Logger | Nivel | Qué muestra |
|---|---|---|
| `com.carpooling` | `INFO` | Operaciones principales (load, journey, dropoff, reassign) |
| `com.carpooling` | `DEBUG` | + Detalle de flota, asientos, localizaciones |
| `DispatcherServlet` | `DEBUG` | Método y URI de cada petición HTTP |

Para cambiar en producción sin recompilar:
```bash
# Via variable de entorno
LOGGING_LEVEL_COM_CARPOOLING=DEBUG

# O en docker-compose.yml
environment:
  LOGGING_LEVEL_COM_CARPOOLING: DEBUG
```
