# TLS Certificate Placeholder

This directory is mounted read-only by the optional `https-proxy` Compose profile.

Expected production files:

- `fullchain.pem`: certificate chain for the public domain.
- `privkey.pem`: private key for the certificate.

Do not commit real private keys. For local HTTPS testing, place temporary self-signed files here and run:

```powershell
docker compose --profile https up -d https-proxy
```

The default stack does not start `https-proxy`, so missing certificate files do not break normal local development.
