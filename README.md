# 🚗 Car Pooling Service

Servicio de asignación de coches compartidos. Gestiona una flota de vehículos y asigna grupos de personas según disponibilidad de asientos, respetando el orden de llegada.

## ⚡ Quick Start

```bash
# Con Docker (recomendado)
docker compose up --build

# Sin Docker (requiere PostgreSQL + Java 21 + Maven)
mvn spring-boot:run
```

Abre `http://localhost:8080` en el navegador.

## 🛠️ Tech Stack

| Componente | Tecnología |
|---|---|
| Backend | Spring Boot 3.2.5, Java 21 |
| Base de datos | PostgreSQL 16 (prod), H2 (test) |
| Frontend | HTML/CSS/JS SPA |
| Contenedor | Docker multi-stage |
| Cloud | Render (free tier) |

## 📡 API REST

Base: `/api/v1`

| Método | Endpoint | Descripción | Status |
|---|---|---|---|
| `GET` | `/status` | Health check | 200 |
| `PUT` | `/cars` | Cargar flota | 200 / 400 |
| `POST` | `/journeys` | Registrar grupo | 201 / 400 / 409 |
| `DELETE` | `/journeys/{id}` | Dropoff | 204 / 404 |
| `GET` | `/journeys/{id}/car` | Localizar coche | 200 / 204 / 404 |

## 📚 Documentación

| Documento | Descripción |
|---|---|
| [📋 Requisitos](docs/01-requirements.md) | Requisitos funcionales y no funcionales |
| [🏗️ Diseño](docs/02-design.md) | Arquitectura, modelo de datos, algoritmos |
| [👤 Manual de usuario](docs/03-user-manual.md) | Guía de uso de la interfaz web |
| [⚙️ Configuración](docs/04-configuration.md) | Variables de entorno, Docker, Render |
| [🧪 Pruebas](docs/05-testing.md) | 40 tests: cobertura, ejecución, cómo añadir |

## 🧪 Tests

```bash
mvn test     # 40 tests (23 servicio + 16 controlador + 1 contexto)
```

## 🌐 Idiomas

Español 🇪🇸 | English 🇬🇧 | Français 🇫🇷 | Deutsch 🇩🇪
