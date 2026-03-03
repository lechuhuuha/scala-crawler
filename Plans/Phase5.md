Phase 5 — Worker (Node) runs Lighthouse headless + returns JSON
Goal: stateless worker that executes Lighthouse CLI and posts results.

Use Lighthouse CLI options from official repo/docs.
Run headless Chrome via --chrome-flags="--headless=new ..." if needed.
Output JSON (--output json --output-path -) and post to control-plane
Extract scores:
performance/accessibility/best-practices/seo (from Lighthouse JSON categories)
DoD:
worker leases tasks and completes them end-to-end for real URLs

Prompt
In services/worker:
- Implement a loop: POST /tasks/lease, run lighthouse for the URL, POST /tasks/{taskId}/complete
- Run lighthouse in headless mode and return JSON output
- Extract category scores (performance, accessibility, best-practices, seo) into extracted_scores
- On network/timeouts, return an error classification that the control-plane treats as retryable; other errors non-retryable.
Provide a Dockerfile for the worker that includes Node + lighthouse + Chromium dependencies.