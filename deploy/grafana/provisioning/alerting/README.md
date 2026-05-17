# Grafana Alerting Baseline

Prometheus loads alert rules from `deploy/prometheus/rules/*.yml`. Grafana can either display those Prometheus rules through the configured Prometheus datasource or later manage notification policies directly.

Minimum production wiring:

- Route `critical` alerts to the on-call channel.
- Route `warning` alerts to the service owner channel.
- Add inhibition so `BlogCloudServiceDown` suppresses noisy latency/error alerts for the same job.
- Add contact points for email, IM, or incident tooling outside this repository.
- Enable RabbitMQ, MySQL, and Elasticsearch exporters before relying on infrastructure metric alerts.

Local validation:

```powershell
docker compose run --rm --no-deps prometheus promtool check rules /etc/prometheus/rules/blog-cloud-alerts.yml
docker compose restart prometheus
curl.exe -fsS http://localhost:9090/-/ready
```
