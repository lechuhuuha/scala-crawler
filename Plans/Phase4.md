Phase 4 — Admission + task creation + worker leasing (core engine)
Goal: select URLs (SEO-first), admit under caps, create 3 runs, create tasks, lease tasks safely.
SEO-first selection:
homepage first
then increasing path_depth

Admission logic:
Use max_audit_urls from job config (default 10)
ELIGIBLE -> ADMITTED for top-N under caps (N = max_audit_urls)
remaining ELIGIBLE -> DEFERRED with reason (cap hit / overload)
Create 3 url_runs (run_index 1..3) + 3 tasks per admitted URL

Leasing strategy:
SELECT ... FOR UPDATE SKIP LOCKED to safely lease tasks concurrently
Lease TTL: if not completed within TTL, return to PENDING

DoD:
calling lease repeatedly returns tasks without duplicate leasing
tasks move PENDING -> LEASED -> DONE via complete endpoint

Prompt
Implement URL admission + task generation:
- max_audit_urls should default to 10 (from jobs.config_json) and be used to admit URLs.
- Admit homepage + shallow paths first up to max_audit_urls.
- Non-admitted eligible URLs become DEFERRED (postpone), not dropped.
- For each admitted URL, create 3 url_runs and 3 tasks (one per run_index).
- Implement POST /tasks/lease using SELECT FOR UPDATE SKIP LOCKED to lease one task.
- Implement lease TTL recovery (leased tasks that expired become PENDING again).
- Implement POST /tasks/{taskId}/complete to store lighthouse_json (jsonb), extracted scores, duration, and mark run/task statuses.
Only retry network/timeouts: classify errors accordingly.
