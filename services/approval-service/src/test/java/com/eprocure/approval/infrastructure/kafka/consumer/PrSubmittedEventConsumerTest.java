package com.eprocure.approval.infrastructure.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.approval.application.port.in.StartApprovalProcessCommand;
import com.eprocure.approval.application.service.StartApprovalProcessResult;
import com.eprocure.approval.application.usecase.StartApprovalProcessUseCase;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class PrSubmittedEventConsumerTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TRACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void should_start_approval_process_when_pr_submitted_event_is_consumed() {
        CapturingStartApprovalProcessUseCase useCase = new CapturingStartApprovalProcessUseCase();
        PrSubmittedEventConsumer consumer = new PrSubmittedEventConsumer(useCase, objectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "procurement.pr.submitted",
                2,
                45L,
                PR_ID.toString(),
                eventJson());

        consumer.consume(record);

        StartApprovalProcessCommand command = useCase.command;
        assertThat(command).isNotNull();
        assertThat(command.eventId()).isEqualTo("evt-pr-submitted-001");
        assertThat(command.topic()).isEqualTo("procurement.pr.submitted");
        assertThat(command.partitionId()).isEqualTo(2);
        assertThat(command.offsetValue()).isEqualTo(45L);
        assertThat(command.traceId()).isEqualTo(TRACE_ID);
        assertThat(command.purchaseRequestId()).isEqualTo(PR_ID);
        assertThat(command.prNumber()).isEqualTo("PR-2026-05-00001");
        assertThat(command.title()).isEqualTo("Mua laptop Dell XPS 15 cho team phat trien");
        assertThat(command.requesterId()).isEqualTo(REQUESTER_ID);
        assertThat(command.departmentId()).isEqualTo(DEPARTMENT_ID);
        assertThat(command.totalAmount().amount()).isEqualByComparingTo("12000000.0000");
        assertThat(command.categories()).containsExactlyInAnyOrder("IT_HARDWARE", "SAAS");
        assertThat(command.priority()).isEqualTo(PurchaseRequestPriority.NORMAL);
        assertThat(command.entitySnapshot()).containsEntry("prNumber", "PR-2026-05-00001");
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String eventJson() {
        return """
                {
                  "eventId": "evt-pr-submitted-001",
                  "eventType": "PURCHASE_REQUEST_SUBMITTED",
                  "version": "1.0",
                  "source": "purchase-request-service",
                  "timestamp": "2026-05-27T02:00:00Z",
                  "traceId": "%s",
                  "payload": {
                    "purchaseRequestId": "%s",
                    "prNumber": "PR-2026-05-00001",
                    "title": "Mua laptop Dell XPS 15 cho team phat trien",
                    "requesterId": "%s",
                    "departmentId": "%s",
                    "priority": "NORMAL",
                    "totalAmount": {
                      "amount": "12000000.0000",
                      "currency": "VND"
                    },
                    "categories": ["IT_HARDWARE", "SAAS"]
                  }
                }
                """.formatted(TRACE_ID, PR_ID, REQUESTER_ID, DEPARTMENT_ID);
    }

    private static final class CapturingStartApprovalProcessUseCase extends StartApprovalProcessUseCase {
        private StartApprovalProcessCommand command;

        private CapturingStartApprovalProcessUseCase() {
            super(null, null, null, null, null, Clock.systemUTC());
        }

        @Override
        public StartApprovalProcessResult execute(StartApprovalProcessCommand command) {
            this.command = command;
            return StartApprovalProcessResult.skippedEvent();
        }
    }
}
