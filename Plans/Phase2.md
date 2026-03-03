Phase 2 — Control-plane HTTP API skeleton (no crawler yet)
Goal: endpoints exist and manipulate DB/state transitions.

Implement these endpoints in Akka HTTP (quickstart style).
POST /jobs {domain} should persist config defaults:
- config_json.max_audit_urls = 10
- config_json.audit_runs_per_url = 3

GET /jobs/{id} should return status, run_no, and config (including caps)
GET /jobs/{id}/report (returns placeholder)
POST /jobs/{id}/resume (placeholder)

Worker endpoints:
POST /tasks/lease
POST /tasks/{taskId}/complete

DoD:
You can create a job and see it in DB
Worker can lease "no tasks" without error

Prompt
Implement Akka HTTP routes for:
POST /jobs (domain input), GET /jobs/{id}, GET /jobs/{id}/report (placeholder), POST /jobs/{id}/resume (placeholder),
POST /tasks/lease, POST /tasks/{taskId}/complete.
Use DB persistence for jobs and tasks (even if tasks are empty for now).
Return clear JSON responses and HTTP status codes.
POST /jobs should persist job config defaults: max_audit_urls=10 and audit_runs_per_url=3.
GET /jobs/{id} should return config (including max_audit_urls) and run_no.
