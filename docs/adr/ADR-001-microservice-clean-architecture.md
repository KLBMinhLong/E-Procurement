## ADR-001 — Kiến trúc Microservices với Clean Architecture

**Status:** Accepted  
**Date:** 2025-01  
**Deciders:** Engineering Lead, Architect

### Bối cảnh
Hệ thống E-Procurement có nhiều bounded context rõ ràng (IAM, PR, Approval, Finance, Inventory, Notification). Cần lựa chọn giữa Monolith, Modular Monolith, và Microservices.

### Quyết định
Sử dụng **Microservices** với mỗi service tuân thủ **Clean Architecture**:

```
┌──────────────────────────────────────────────────────┐
│  Presentation Layer  (Controller, Request/Response DTO)│
├──────────────────────────────────────────────────────┤
│  Application Layer   (UseCase, ApplicationService)    │
├──────────────────────────────────────────────────────┤
│  Domain Layer        (Entity, ValueObject, Repository │
│                       Interface, DomainService, Event)│
├──────────────────────────────────────────────────────┤
│  Infrastructure Layer(RepositoryImpl, MyBatis Mapper, │
│                       KafkaProducer, RedisAdapter)    │
└──────────────────────────────────────────────────────┘
```

**Quy tắc quan trọng:**
- Domain Layer là **POJO thuần** — không import Spring, không annotation framework
- Dependency Inversion: Infrastructure implements Domain interfaces
- Không để logic nghiệp vụ trong Controller
- UseCase = một hành động nghiệp vụ = một class

### Hậu quả
- (+) Mỗi service có thể scale, deploy độc lập
- (+) Bounded context rõ ràng, dễ maintain dài hạn
- (-) Overhead vận hành cao hơn (nhiều service cần monitor)
- (-) Distributed transaction phức tạp hơn → xử lý bằng Saga/Event-driven
- **Giải pháp:** Docker Compose cho dev, giới hạn RAM/CPU chặt chẽ cho môi trường 8GB RAM