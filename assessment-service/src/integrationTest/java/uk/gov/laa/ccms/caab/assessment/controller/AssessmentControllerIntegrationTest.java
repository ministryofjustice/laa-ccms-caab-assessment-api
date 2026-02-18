package uk.gov.laa.ccms.caab.assessment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import uk.gov.laa.ccms.caab.assessment.OracleContainerIntegrationTest;
import static uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl.currentUserHolder;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

@SpringBootTest
@Sql(executionPhase = BEFORE_TEST_CLASS, scripts = "/sql/assessment_tables_create_schema.sql")
@Sql(executionPhase = AFTER_TEST_CLASS, scripts = "/sql/assessment_tables_drop_schema.sql")
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = "/sql/delete_data.sql")
@SqlMergeMode(MergeMode.MERGE)
public class AssessmentControllerIntegrationTest extends
    OracleContainerIntegrationTest {

  @Autowired
  private AssessmentController assessmentController;

  protected final String caabUserLoginId = "audit@user.com";

  @Test
  @Transactional
  public void testCreateAssessment_expect201() {

    currentUserHolder.set(caabUserLoginId);

    AssessmentDetail assessment = new AssessmentDetail()
        .name("assessment1")
        .providerId("owner1")
        .caseReferenceNumber("1234567890")
        .status("status1");

    ResponseEntity<Void> response =
        assessmentController.createAssessment(caabUserLoginId, assessment);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  //get by id expect 200
  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessment_expect200() {
    final Long id  = 26L;
    ResponseEntity<AssessmentDetail> response = assessmentController.getAssessment(id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getCheckpoint());
  }

  private static Stream<Arguments> getAssessmentsArguments() {

    return Stream.of(
        // Means assessment cases
        Arguments.of(List.of("meansAssessment"), "owner1", "1234567890", "status1"),
        Arguments.of(List.of("meansAssessment"), null, null, null),
        Arguments.of(null, "owner1", null, null),
        Arguments.of(null, null, "1234567890", null),
        Arguments.of(null, null, null, "status1"),

        // Merits assessment cases
        Arguments.of(List.of("meritsAssessment"), "owner2", "1234567891", "status2"),
        Arguments.of(List.of("meritsAssessment"), null, null, null),
        Arguments.of(null, "owner2", null, null),
        Arguments.of(null, null, "1234567891", null),
        Arguments.of(null, null, null, "status2"),

        // Billing assessment cases
        Arguments.of(List.of("billingAssessment"), "owner3", "1234567892", "status3"),
        Arguments.of(List.of("billingAssessment"), null, null, null),
        Arguments.of(null, "owner3", null, null),
        Arguments.of(null, null, "1234567892", null),
        Arguments.of(null, null, null, "status3"),

        Arguments.of(List.of("poaAssessment"), "owner4", "1234567893", "status4"),
        Arguments.of(List.of("poaAssessment"), null, null, null),
        Arguments.of(null, "owner4", null, null),
        Arguments.of(null, null, "1234567893", null),
        Arguments.of(null, null, null, "status4")
    );
  }

  @ParameterizedTest
  @MethodSource("getAssessmentsArguments")
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessments_expect200(
      final List<String> names,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {

    ResponseEntity<AssessmentDetails> response =
        assessmentController.getAssessments(names, providerId, caseReferenceNumber, status);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getContent().size());
  }

  private static Stream<Arguments> getAssessmentsEmptyArguments() {

    return Stream.of(
        Arguments.of(List.of("assessment2"), "random1", "1234567891", "status2"),
        Arguments.of(List.of("assessment2"), null, null, null),
        Arguments.of(null, "random1", null, null),
        Arguments.of(null, null, "0987654321", null),
        Arguments.of(null, null, null, "randomStatus")
    );
  }

  @ParameterizedTest
  @MethodSource("getAssessmentsEmptyArguments")
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessments_expect200_empty(
      final List<String> names,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {

    ResponseEntity<AssessmentDetails> response =
        assessmentController.getAssessments(names, providerId, caseReferenceNumber, status);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, response.getBody().getContent().size());
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testDeleteAssessment() {
    final String providerId = "owner1";
    final String caseReferenceNumber = "1234567890";

    ResponseEntity<AssessmentDetails> beforeDeleteResponse =
        assessmentController.getAssessments(null, null , null, null);

    ResponseEntity<Void> response = assessmentController.deleteAssessments(
        "test", providerId, caseReferenceNumber, null, null);

    ResponseEntity<AssessmentDetails> afterDeleteResponse =
        assessmentController.getAssessments(null, null , null, null);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    int beforeSize = beforeDeleteResponse.getBody().getContent().size();
    int afterSize = afterDeleteResponse.getBody().getContent().size();

    assertTrue(beforeSize > afterSize);
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testPatchAssessments_expect200_empty() {

    final Long id  = 26L;
    ResponseEntity<AssessmentDetail> beforePatchResponse = assessmentController.getAssessment(id);

    PatchAssessmentDetail patch = new PatchAssessmentDetail()
        .caseReferenceNumber("9876543210")
        .name("assessment2")
        .status("status2");

    ResponseEntity<Void> response =
        assessmentController.patchAssessment(26L, "testUser", patch);

    ResponseEntity<AssessmentDetail> afterPatchResponse = assessmentController.getAssessment(id);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    String beforeCaseReferenceNumber = beforePatchResponse.getBody().getCaseReferenceNumber();
    String afterCaseReferenceNumber = afterPatchResponse.getBody().getCaseReferenceNumber();
    String beforeName = beforePatchResponse.getBody().getName();
    String afterName = afterPatchResponse.getBody().getName();
    String beforeStatus = beforePatchResponse.getBody().getStatus();
    String afterStatus = afterPatchResponse.getBody().getStatus();

    //check patching amended values
    assertNotEquals(beforeCaseReferenceNumber, afterCaseReferenceNumber);
    assertNotEquals(beforeName, afterName);
    assertNotEquals(beforeStatus, afterStatus);

    //check patching did not amend values
    String beforeProviderId = beforePatchResponse.getBody().getProviderId();
    String afterProviderId = afterPatchResponse.getBody().getProviderId();
    List<AssessmentEntityTypeDetail> beforeEntityTypes = beforePatchResponse.getBody().getEntityTypes();
    List<AssessmentEntityTypeDetail> afterEntityTypes = afterPatchResponse.getBody().getEntityTypes();

    assertEquals(beforeProviderId, afterProviderId);
    assertEquals(beforeEntityTypes, afterEntityTypes);
  }
  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testDeleteCheckpoint_returns204() {
    final Long id = 26L;
    ResponseEntity<Void> response = assessmentController.deleteAssessmentCheckpoint(id, "testUser");
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    ResponseEntity<AssessmentDetail> assessmentResponse = assessmentController.getAssessment(id);
    assertNotNull(assessmentResponse.getBody());
    assertNull(assessmentResponse.getBody().getCheckpoint());
  }

  private static Stream<Arguments> updateAssessmentArguments_base() {
    return Stream.of(
        Arguments.of(
            new AssessmentDetail().id(26L)
                .name("new_assessment1")
                .status("new_status1")
                .providerId("new_owner1")
                .caseReferenceNumber("987654321")
        )
    );
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testUpdateAssessment() {
    final Long assessmentId = 26L;
    currentUserHolder.set(caabUserLoginId);

    AssessmentDetail expected = buildAssessmentDetail();

    ResponseEntity<Void> response = assessmentController.updateAssessment(assessmentId, caabUserLoginId, expected);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    ResponseEntity<AssessmentDetail> updatedResponse = assessmentController.getAssessment(expected.getId());
    assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());

    AssessmentDetail actual = updatedResponse.getBody();
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getStatus(), actual.getStatus());
    assertEquals(expected.getProviderId(), actual.getProviderId());
    assertEquals(expected.getCaseReferenceNumber(), actual.getCaseReferenceNumber());

    assertEquals(expected.getEntityTypes().size(), actual.getEntityTypes().size());
    for (AssessmentEntityTypeDetail expectedEntityType : expected.getEntityTypes()) {
      AssessmentEntityTypeDetail actualEntityType = actual.getEntityTypes().stream()
          .filter(a -> a.getId().equals(expectedEntityType.getId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("EntityType not found"));

      assertEquals(expectedEntityType.getName(), actualEntityType.getName());
      assertEquals(expectedEntityType.getId(), actualEntityType.getId());
      assertEquals(expectedEntityType.getEntities().size(), actualEntityType.getEntities().size());

      for (AssessmentEntityDetail expectedEntity : expectedEntityType.getEntities()) {
        AssessmentEntityDetail actualEntity = actualEntityType.getEntities().stream()
            .filter(a -> a.getId().equals(expectedEntity.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Entity not found"));

        assertEquals(expectedEntity.getName(), actualEntity.getName());
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getAttributes().size(), actualEntity.getAttributes().size());

        for (AssessmentAttributeDetail expectedAttribute : expectedEntity.getAttributes()) {
          AssessmentAttributeDetail actualAttribute = actualEntity.getAttributes().stream()
              .filter(a -> a.getId().equals(expectedAttribute.getId()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Attribute not found"));

          assertEquals(expectedAttribute.getName(), actualAttribute.getName());
          assertEquals(expectedAttribute.getId(), actualAttribute.getId());
          assertEquals(expectedAttribute.getType(), actualAttribute.getType());
          assertEquals(expectedAttribute.getValue(), actualAttribute.getValue());
          assertEquals(expectedAttribute.getPrepopulated(), actualAttribute.getPrepopulated());
          assertEquals(expectedAttribute.getAsked(), actualAttribute.getAsked());
        }

        assertEquals(expectedEntity.getRelations().size(), actualEntity.getRelations().size());

        for (AssessmentRelationshipDetail expectedRelation : expectedEntity.getRelations()) {
          AssessmentRelationshipDetail actualRelation = actualEntity.getRelations().stream()
              .filter(a -> a.getId().equals(expectedRelation.getId()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Relation not found"));

          assertEquals(expectedRelation.getName(), actualRelation.getName());
          assertEquals(expectedRelation.getId(), actualRelation.getId());
          assertEquals(expectedRelation.getRelationshipTargets().size(), actualRelation.getRelationshipTargets().size());

          for (AssessmentRelationshipTargetDetail expectedTarget : expectedRelation.getRelationshipTargets()) {
            AssessmentRelationshipTargetDetail actualTarget = actualRelation.getRelationshipTargets().stream()
                .filter(a -> a.getId().equals(expectedTarget.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("RelationshipTarget not found"));

            assertEquals(expectedTarget.getId(), actualTarget.getId());
            assertEquals(expectedTarget.getTargetEntityId(), actualTarget.getTargetEntityId());
          }
        }
      }
    }


  }

  public static AssessmentDetail buildAssessmentDetail() {
    return new AssessmentDetail()
        .id(26L)
        .name("new_assessment1")
        .status("new_status1")
        .providerId("new_owner1")
        .caseReferenceNumber("987654321")
        .addEntityTypesItem(buildEntityType());
  }

  public static AssessmentEntityTypeDetail buildEntityType() {
    return new AssessmentEntityTypeDetail()
        .id(27L)
        .name("new_entity_type1")
        .addEntitiesItem(buildEntity());
  }

  public static AssessmentEntityDetail buildEntity() {
    return new AssessmentEntityDetail()
        .id(28L)
        .name("new_entity1")
        .prepopulated(true)
        .addAttributesItem(buildAttribute())
        .addRelationsItem(buildRelation());
  }

  public static AssessmentAttributeDetail buildAttribute() {
    return new AssessmentAttributeDetail()
        .id(29L)
        .name("new_attribute1")
        .type("new_type1")
        .value("new_value1")
        .prepopulated(true)
        .asked(true);
  }

  public static AssessmentRelationshipDetail buildRelation() {
    return new AssessmentRelationshipDetail()
        .id(30L)
        .name("new_relation1")
        .prepopulated(true)
        .addRelationshipTargetsItem(buildRelationTarget());
  }

  public static AssessmentRelationshipTargetDetail buildRelationTarget() {
    return new AssessmentRelationshipTargetDetail()
        .id(31L)
        .targetEntityId("new_entity1");
  }






}
