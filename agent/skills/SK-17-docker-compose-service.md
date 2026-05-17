## SK-17 · Docker Compose Service

### Trigger
Agent thêm service mới vào docker-compose hoặc modify resource limits.

### Inputs Required
- Service name + image tag
- Env vars + secrets
- Dependency services
- Healthcheck endpoint

### Rules
```
[R1] Mọi service có deploy.resources.limits (cpu + memory)
[R2] TZ=Asia/Ho_Chi_Minh env var bắt buộc
[R3] SPRING_PROFILES_ACTIVE lấy từ env: ${SPRING_PROFILES:dev}
[R4] healthcheck bắt buộc cho tất cả Spring Boot service
[R5] depends_on với condition: service_healthy (không chỉ service_started)
[R6] Log driver: json-file với max-size và max-file giới hạn
[R7] Network: eprocure-net (bridge, internal)
[R8] Không expose port DB/Redis ra host ở prod profile
```

### Template
```yaml
# Service snippet cho docker-compose.yml
  {service-name}:
    image: eprocure/{service-name}:${IMAGE_TAG:-latest}
    container_name: eprocure-{service-name}
    restart: unless-stopped
    environment:
      - TZ=Asia/Ho_Chi_Minh
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES:-dev}
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=db_{service}
      - DB_USER=${DB_USER}
      - DB_PASS=${DB_PASS}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - API_KEY=${INTERNAL_API_KEY}
      - ENCRYPTION_ENABLED=${ENCRYPTION_ENABLED:-false}
      - TZ=Asia/Ho_Chi_Minh
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - eprocure-net
    deploy:
      resources:
        limits:
          cpus: '0.25'
          memory: 512M
        reservations:
          cpus: '0.1'
          memory: 256M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

### Checklist
```
[ ] Resource limits được set
[ ] TZ=Asia/Ho_Chi_Minh trong env
[ ] depends_on với condition: service_healthy
[ ] healthcheck có cấu hình
```
