package com.eprocure.approval.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.stream.Stream;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BpmnDiagramResourceTest {

    @ParameterizedTest
    @MethodSource("bpmnResources")
    @DisplayName("BPMN có process executable và diagram metadata để mở bằng Camunda Modeler")
    void should_parse_bpmn_and_include_diagram_metadata_when_resource_is_loaded(String resourcePath) {
        // Given
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(inputStream).as("BPMN resource must exist").isNotNull();

        // When
        BpmnModelInstance modelInstance = Bpmn.readModelFromStream(inputStream);

        // Then
        assertThat(modelInstance.getModelElementsByType(Process.class))
                .singleElement()
                .satisfies(process -> assertThat(process.isExecutable()).isTrue());
        assertThat(modelInstance.getModelElementsByType(BpmnDiagram.class)).isNotEmpty();
        assertThat(modelInstance.getModelElementsByType(BpmnPlane.class)).isNotEmpty();
        assertThat(modelInstance.getModelElementsByType(BpmnShape.class)).isNotEmpty();
        assertThat(modelInstance.getModelElementsByType(BpmnEdge.class)).isNotEmpty();
    }

    static Stream<String> bpmnResources() {
        return Stream.of(
                "bpmn/pr-approval-process.bpmn",
                "bpmn/emergency-approval.bpmn");
    }
}
