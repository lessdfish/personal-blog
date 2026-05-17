# Load Test Baseline

Use these scripts after the local stack is healthy or before production release against a staging environment.

## Smoke

```powershell
k6 run deploy/loadtest/k6-smoke.js
```

Optional environment overrides:

```powershell
$env:BASE_URL="https://api.example.com"
$env:WEB_URL="https://www.example.com"
$env:LOGIN_USERNAME="staging-user"
$env:LOGIN_PASSWORD="staging-password"
k6 run deploy/loadtest/k6-smoke.js
```

The default login request uses invalid credentials and only asserts that the endpoint does not return 5xx. Use a staging-only account if you want to measure successful login.

## Baseline

```powershell
k6 run deploy/loadtest/k6-baseline.js
```

Initial acceptance threshold:

- Error rate below 1%.
- Public read p95 below 800 ms.
- Public read p99 below 1500 ms.
- Checks above 99%.

Record every run with:

- Commit SHA.
- Deployment environment.
- Container resource profile.
- Database size.
- VU profile and duration.
- p95, p99, requests per second, and error rate.

Do not run write-heavy tests against production without a rollback plan and explicit approval.
