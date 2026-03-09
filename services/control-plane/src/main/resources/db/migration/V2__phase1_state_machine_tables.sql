CREATE TABLE jobs (
  id BIGSERIAL PRIMARY KEY,
  domain TEXT NOT NULL,
  status TEXT NOT NULL CHECK (
    status IN (
      'CREATED',
      'DISCOVERING_SITEMAPS',
      'PARSING_SITEMAPS',
      'BUILDING_URLSET',
      'AUDITING',
      'OVERLOADED',
      'PAUSED',
      'COMPLETED',
      'FAILED'
    )
  ),
  run_no INTEGER NOT NULL DEFAULT 1,
  config_json JSONB NOT NULL DEFAULT '{
    "max_audit_urls": 10,
    "audit_runs_per_url": 3,
    "max_sitemap_recursion_depth": 3,
    "lease_ttl_seconds": 300,
    "error_rate_threshold": 0.25
  }'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE job_runs (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  run_no INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (
    status IN (
      'CREATED',
      'RUNNING',
      'PAUSED',
      'COMPLETED',
      'FAILED'
    )
  ),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (job_id, run_no)
);

CREATE TABLE sitemaps (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  url TEXT NOT NULL,
  source TEXT NOT NULL DEFAULT 'ROBOTS' CHECK (
    source IN ('ROBOTS', 'FALLBACK', 'SITEMAP_INDEX', 'SITEMAP_URLSET')
  ),
  status TEXT NOT NULL DEFAULT 'DISCOVERED' CHECK (
    status IN ('DISCOVERED', 'FETCHED', 'PARSED', 'FAILED')
  ),
  recursion_depth INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (job_id, url)
);

CREATE TABLE robots_cache (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  domain TEXT NOT NULL,
  rules_blob TEXT NOT NULL DEFAULT '',
  mode TEXT NOT NULL DEFAULT 'NORMAL' CHECK (
    mode IN ('NORMAL', 'ALLOW_ALL_FALLBACK')
  ),
  last_error TEXT,
  fetched_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (job_id, domain)
);

CREATE TABLE job_urls (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  sitemap_id BIGINT REFERENCES sitemaps(id) ON DELETE SET NULL,
  url TEXT NOT NULL,
  normalized_url TEXT NOT NULL,
  path_depth INTEGER NOT NULL DEFAULT 0,
  priority INTEGER NOT NULL DEFAULT 1000,
  status TEXT NOT NULL CHECK (
    status IN (
      'DISCOVERED',
      'SKIPPED_ROBOTS',
      'ELIGIBLE',
      'ADMITTED',
      'QUEUED',
      'RUNNING',
      'DONE',
      'DEFERRED',
      'FAILED'
    )
  ),
  defer_reason TEXT,
  aggregated_scores JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (job_id, normalized_url)
);

CREATE TABLE url_runs (
  id BIGSERIAL PRIMARY KEY,
  job_url_id BIGINT NOT NULL REFERENCES job_urls(id) ON DELETE CASCADE,
  run_index INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (
    status IN (
      'PENDING',
      'LEASED',
      'SUCCEEDED',
      'RETRYABLE_FAILED',
      'FAILED'
    )
  ),
  leased_at TIMESTAMPTZ,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  attempt_count INTEGER NOT NULL DEFAULT 0,
  duration_ms INTEGER,
  lighthouse_json JSONB,
  extracted_scores JSONB,
  error_class TEXT,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (run_index >= 1),
  UNIQUE (job_url_id, run_index)
);

CREATE TABLE tasks (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  job_url_id BIGINT REFERENCES job_urls(id) ON DELETE CASCADE,
  url_run_id BIGINT REFERENCES url_runs(id) ON DELETE CASCADE,
  task_type TEXT NOT NULL DEFAULT 'LIGHTHOUSE_AUDIT',
  status TEXT NOT NULL CHECK (
    status IN ('PENDING', 'LEASED', 'DONE')
  ),
  lease_owner TEXT,
  leased_at TIMESTAMPTZ,
  lease_expires_at TIMESTAMPTZ,
  payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  result_json JSONB,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);

CREATE INDEX idx_tasks_status_leased_at
  ON tasks(status, leased_at);

CREATE INDEX idx_tasks_status_lease_expires_at
  ON tasks(status, lease_expires_at);

CREATE INDEX idx_job_urls_job_status_priority
  ON job_urls(job_id, status, priority);

CREATE INDEX idx_url_runs_job_url_run_index
  ON url_runs(job_url_id, run_index);

CREATE INDEX idx_url_runs_status_leased_at
  ON url_runs(status, leased_at);
