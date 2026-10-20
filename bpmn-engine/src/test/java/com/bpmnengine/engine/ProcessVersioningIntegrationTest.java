package com.bpmnengine.engine;

import com.bpmnengine.api.dto.*;
import com.bpmnengine.persistence.entity.ProcessInstanceEntity;
import com.bpmnengine.persistence.repository.ProcessDefinitionRepository;
import com.bpmnengine.persistence.repository.ProcessInstanceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for process versioning, suspend/activate, and definition management.
 * Runs against H2 in-memory database.
 */
@SpringBootTest
@Transactional
class ProcessVersioningIntegrationTest {

    private static final String BPMN_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "             targetNamespace=\"http://bpmnengine.com\">" +
            "  <process id=\"%s\" name=\"%s\" isExecutable=\"true\">" +
            "    <startEvent id=\"start\"/>" +
            "    <sequenceFlow id=\"sf1\" sourceRef=\"start\" targetRef=\"end\"/>" +
            "    <endEvent id=\"end\"/>" +
            "  </process>" +
            "</definitions>";

    @Autowired ProcessEngine engine;
    @Autowired ProcessDefinitionRepository definitionRepo;
    @Autowired ProcessInstanceRepository instanceRepo;

    private String bpmnV1(String key) {
        return String.format(BPMN_TEMPLATE, key, key + " v1");
    }

    private String bpmnV2(String key) {
        return String.format(BPMN_TEMPLATE, key, key + " v2");
    }

    private DeployProcessRequest req(String key, String xml, String deployedBy, String msg) {
        DeployProcessRequest r = new DeployProcessRequest();
        r.setBpmnXml(xml);
        r.setDeployedBy(deployedBy);
        r.setDeployMessage(msg);
        return r;
    }

    // ── Deploy ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("First deployment creates version 1 as active")
    void deploy_firstTime_createsVersion1Active() {
        ProcessDefinitionDto dto = engine.deployProcess(req("vtest1", bpmnV1("vtest1"), "alice", "initial"));

        assertThat(dto.getVersion()).isEqualTo(1);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.isSuspended()).isFalse();
        assertThat(dto.getDeployedBy()).isEqualTo("alice");
        assertThat(dto.getDeployMessage()).isEqualTo("initial");
    }

    @Test
    @DisplayName("Second deployment of same key creates version 2 and deactivates version 1")
    void deploy_secondTime_createsVersion2AndDeactivatesV1() {
        engine.deployProcess(req("vtest2", bpmnV1("vtest2"), "alice", "v1"));
        ProcessDefinitionDto dto2 = engine.deployProcess(req("vtest2", bpmnV2("vtest2"), "bob", "v2"));

        assertThat(dto2.getVersion()).isEqualTo(2);
        assertThat(dto2.isActive()).isTrue();

        // V1 must now be inactive
        ProcessDefinitionDto v1 = engine.getDefinitionByVersion("vtest2", 1);
        assertThat(v1.isActive()).isFalse();
    }

    @Test
    @DisplayName("Legacy deployProcess(resourceName, xml) still works and returns definition ID")
    void deploy_legacyOverload_returnsId() {
        String id = engine.deployProcess("legacy.bpmn", bpmnV1("legacy1"));
        assertThat(id).isNotBlank();
        ProcessDefinitionDto dto = engine.getDefinitionById(id);
        assertThat(dto.getKey()).isEqualTo("legacy1");
    }

    // ── List / search ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listDefinitions returns one entry per key (active version only)")
    void list_returnsOnePerKey() {
        engine.deployProcess(req("list1", bpmnV1("list1"), null, null));
        engine.deployProcess(req("list1", bpmnV2("list1"), null, null)); // v2 now active
        engine.deployProcess(req("list2", bpmnV1("list2"), null, null));

        List<ProcessDefinitionDto> all = engine.listDefinitions();
        // Filter to just our test keys (other tests may run first)
        long list1Count = all.stream().filter(d -> "list1".equals(d.getKey())).count();
        long list2Count = all.stream().filter(d -> "list2".equals(d.getKey())).count();

        assertThat(list1Count).isEqualTo(1);
        assertThat(list2Count).isEqualTo(1);

        ProcessDefinitionDto list1Active = all.stream()
                .filter(d -> "list1".equals(d.getKey())).findFirst().orElseThrow();
        assertThat(list1Active.getVersion()).isEqualTo(2);
        assertThat(list1Active.isActive()).isTrue();
    }

    @Test
    @DisplayName("searchDefinitions finds by key substring")
    void search_byKey_returnsMatches() {
        engine.deployProcess(req("searchable-proc", bpmnV1("searchable-proc"), null, null));
        engine.deployProcess(req("another-proc", bpmnV1("another-proc"), null, null));

        List<ProcessDefinitionDto> results = engine.searchDefinitions("searchable");
        assertThat(results).extracting(ProcessDefinitionDto::getKey)
                .contains("searchable-proc")
                .doesNotContain("another-proc");
    }

    // ── Get definition ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveDefinition returns the active version with BPMN XML")
    void getActive_returnsActiveVersionWithXml() {
        engine.deployProcess(req("active1", bpmnV1("active1"), null, null));
        engine.deployProcess(req("active1", bpmnV2("active1"), null, null));

        ProcessDefinitionDto dto = engine.getActiveDefinition("active1");
        assertThat(dto.getVersion()).isEqualTo(2);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getBpmnXml()).isNotBlank();
    }

    @Test
    @DisplayName("getActiveDefinition throws for unknown key")
    void getActive_unknownKey_throws() {
        assertThatThrownBy(() -> engine.getActiveDefinition("no-such-key"))
                .isInstanceOf(EngineException.class)
                .hasMessageContaining("no-such-key");
    }

    @Test
    @DisplayName("getDefinitionByVersion returns the correct version with BPMN XML")
    void getByVersion_returnsCorrectVersion() {
        engine.deployProcess(req("byver1", bpmnV1("byver1"), null, null));
        engine.deployProcess(req("byver1", bpmnV2("byver1"), null, null));

        ProcessDefinitionDto v1 = engine.getDefinitionByVersion("byver1", 1);
        assertThat(v1.getVersion()).isEqualTo(1);
        assertThat(v1.getBpmnXml()).contains("byver1 v1");

        ProcessDefinitionDto v2 = engine.getDefinitionByVersion("byver1", 2);
        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v2.getBpmnXml()).contains("byver1 v2");
    }

    @Test
    @DisplayName("getDefinitionById returns the correct definition")
    void getById_returnsCorrectDefinition() {
        ProcessDefinitionDto deployed = engine.deployProcess(req("byid1", bpmnV1("byid1"), null, null));
        ProcessDefinitionDto found = engine.getDefinitionById(deployed.getId());
        assertThat(found.getId()).isEqualTo(deployed.getId());
        assertThat(found.getKey()).isEqualTo("byid1");
    }

    // ── Version history ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getVersionHistory returns all versions newest first")
    void versionHistory_returnsAllVersions() {
        engine.deployProcess(req("hist1", bpmnV1("hist1"), "alice", "v1"));
        engine.deployProcess(req("hist1", bpmnV2("hist1"), "bob", "v2"));

        ProcessVersionSummaryDto summary = engine.getVersionHistory("hist1");
        assertThat(summary.getKey()).isEqualTo("hist1");
        assertThat(summary.getLatestVersion()).isEqualTo(2);
        assertThat(summary.getActiveVersion()).isEqualTo(2);
        assertThat(summary.getTotalVersions()).isEqualTo(2);
        assertThat(summary.getVersions()).hasSize(2);
        assertThat(summary.getVersions().get(0).getVersion()).isEqualTo(2); // newest first
        assertThat(summary.getVersions().get(1).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("getBpmnXml returns the raw XML for the requested version")
    void getBpmnXml_returnsCorrectXml() {
        engine.deployProcess(req("xmltest1", bpmnV1("xmltest1"), null, null));
        String xml = engine.getBpmnXml("xmltest1", 1);
        assertThat(xml).contains("xmltest1");
    }

    // ── setActiveVersion ──────────────────────────────────────────────────────

    @Test
    @DisplayName("setActiveVersion changes the active flag to the requested version")
    void setActive_changesActiveVersion() {
        engine.deployProcess(req("setact1", bpmnV1("setact1"), null, null));
        engine.deployProcess(req("setact1", bpmnV2("setact1"), null, null));
        // v2 is active; switch back to v1
        ProcessDefinitionDto result = engine.setActiveVersion("setact1", 1);

        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();

        assertThat(engine.getActiveDefinition("setact1").getVersion()).isEqualTo(1);
        assertThat(engine.getDefinitionByVersion("setact1", 2).isActive()).isFalse();
    }

    // ── deleteVersion ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteVersion removes the specified version")
    void deleteVersion_removesIt() {
        engine.deployProcess(req("delv1", bpmnV1("delv1"), null, null));
        engine.deployProcess(req("delv1", bpmnV2("delv1"), null, null));
        // Delete v1 (inactive — safe)
        engine.deleteVersion("delv1", 1);

        assertThatThrownBy(() -> engine.getDefinitionByVersion("delv1", 1))
                .isInstanceOf(EngineException.class);
        assertThat(engine.getVersionHistory("delv1").getTotalVersions()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteVersion of active version promotes previous version to active")
    void deleteVersion_activeVersion_promotesPrevious() {
        engine.deployProcess(req("delact1", bpmnV1("delact1"), null, null));
        engine.deployProcess(req("delact1", bpmnV2("delact1"), null, null));
        // v2 is active — delete it
        engine.deleteVersion("delact1", 2);

        // v1 should now be active
        assertThat(engine.getActiveDefinition("delact1").getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteVersion throws when running instances exist")
    void deleteVersion_withRunningInstances_throws() {
        engine.deployProcess(req("delrun1", bpmnV1("delrun1"), null, null));
        // start an instance (it completes immediately — simple flow — so no blocker here;
        // we manually set it to RUNNING via repo)
        ProcessDefinitionDto def = engine.getActiveDefinition("delrun1");
        ProcessInstanceEntity inst = new ProcessInstanceEntity();
        inst.setId(java.util.UUID.randomUUID().toString());
        inst.setProcessDefinitionId(def.getId());
        inst.setProcessDefinitionKey("delrun1");
        inst.setState(ProcessInstanceEntity.State.RUNNING);
        inst.setStartTime(java.time.Instant.now());
        inst.setCreatedAt(java.time.Instant.now());
        inst.setUpdatedAt(java.time.Instant.now());
        instanceRepo.save(inst);

        assertThatThrownBy(() -> engine.deleteVersion("delrun1", 1))
                .isInstanceOf(EngineException.class)
                .hasMessageContaining("running instance");
    }

    // ── deleteAllVersions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAllVersions removes all versions of a key")
    void deleteAll_removesAllVersions() {
        engine.deployProcess(req("delall1", bpmnV1("delall1"), null, null));
        engine.deployProcess(req("delall1", bpmnV2("delall1"), null, null));

        engine.deleteAllVersions("delall1");

        assertThat(definitionRepo.findAllVersionsByKey("delall1")).isEmpty();
    }

    @Test
    @DisplayName("deleteAllVersions throws when any running instance exists")
    void deleteAll_withRunning_throws() {
        engine.deployProcess(req("delallrun1", bpmnV1("delallrun1"), null, null));
        ProcessDefinitionDto def = engine.getActiveDefinition("delallrun1");
        ProcessInstanceEntity inst = new ProcessInstanceEntity();
        inst.setId(java.util.UUID.randomUUID().toString());
        inst.setProcessDefinitionId(def.getId());
        inst.setProcessDefinitionKey("delallrun1");
        inst.setState(ProcessInstanceEntity.State.RUNNING);
        inst.setStartTime(java.time.Instant.now());
        inst.setCreatedAt(java.time.Instant.now());
        inst.setUpdatedAt(java.time.Instant.now());
        instanceRepo.save(inst);

        assertThatThrownBy(() -> engine.deleteAllVersions("delallrun1"))
                .isInstanceOf(EngineException.class)
                .hasMessageContaining("running instance");
    }

    // ── Suspend / activate ────────────────────────────────────────────────────

    @Test
    @DisplayName("suspendProcessDefinition marks all versions suspended")
    void suspend_allVersions_marked() {
        engine.deployProcess(req("susp1", bpmnV1("susp1"), null, null));
        engine.deployProcess(req("susp1", bpmnV2("susp1"), null, null));

        engine.suspendProcessDefinition("susp1");

        definitionRepo.findAllVersionsByKey("susp1")
                .forEach(d -> assertThat(d.isSuspended()).isTrue());
    }

    @Test
    @DisplayName("activateProcessDefinition clears suspended flag on all versions")
    void activate_allVersions_cleared() {
        engine.deployProcess(req("reac1", bpmnV1("reac1"), null, null));
        engine.suspendProcessDefinition("reac1");
        engine.activateProcessDefinition("reac1");

        definitionRepo.findAllVersionsByKey("reac1")
                .forEach(d -> assertThat(d.isSuspended()).isFalse());
    }

    @Test
    @DisplayName("suspendProcessDefinitionVersion marks only that version")
    void suspendVersion_onlyOneVersion() {
        engine.deployProcess(req("suspv1", bpmnV1("suspv1"), null, null));
        engine.deployProcess(req("suspv1", bpmnV2("suspv1"), null, null));

        engine.suspendProcessDefinitionVersion("suspv1", 1);

        assertThat(engine.getDefinitionByVersion("suspv1", 1).isSuspended()).isTrue();
        assertThat(engine.getDefinitionByVersion("suspv1", 2).isSuspended()).isFalse();
    }

    @Test
    @DisplayName("activateProcessDefinitionVersion clears suspended on only that version")
    void activateVersion_onlyOneVersion() {
        engine.deployProcess(req("actv1", bpmnV1("actv1"), null, null));
        engine.deployProcess(req("actv1", bpmnV2("actv1"), null, null));
        engine.suspendProcessDefinition("actv1");

        engine.activateProcessDefinitionVersion("actv1", 2);

        assertThat(engine.getDefinitionByVersion("actv1", 1).isSuspended()).isTrue();
        assertThat(engine.getDefinitionByVersion("actv1", 2).isSuspended()).isFalse();
    }

    @Test
    @DisplayName("Starting a suspended definition throws EngineException")
    void startSuspended_throws() {
        engine.deployProcess(req("startsus1", bpmnV1("startsus1"), null, null));
        engine.suspendProcessDefinition("startsus1");

        assertThatThrownBy(() -> engine.startProcessByKey("startsus1", Map.of()))
                .isInstanceOf(EngineException.class)
                .hasMessageContaining("suspended");
    }

    // ── startProcessByKeyAndVersion / startProcessById ────────────────────────

    @Test
    @DisplayName("startProcessByKeyAndVersion starts the specified version")
    void startByKeyAndVersion_usesSpecifiedVersion() {
        ProcessDefinitionDto v1 = engine.deployProcess(req("startver1", bpmnV1("startver1"), null, null));
        engine.deployProcess(req("startver1", bpmnV2("startver1"), null, null)); // v2 now active

        // Explicitly start v1
        ProcessInstanceResult result = engine.startProcessByKeyAndVersion("startver1", 1, null, Map.of());
        assertThat(result).isNotNull();
        assertThat(result.getProcessDefinitionId()).isEqualTo(v1.getId());
    }

    @Test
    @DisplayName("startProcessById starts by definition ID")
    void startById_usesCorrectDefinition() {
        ProcessDefinitionDto dto = engine.deployProcess(req("startbyid1", bpmnV1("startbyid1"), null, null));
        ProcessInstanceResult result = engine.startProcessById(dto.getId(), null, Map.of());
        assertThat(result.getProcessDefinitionId()).isEqualTo(dto.getId());
    }

    @Test
    @DisplayName("startProcessByKey uses the active version, not the latest")
    void startByKey_usesActiveVersion() {
        ProcessDefinitionDto v1dto = engine.deployProcess(req("startact1", bpmnV1("startact1"), null, null));
        engine.deployProcess(req("startact1", bpmnV2("startact1"), null, null)); // v2 active
        // Roll back to v1
        engine.setActiveVersion("startact1", 1);

        ProcessInstanceResult result = engine.startProcessByKey("startact1", Map.of());
        assertThat(result.getProcessDefinitionId()).isEqualTo(v1dto.getId());
    }
}
