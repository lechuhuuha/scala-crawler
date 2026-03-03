Phase 1 — DB schema + state machine tables (small updates)
Goal: database supports jobs/urls/runs/tasks/robots/sitemaps exactly as your model.
Create tables:
jobs, job_runs, sitemaps, job_urls, url_runs, robots_cache, plus tasks (for lease/complete)
Encode enums as text values (easier to inspect during demo)
Add indexes:
tasks(status, leased_at), job_urls(job_id, status, priority), url_runs(job_url_id, run_index)
Job config defaulting to max_audit_urls = 10
Robots fallback mode (so you can explain “allow all on invalid robots” clearly)
jobs.config_json jsonb (or equivalent)
must include: { "max_audit_urls": 10, "audit_runs_per_url": 3, ... }
robots_cache.mode text with values: NORMAL | ALLOW_ALL_FALLBACK
robots_cache.last_error text (nullable)

DoD:
migrations apply cleanly, ndexes for leasing/reporting, and you can insert a dummy job

Prompt
In services/control-plane, implement SQL migrations to create tables:
jobs, job_runs, sitemaps, job_urls, url_runs, robots_cache, tasks.
Use the following states exactly:
- jobs.status: CREATED, DISCOVERING_SITEMAPS, PARSING_SITEMAPS, BUILDING_URLSET, AUDITING, OVERLOADED, PAUSED, COMPLETED, FAILED
- job_urls.status: DISCOVERED, SKIPPED_ROBOTS, ELIGIBLE, ADMITTED, QUEUED, RUNNING, DONE, DEFERRED, FAILED
- url_runs.status: PENDING, LEASED, SUCCEEDED, RETRYABLE_FAILED, FAILED
- tasks.status: PENDING, LEASED, DONE
- jobs.config_json jsonb with defaults: max_audit_urls=10, audit_runs_per_url=3
- robots_cache.mode text enum-like (NORMAL, ALLOW_ALL_FALLBACK)
- robots_cache.last_error text nullable
Add sensible indexes for leasing and reporting.