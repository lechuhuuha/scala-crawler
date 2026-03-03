Phase 6 — Median-of-3 aggregation + reporting
Goal: when 3 runs succeed, compute median, store aggregated result, expose /report.
URL becomes DONE only when all 3 runs SUCCEEDED (matches your accuracy definition)
/jobs/{id}/report returns:
per-URL median scores
job summary averages
DoD:
report is stable and fast (uses aggregated column, not scanning full JSON)

Prompt
Implement aggregation:
- When all 3 url_runs for a job_url are SUCCEEDED, compute median per score field and mark job_url DONE
- Store aggregated median scores in job_urls (json column)
Implement GET /jobs/{id}/report to return per-URL medians and a job summary.
Ensure report queries avoid scanning lighthouse_json blobs.