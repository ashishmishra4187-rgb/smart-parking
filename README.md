# Smart Parking Management System

A real-time parking reservation system: drivers reserve slots, a camera reads
number plates at the gate, and managers monitor everything live.

## Tech stack
- Backend: Java 21, Spring Boot 4.1, Spring Data JPA, PostgreSQL 18
- Frontend (planned): React + TypeScript
- Vision / ML (planned): Python, OpenCV, EasyOCR, scikit-learn

## Project status
Week 1 complete: database, entities, seed data, REST read endpoints, vehicle registration.

## Folder layout
- `backend/` Spring Boot application
- `frontend/` React app (not started)
- `cv/` number plate reading (not started)
- `ml/` occupancy prediction (not started)
- `practice/` small learning experiments

## How to run the backend
1. Install JDK 21, PostgreSQL 18 and Maven (the project includes a wrapper).
2. In PostgreSQL create a login and database:
```sql
   CREATE ROLE parking_user WITH LOGIN PASSWORD '<your-password>';
   CREATE DATABASE parking_db OWNER parking_user;
   GRANT ALL ON SCHEMA public TO parking_user;
```
3. Create `backend/src/main/resources/application-local.yml` (this file is
   git-ignored and never committed):
```yaml
   spring:
     datasource:
       username: parking_user
       password: <your-password>
```
4. Start the app from `backend/`: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
5. Open `http://localhost:8080/api/health`. Expect `{"status":"UP","database":"UP"}`.

On first start the app creates the tables and seeds one lot, 3 sections and 24 slots.

## API (so far)
| Method | URL | Purpose |
|---|---|---|
| GET | `/api/health` | App and database status |
| GET | `/api/parking/lots` | Lots with live slot counts |
| GET | `/api/parking/lots/{id}/slots` | All slots of a lot |
| POST | `/api/vehicles` | Register a vehicle (temporary, no login yet) |
| GET | `/api/vehicles` | List vehicles |

## Known limitations
- No authentication yet (a temporary demo user owns all vehicles).
- `ddl-auto: update` is used for development convenience.