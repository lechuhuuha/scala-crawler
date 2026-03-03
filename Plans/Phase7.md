Phase 7 — Overload controller (error-rate spikes) + defer + manual resume
Goal: error spikes tighten caps; remaining URLs become DEFERRED; manual resume re-admits later.
Overload signal: error-rate over a sliding window
When overloaded:
job status AUDITING -> OVERLOADED
tighten caps (stepwise)
new admissions become DEFERRED
When auditing finishes and deferred exists -> job PAUSED
Manual resume:
POST /jobs/{id}/resume increments run_no, re-admits DEFERRED URLs using current caps
DoD:
you can force overload (e.g., low timeouts) and observe DEFERRED + PAUSED + resume flow

Prompt
Implement overload controller in control-plane:
- Every 2 minutes compute error_rate over last 10 minutes (only if finished_runs >= 50).
- Enter overload when error_rate > threshold for 2 consecutive windows; tighten caps stepwise.
- While overloaded, do not reduce Lighthouse quality; instead defer new URLs (ELIGIBLE -> DEFERRED).
- When admitted URLs finish and deferred exists, set job status PAUSED.
- Implement POST /jobs/{id}/resume to start a new job_run and re-admit deferred URLs under current caps.
