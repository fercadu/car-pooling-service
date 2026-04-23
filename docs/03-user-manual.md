# 👤 User Manual

## 1. Accessing the Application

Open a browser and navigate to:
- **Local**: `http://localhost:8080`
- **Docker**: `http://localhost:8080`
- **Render**: `https://car-pooling-service.onrender.com`

## 2. Main Interface

The interface is divided into 4 zones:

```
┌────────────────────────────────────────────────────┐
│  🚗 Car Pooling Service    [🇪🇸 ES][🇬🇧 EN]  ● Online │  ← Header
├─────────────┬──────────────────────────────────────┤
│    Forms    │   Statistics + Fleet + Queue          │  ← Main area
│             │                                      │
├─────────────┴──────────────────────────────────────┤
│  🖥 Log Console                                    │  ← Log
└────────────────────────────────────────────────────┘
```

### 2.1 Status Indicator

In the upper-right corner, a badge indicates:
- 🟢 **Online** — The service is responding correctly
- 🔴 **Offline** — Cannot connect to the service

### 2.2 Language Selector

Buttons in the header: 🇪🇸 ES | 🇬🇧 EN | 🇫🇷 FR | 🇩🇪 DE. The language is saved automatically.

## 3. Operations

### 3.1 Load Car Fleet

1. In the **🚘 Load Fleet** panel, edit the JSON with the cars.
2. Each car needs `id` (integer) and `seats` (4, 5, or 6).
3. Click **⬆ Load Cars**.

```json
[
  {"id": 1, "seats": 4},
  {"id": 2, "seats": 6},
  {"id": 3, "seats": 5}
]
```

> ⚠️ Loading a new fleet **deletes all previous journeys and cars**.

### 3.2 Register a Journey Group

1. In the **🧑‍🤝‍🧑 New Journey** panel, enter:
   - **Group ID**: unique positive number
   - **People**: from 1 to 6
2. Click **➕ Request Journey**.

Possible outcomes:
- ✅ **Assigned** — The group appears in a car in the fleet.
- ⏳ **Waiting** — No car available; appears in the queue.
- ❌ **Error** — Duplicate ID, invalid data, etc.

### 3.3 Locate a Group

1. In the **🔍 Locate / Dropoff** panel, enter the group ID.
2. Click **📍 Locate**.

Result:
- Shows the assigned car (ID and seats).
- If the group is waiting, it reports no car assigned.
- If the group does not exist, an error appears.

### 3.4 Dropoff

1. In the **🔍 Locate / Dropoff** panel, enter the group ID.
2. Click **🚪 Dropoff**.

Result:
- The group is removed from the system.
- If it had a car, the seats are freed.
- Waiting groups are automatically reassigned if space is available.

## 4. Statistics Panel

| Card | Meaning |
|---|---|
| 🔵 **Cars** | Total cars in the fleet |
| 🟢 **Free Seats** | Available seats across the entire fleet |
| 🟣 **Active Journeys** | Groups with an assigned car |
| 🟠 **Waiting** | Groups in queue without a car |

## 5. Fleet Visualization

Each car is displayed as a card with:
- Car ID and number of seats
- Seat icons: 🟢 free, 🔵 occupied
- List of groups assigned to the car

## 6. Waiting Queue

Shows groups waiting for a car, in arrival order, with the number of people.

## 7. Log Console

The bottom console shows the operation log in real time:

- **Filters**: ALL | Info | Warn | Error | Debug
- **Auto-scroll**: toggle automatic scrolling to the bottom
- **🗑 Clear**: clears all traces

Log levels:
| Level | Color | Example |
|---|---|---|
| **INFO** | Blue | Successful operations (load, assignment, dropoff) |
| **WARN** | Yellow | Group queued, duplicate rejected |
| **ERROR** | Red | Connection errors, unexpected responses |
| **DEBUG** | Gray | Seat details, locate calls |

## 8. Error Messages (popups)

Errors are shown as toast notifications in the upper-right corner:

| Situation | Message |
|---|---|
| Duplicate group ID | "Group with ID X is already registered" |
| Group not found | "Group not found" |
| Seats out of range | "Seats must be at least 4" |
| People out of range | "People must be at least 1" |
| Malformed JSON | "Error in JSON format" |
| Service down | "Connection error" |
