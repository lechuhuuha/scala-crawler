# Scala Crawler Monorepo

Monorepo for a control-plane and worker architecture:
- `services/control-plane`: Scala + Akka HTTP + Flyway + ScalaTest
- `services/worker`: Node worker (Lighthouse-ready skeleton)
- `deploy/k8s`: Kubernetes base manifests
- `deploy/argocd`: Argo CD Applications

## Local Run (Phase 0)

Prerequisites:
- Docker + Docker Compose

Start stack:

```bash
docker compose up --build
```

Services:
- Control-plane: `http://localhost:8080/health`
- Worker: `http://localhost:3000/health`
- Postgres: `localhost:5432`

Stop stack:

```bash
docker compose down -v
```

## Notes

This is the Phase 0 baseline from `Plans/` and intentionally starts with placeholder runtime behavior.
