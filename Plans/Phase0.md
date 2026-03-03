Phase 0 — Repo skeleton + tooling
Goal: a runnable monorepo with Scala control-plane + Node worker + Postgres.
Create folders:
services/control-plane/ (Scala + Akka HTTP)
services/worker/ (Node + Lighthouse CLI)
deploy/k8s/ + deploy/argocd/
docker-compose.yml (local run: control-plane + worker + postgres)
Use Akka HTTP quickstart structure as baseline.
Add Scala test framework (ScalaTest) + DB migration tool (Flyway or Liquibase—pick one).
DoD:
docker compose up brings up Postgres + placeholder services

Prompt
Create a monorepo structure with:
- services/control-plane (Scala sbt project) using Akka HTTP (server), JSON (circe or spray-json), ScalaTest
- services/worker (Node project) to run lighthouse CLI
- docker-compose.yml to run postgres + both services
Include minimal README with local run instructions.
Use Akka HTTP quickstart style for the Scala service.