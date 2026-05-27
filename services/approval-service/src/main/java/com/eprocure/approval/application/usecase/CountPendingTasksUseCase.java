package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.service.ApprovalInboxCount;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CountPendingTasksUseCase {
    private static final Logger log = LogManager.getLogger(CountPendingTasksUseCase.class);

    private final ApprovalProcessRepository approvalProcessRepository;

    public CountPendingTasksUseCase(ApprovalProcessRepository approvalProcessRepository) {
        this.approvalProcessRepository = approvalProcessRepository;
    }

    @Transactional(readOnly = true)
    public ApprovalInboxCount execute(UUID userId) {
        log.info("[ACTION] Start CountPendingTasks | userId={}", userId);

        long total = approvalProcessRepository.countPendingTasks(userId, null, null, null, null);
        long overdue = approvalProcessRepository.countPendingTasks(userId, null, null, null, true);
        long emergency = approvalProcessRepository.countPendingTasks(userId, "EMERGENCY", null, null, null);

        ApprovalInboxCount count = new ApprovalInboxCount(total, overdue, emergency);

        log.info("[ACTION] Complete CountPendingTasks | userId={} | total={} | overdue={} | emergency={}",
                userId, total, overdue, emergency);
        return count;
    }
}
