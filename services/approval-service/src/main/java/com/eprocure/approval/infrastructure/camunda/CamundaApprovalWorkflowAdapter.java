package com.eprocure.approval.infrastructure.camunda;

import com.eprocure.approval.application.port.out.ApprovalWorkflowPort;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

@Component
public class CamundaApprovalWorkflowAdapter implements ApprovalWorkflowPort {
    private final RuntimeService runtimeService;

    public CamundaApprovalWorkflowAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public StartedWorkflow start(StartWorkflowCommand command) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                command.processDefinitionKey(),
                command.businessKey(),
                command.variables());
        return new StartedWorkflow(instance.getProcessInstanceId());
    }
}
