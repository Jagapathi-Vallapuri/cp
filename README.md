# Distributed Code Judge

A small distributed “code judge” system:

- A React/Vite frontend for submitting code.
- A Spring Boot backend that stores submissions in Postgres and publishes jobs to RabbitMQ.
- A C++ worker (built as a Docker image) that consumes jobs from RabbitMQ, executes code, and publishes results.

## Repository layout

- [client/](client/) — React + Vite UI (see [client/README.md](client/README.md))
- [code_judge/](code_judge/) — Spring Boot API (see [code_judge/HELP.md](code_judge/HELP.md))
- [CodeExecutor/](CodeExecutor/) — C++ “judge-worker” container
- [docker-compose.yml](docker-compose.yml) — Postgres + RabbitMQ + worker services
- [stress_test.py](stress_test.py) — simple load generator for the submit endpoint

## Architecture (high level)

1. The frontend calls the backend `POST /api/submit` with code, problemId, username, and language.
2. The backend stores a `Submission` with status `PENDING` and publishes a message to the RabbitMQ queue `submission_queue`.
3. One of the C++ workers consumes the job, executes it, determines a verdict, and publishes to `result_queue`.
4. The backend listens to `result_queue` and updates the `Submission` as `COMPLETED`.

## Local development

### Prerequisites

- Docker Desktop (for Postgres/RabbitMQ/worker)
- Node.js 18+ (for the frontend)
- JDK 25 (the backend declares `java.version=25` in [code_judge/pom.xml](code_judge/pom.xml))

### Ports

- Backend API: `http://localhost:8080`
- Frontend dev server: `http://localhost:5173`
- Postgres: `localhost:5433` (mapped to container `5432`)
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ management UI: `http://localhost:15672` (default creds `guest` / `guest`)

### 1) Start Postgres + RabbitMQ + workers

From the repo root:

- Start infra:
  - `docker compose up -d postgres rabbitmq`
- Build and run workers:
  - `docker compose up -d --build judge-worker`

Scaling workers:

- If you are using Docker Compose (not Swarm), `deploy.replicas` is ignored; scale with:
  - `docker compose up -d --build --scale judge-worker=5 judge-worker`

### 2) Run the backend (Spring Boot)

From the repo root:

- `cd code_judge`
- Windows: `mvnw.cmd spring-boot:run`
- macOS/Linux: `./mvnw spring-boot:run`

The backend reads connection settings from [code_judge/src/main/resources/application.properties](code_judge/src/main/resources/application.properties) and is preconfigured for the Docker Compose ports.

### 3) Seed at least one Problem row

Submissions reference a `problemId`. If there is no `Problem` in the database, the backend will reject submissions.

Example SQL (creates `id=1`):

```sql
INSERT INTO problem (title, description, test_input, expected_output, time_limit_seconds, memory_limit_mb)
VALUES (
  'Sample: Echo',
  'Print the input exactly.',
  'hello\n',
  'hello\n',
  2.0,
  256
);
```

You can connect to Postgres at `localhost:5433` using:

- DB: `judge_db`
- User: `postgres`
- Password: `password`

### 4) Run the frontend

From the repo root:

- `cd client`
- `npm install`
- `npm run dev`

The backend enables CORS for `http://localhost:5173` (see `@CrossOrigin` in the submission controller).

## API

### Submit code

- `POST http://localhost:8080/api/submit`

Example:

```bash
curl -X POST http://localhost:8080/api/submit \
  -H "Content-Type: application/json" \
  -d '{
    "problemId": 1,
    "username": "demo",
    "language": "python",
    "code": "print(\"hello\")\n"
  }'
```

### Poll submission status

- `GET http://localhost:8080/api/submit/{uuid}`

```bash
curl http://localhost:8080/api/submit/<submission-uuid>
```

## Stress test

The script [stress_test.py](stress_test.py) submits many jobs concurrently.

- Install deps: `pip install requests`
- Run: `python stress_test.py`

Adjust `TOTAL_JOBS` and `SLEEP_TIME` inside the script to change load.

## Troubleshooting

- If workers are not processing jobs, check RabbitMQ queues in `http://localhost:15672`.
- If scaling doesn’t work, use `docker compose up --scale ...` (Compose ignores `deploy.replicas`).
- If the backend cannot connect to Postgres/RabbitMQ, verify containers are healthy and that the mapped ports in [docker-compose.yml](docker-compose.yml) match your local config.
