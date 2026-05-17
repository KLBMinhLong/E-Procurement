# Spring Profiles Strategy

## Profile files
- application.yml: base config
- application-local.yml: local dev, encryption OFF
- application-dev.yml: docker dev, DEBUG logs, encryption OFF
- application-prod.yml: production, encryption ON

## Rules
- No secrets in repo
- Use env vars with defaults: ${ENV_VAR:default}
- Keep config keys consistent across profiles
