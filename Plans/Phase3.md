Phase 3 — Robots + sitemap discovery/merge (+ .xml.gz)
Goal: given a domain, discover all sitemaps from robots.txt, merge them, parse sitemapindex/urlset (including .xml.gz).
Fetch https://{domain}/robots.txt, extract all Sitemap: lines, store in sitemaps
robots fetch failure -> allow all + persist: robots_cache.rules_blob = "", robots_cache.mode = ALLOW_ALL_FALLBACK, robots_cache.last_error = <message>
Fallback to https://{domain}/sitemap.xml if none found
Support:
sitemapindex.xml recursion with max_sitemap_recursion_depth (default from jobs.config_json)
.xml.gz via gzip decode
Robots parsing/matching:
Use a Scala robots parser library (e.g., robotparser-scala).
Evaluate custom UA (e.g., MiniSiteAuditBot/0.1) before marking URL ELIGIBLE
DoD:
POST /jobs populates sitemaps + job_urls (DISCOVERED/ELIGIBLE/SKIPPED_ROBOTS)
Multiple sitemaps are merged (dedupe)

Prompt
Implement domain -> sitemap discovery:
- Fetch robots.txt, extract ALL Sitemap: URLs, merge many
- If robots.txt fetch fails or parsing fails/invalid, treat as ALLOW ALL.
- Persist robots_cache.mode=ALLOW_ALL_FALLBACK and robots_cache.last_error.
Then mark URLs as:
- SKIPPED_ROBOTS when explicitly disallowed by valid robots rules
- ELIGIBLE otherwise (including allow-all fallback)
- Fallback to /sitemap.xml when none
Implement sitemap parsing:
- support sitemapindex recursion (bounded by max_sitemap_recursion_depth)
- support .xml.gz by gzip decoding
Populate job_urls with normalized URLs and computed path_depth and priority (homepage first then smaller path_depth).
Implement robots gating using custom UA "MiniSiteAuditBot/0.1" with a Scala robots.txt parser library.
Persist everything in Postgres.
