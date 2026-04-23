# 👤 Manual de Usuario

## 1. Acceso a la aplicación

Abre un navegador y navega a:
- **Local**: `http://localhost:8080`
- **Docker**: `http://localhost:8080`
- **Render**: `https://car-pooling-service.onrender.com`

## 2. Interfaz principal

La interfaz se divide en 4 zonas:

```
┌────────────────────────────────────────────────────┐
│  🚗 Car Pooling Service    [🇪🇸 ES][🇬🇧 EN]  ● Online │  ← Cabecera
├─────────────┬──────────────────────────────────────┤
│  Formularios│   Estadísticas + Flota + Cola        │  ← Zona principal
│             │                                      │
├─────────────┴──────────────────────────────────────┤
│  🖥 Consola de trazas                              │  ← Log
└────────────────────────────────────────────────────┘
```

### 2.1 Indicador de estado

En la esquina superior derecha hay un badge que indica:
- 🟢 **Online** — El servicio responde correctamente
- 🔴 **Offline** — No se puede conectar al servicio

### 2.2 Selector de idioma

Botones en la cabecera: 🇪🇸 ES | 🇬🇧 EN | 🇫🇷 FR | 🇩🇪 DE. El idioma se guarda automáticamente.

## 3. Operaciones

### 3.1 Cargar flota de coches

1. En el panel **🚘 Cargar flota**, edita el JSON con los coches.
2. Cada coche necesita `id` (entero) y `seats` (4, 5 o 6).
3. Pulsa **⬆ Cargar coches**.

```json
[
  {"id": 1, "seats": 4},
  {"id": 2, "seats": 6},
  {"id": 3, "seats": 5}
]
```

> ⚠️ Cargar una flota nueva **elimina todos los viajes y coches previos**.

### 3.2 Registrar un grupo de viaje

1. En el panel **🧑‍🤝‍🧑 Nuevo viaje**, introduce:
   - **ID del grupo**: número positivo único
   - **Personas**: de 1 a 6
2. Pulsa **➕ Solicitar viaje**.

Resultado posible:
- ✅ **Asignado** — El grupo aparece en un coche de la flota.
- ⏳ **En espera** — No hay coche disponible; aparece en la cola.
- ❌ **Error** — ID duplicado, datos inválidos, etc.

### 3.3 Localizar un grupo

1. En el panel **🔍 Localizar / Abandonar**, introduce el ID del grupo.
2. Pulsa **📍 Localizar**.

Resultado:
- Se muestra el coche asignado (ID y asientos).
- Si el grupo está en espera, se informa que no tiene coche.
- Si el grupo no existe, aparece error.

### 3.4 Abandonar (dropoff)

1. En el panel **🔍 Localizar / Abandonar**, introduce el ID del grupo.
2. Pulsa **🚪 Abandonar**.

Resultado:
- El grupo se elimina del sistema.
- Si tenía coche, los asientos se liberan.
- Los grupos en cola se reasignan automáticamente si hay sitio.

## 4. Panel de estadísticas

| Tarjeta | Significado |
|---|---|
| 🔵 **Coches** | Total de coches en la flota |
| 🟢 **Asientos libres** | Asientos disponibles en toda la flota |
| 🟣 **Viajes activos** | Grupos con coche asignado |
| 🟠 **En espera** | Grupos en cola sin coche |

## 5. Visualización de la flota

Cada coche se muestra como una tarjeta con:
- ID del coche y número de asientos
- Iconos de asientos: 🟢 libre, 🔵 ocupado
- Lista de grupos asignados al coche

## 6. Cola de espera

Muestra los grupos que esperan coche, en orden de llegada, con el número de personas.

## 7. Consola de trazas

La consola inferior muestra el log de operaciones en tiempo real:

- **Filtros**: ALL | Info | Warn | Error | Debug
- **Auto-scroll**: activa/desactiva el scroll automático al fondo
- **🗑 Limpiar**: borra todas las trazas

Los niveles de log:
| Nivel | Color | Ejemplo |
|---|---|---|
| **INFO** | Azul | Operaciones exitosas (carga, asignación, dropoff) |
| **WARN** | Amarillo | Grupo en cola, duplicado rechazado |
| **ERROR** | Rojo | Errores de conexión, respuestas inesperadas |
| **DEBUG** | Gris | Detalle de asientos, localización |

## 8. Mensajes de error (popups)

Los errores se muestran como notificaciones toast en la esquina superior derecha:

| Situación | Mensaje |
|---|---|
| ID de grupo duplicado | "El grupo con ID X ya está registrado" |
| Grupo no encontrado | "Grupo no encontrado" |
| Asientos fuera de rango | "Seats must be at least 4" |
| Personas fuera de rango | "People must be at least 1" |
| JSON mal formado | "Error en el formato del JSON" |
| Servicio caído | "Error de conexión" |
