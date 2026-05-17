## ADR-005 — MyBatis thay vì JPA/Hibernate

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Hệ thống có nhiều query phức tạp: approval chain calculation, budget aggregation, 3-way match. JPA/Hibernate khó kiểm soát N+1, lazy loading issues trong môi trường phức tạp.

### Quyết định
Sử dụng **MyBatis** với annotation + XML mapper:

- Annotation cho CRUD đơn giản
- XML mapper cho query phức tạp (JOIN nhiều bảng, dynamic SQL)
- `ObjectMapper` (Jackson) để chuyển đổi DTO ↔ Domain

```java
// Interface trong Domain Layer (POJO)
public interface PurchaseRequestRepository {
    Optional<PurchaseRequest> findById(UUID id);
    List<PurchaseRequest> findPendingByApprover(UUID approverId, PageRequest page);
}

// Implementation trong Infrastructure Layer
@Repository
public class PurchaseRequestRepositoryImpl implements PurchaseRequestRepository {
    @Autowired private PurchaseRequestMapper mapper; // MyBatis
    // ...
}
```

### Hậu quả
- (+) SQL rõ ràng, kiểm soát hoàn toàn performance
- (+) Dễ debug, dễ optimize từng query
- (+) Phù hợp với Clean Architecture (không pollute domain với ORM annotation)
- (-) Nhiều boilerplate hơn JPA cho CRUD đơn giản
- (-) Developer cần biết SQL tốt
