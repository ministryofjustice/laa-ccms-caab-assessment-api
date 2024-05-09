package uk.gov.laa.ccms.caab.assessment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

public abstract class BaseAssessmentControllerIntegrationTest {

  @Autowired
  private AssessmentController assessmentController;

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
        Arguments.of(List.of("assessment1"), "owner1", "1234567890", "status1"),
        Arguments.of(List.of("assessment1"), null, null, null),
        Arguments.of(null, "owner1", null, null),
        Arguments.of(null, null, "1234567890", null),
        Arguments.of(null, null, null, "status1"),
        Arguments.of(null, null, null, null)
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
        Arguments.of(List.of("assessment2"), "owner2", "1234567891", "status2"),
        Arguments.of(List.of("assessment2"), null, null, null),
        Arguments.of(null, "owner2", null, null),
        Arguments.of(null, null, "1234567891", null),
        Arguments.of(null, null, null, "status2")
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
  public void testGetAssessments_expect200_empty() {
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
  public void testPatchAssessments_expect200_empty() {

    final Long id  = 26L;
    ResponseEntity<AssessmentDetail> beforePatchResponse = assessmentController.getAssessment(id);

    PatchAssessmentDetail patch = new PatchAssessmentDetail()
        .caseReferenceNumber("9876543210")
        .name("assessment2")
        .status("status2");

    ResponseEntity<Void> response =
        assessmentController.updateAssessment(26L, "testUser", patch);

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






}
