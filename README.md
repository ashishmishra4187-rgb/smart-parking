# Smart Parking Management System

**Java 21 · Spring Boot · PostgreSQL · React · TypeScript · OpenCV · EasyOCR**

A driver reserves a parking space through the web app. At the gate, a Python service reads a number plate and can send an entry or exit event to the backend. Managers have a separate dashboard and gate view.

| Follow the workflow | Explore the implementation |
| --- | --- |
| Find a lot, inspect slots and manage a reservation | [Driver screens](frontend/src/pages) · [Reservation API](backend/src/main/java/com/parking/backend/reservation) |
| Keep an open slot view in sync | [React WebSocket hook](frontend/src/useSlotsSocket.ts) |
| Read a plate and submit a gate event | [Plate reader](cv/plate_reader.py) · [Gate client](cv/gate_client.py) · [Gate API](backend/src/main/java/com/parking/backend/gate) |

### Inside the plate reader

![Plate preprocessing example](cv/out/1_original.png) → ![Cropped plate](cv/out/6_crop.png)

The repository includes labeled synthetic plates with blur, noise, tilt and low light. From `cv/`, run `python -m pip install -r requirements.txt` and `python read_plates.py --only blur` to compare OCR output with the expected registrations.

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

## Run the frontend

From `frontend/`, run `npm ci` and `npm run dev`. Open the Vite URL, register a driver, add a vehicle and inspect a lot. The backend defaults to port 8080 and allows the frontend origin `http://localhost:5173`.

## Development notes

PostgreSQL schema updates use Hibernate in the local profile. The camera and sample-image tools live in [cv/](cv/). The [practice directory](practice/) contains separate learning exercises.

## Deploy the web app and gate API

Use Docker Compose on a server with persistent disk. The stack runs PostgreSQL 18, the Java backend, and an Nginx frontend. The frontend serves browser routes and proxies both API requests and WebSocket connections. Configure one backend replica for the in-memory WebSocket broker.

Create an untracked `.env` beside `compose.yaml` and set these values:

| Variable | Value |
| --- | --- |
| PUBLIC_ORIGIN | Exact browser origin, for example `https://parking.example.com`, without a trailing slash |
| DB_PASSWORD | Random database password |
| JWT_SECRET | Random signing secret of at least 32 bytes |
| PARKING_GATE_KEY | Random shared gate key of at least 16 characters |
| MANAGER_PASSWORD | Strong initial password for the seeded manager account |

Run `docker compose up -d --build`. The web service listens on the host loopback address at port 8088. Point an HTTPS reverse proxy at `127.0.0.1:8088`, preserve the browser origin, and enable WebSocket upgrades for `/ws`. For a local Compose check use `PUBLIC_ORIGIN=http://localhost:8088`. Verify `/api/health`, driver registration, and a reservation. The initial manager login is `manager@parking.local`. The seed password applies when that account is created.

PostgreSQL stores its data in the named volume `parking-data`. Back up the database before upgrading because this version uses Hibernate schema updates. Install the Python camera client at the physical gate with its camera dependencies, set `PARKING_API_URL` to the HTTPS origin and `PARKING_GATE_KEY` to the backend's gate key. A registered driver needs an active reservation to enter.
