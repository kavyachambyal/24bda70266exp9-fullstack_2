# Release Lab

A full-stack, white-background learning dashboard for the CI/CD practical. The frontend talks to a Java 17 / Spring Boot API; Maven runs the API integration tests and Pact consumer contract test, and GitHub Actions builds and publishes a Docker image before requesting a 10% canary deployment.

## Run locally

Requirements: Java 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The Spring Boot app serves the dashboard and its API from the same origin. You can also build and run the container:

```bash
docker build -t release-lab .
docker run --rm -p 8080:8080 release-lab
```

## Try the lab

1. Select **Run pipeline** to create a build and exercise the test workflow.
2. Select **Start canary** to route 10% of traffic to the candidate release.
3. Promote the canary through 25%, 50%, and 100%, or roll it back.
4. Review the activity feed for each operation.

The deployment controls are an in-memory teaching simulation. A real environment should connect them to a deployment controller and its health/metrics checks.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/overview` | Load pipeline checks and deployment state |
| `POST` | `/api/pipeline/run` | Record a successful demo pipeline run |
| `POST` | `/api/deployment/canary` | Start a release at 10% traffic |
| `POST` | `/api/deployment/promote` | Increase traffic to the next canary step |
| `POST` | `/api/deployment/rollback` | Return traffic to the stable release |
| `GET` | `/actuator/health` | Application health check |

Promoting before a canary starts returns `409 Conflict`.

## Automated tests

```bash
mvn verify
```

`PipelineServiceTest` covers business logic, `PipelineControllerTest` exercises API behavior through MockMvc, and `OverviewPactTest` defines and exercises the dashboard-to-API Pact contract. `PostgresContainerTest` starts a real PostgreSQL container for an infrastructure smoke test when Docker is available; it is skipped automatically on machines without Docker. The dashboard is intentionally served by the Spring app, so the consumer contract is represented by a small Java HTTP client in the test.

## GitHub Actions and canary setup

- `.github/workflows/ci.yml` runs Maven verification on pushes and pull requests to `main`, then builds the container image.
- `.github/workflows/cd.yml` verifies the app, publishes an immutable commit-tagged image to GHCR, and asks the configured production deployment webhook to route 10% of traffic to it.
- Configure the repository's `production` environment with `DEPLOY_WEBHOOK_URL` and `DEPLOY_WEBHOOK_TOKEN`. The webhook should perform the canary deployment and return a failing HTTP status if it cannot safely start. Add required reviewers to the environment to gate production deployment.

The webhook is deliberately an integration boundary: Kubernetes, a cloud provider, and production credentials are not assumed by this starter project.
