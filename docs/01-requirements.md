# 📋 Requisitos del Sistema

## 1. Descripción general

Car Pooling Service es un servicio de asignación de coches compartidos. Gestiona una flota de vehículos y asigna grupos de personas a coches según la disponibilidad de asientos, respetando el orden de llegada.

## 2. Requisitos funcionales

### RF-01 — Gestión de flota
- El operador puede cargar una lista de coches con su ID y número de asientos (4, 5 o 6).
- Cargar una nueva flota **reemplaza** todos los datos previos (coches, viajes y cola de espera).
- Se puede recargar la flota en cualquier momento.

### RF-02 — Registro de viajes
- Un grupo de 1 a 6 personas solicita un viaje indicando un ID de grupo y el número de personas.
- Si hay un coche con suficientes asientos libres, se asigna automáticamente.
- Si no hay coche disponible, el grupo entra en **cola de espera**.
- No se permite registrar dos grupos con el mismo ID.

### RF-03 — Asignación de coches
- Se asigna el primer coche disponible con suficientes asientos (orden por ID ascendente).
- Un grupo viaja siempre junto en el mismo coche.
- No se puede reasignar un grupo a otro coche una vez asignado.

### RF-04 — Orden de servicio (fairness)
- Los grupos se sirven lo más rápido posible manteniendo el orden de llegada.
- Un grupo posterior solo puede servirse antes que uno anterior si **ningún coche puede servir** al grupo anterior.
- Ejemplo: un grupo de 6 espera; llega un grupo de 2 y hay 4 asientos libres → el grupo de 2 se asigna.

### RF-05 — Abandono (dropoff)
- Un grupo puede abandonar en cualquier momento, haya viajado o no.
- Si el grupo tenía coche asignado, se liberan los asientos.
- Tras liberar asientos, se intenta reasignar grupos de la cola de espera.

### RF-06 — Localización
- Dado un ID de grupo, se puede consultar en qué coche viaja.
- Si el grupo está esperando, se indica que no tiene coche asignado.
- Si el grupo no existe, se devuelve error.

### RF-07 — Interfaz web
- Panel de operador con visualización en tiempo real de la flota, viajes y cola de espera.
- Consola de trazas con filtrado por nivel (ALL, INFO, WARN, ERROR, DEBUG).
- Estadísticas: coches totales, asientos libres, viajes activos, grupos en espera.
- Multiidioma: español, inglés, francés, alemán.

### RF-08 — Persistencia
- Los datos se almacenan en PostgreSQL.
- El estado sobrevive a reinicios del servicio.

## 3. Requisitos no funcionales

### RNF-01 — Rendimiento
- La asignación de coches opera en tiempo lineal sobre la flota.
- Máximo 500 líneas de log en la interfaz para evitar consumo de memoria del navegador.

### RNF-02 — Escalabilidad
- La aplicación está contenerizada con Docker.
- Configuración mediante variables de entorno para despliegue en cualquier cloud.

### RNF-03 — Usabilidad
- Mensajes de error amigables en popups (sin rutas técnicas de validación).
- Interfaz responsive con tema oscuro.

### RNF-04 — Mantenibilidad
- Arquitectura en capas: Controller → Service → Repository.
- DTOs separados de las entidades de dominio.
- Traducciones externalizadas a ficheros JSON.

### RNF-05 — Seguridad
- El contenedor Docker ejecuta con usuario no-root.
- No se almacenan secretos en código fuente (variables de entorno).

## 4. Restricciones

| Restricción | Valor |
|---|---|
| Asientos por coche | 4, 5 o 6 |
| Personas por grupo | 1 a 6 |
| ID de grupo | Entero positivo, único |
| ID de coche | Entero |
| Idiomas soportados | es, en, fr, de |

## 5. Casos de uso principales

```
┌──────────┐       ┌──────────────────────────┐
│ Operador │──────▶│ Cargar flota de coches    │  PUT /api/v1/cars
│          │──────▶│ Registrar grupo de viaje  │  POST /api/v1/journeys
│          │──────▶│ Localizar grupo           │  GET /api/v1/journeys/{id}/car
│          │──────▶│ Dar de baja grupo         │  DELETE /api/v1/journeys/{id}
│          │──────▶│ Ver estado del servicio   │  GET /api/v1/status
└──────────┘       └──────────────────────────┘
```
