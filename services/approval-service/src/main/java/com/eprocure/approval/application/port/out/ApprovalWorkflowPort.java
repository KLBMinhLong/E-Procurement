package com.eprocure.approval.application.port.out;

import java.util.Map;
import java.util.Objects;

public interface ApprovalWorkflowPort {
    StartedWorkflow start(StartWorkflowCommand command);

    record StartWorkflowCommand(
            String processDefinitionKey,
            String businessKey,
            Map<String, Object> variables) {
        public StartWorkflowCommand {
            if (processDefinitionKey == null || processDefinitionKey.isBlank()) {
                throw new IllegalArgumentException("processDefinitionKey must not be blank");
            }
            if (businessKey == null || businessKey.isBlank()) {
                throw new IllegalArgumentException("businessKey must not be blank");
            }
            variables = variables == null ? Map.of() : Map.copyOf(variables);
        }
    }

    record StartedWorkflow(String processInstanceId) {
        public StartedWorkflow {
            processInstanceId = Objects.requireNonNull(processInstanceId, "processInstanceId must not be null");
            if (processInstanceId.isBlank()) {
                throw new IllegalArgumentException("processInstanceId must not be blank");
            }
        }
    }
}
