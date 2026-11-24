package uk.gov.laa.ccms.caab.assessment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl.currentUserHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.laa.ccms.caab.assessment.OracleContainerIntegrationTest;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.repository.OpaSessionRepository;
import uk.gov.laa.ccms.caab.assessment.security.ProviderContext;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtDecoder;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtToken;

@Import(AssessmentControllerIntegrationTest.SecurityTestConfiguration.class)
@Sql(executionPhase = BEFORE_TEST_CLASS, scripts = "/sql/assessment_tables_create_schema.sql")
@Sql(executionPhase = AFTER_TEST_CLASS, scripts = "/sql/assessment_tables_drop_schema.sql")
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = "/sql/delete_data.sql")
@SqlMergeMode(MergeMode.MERGE)
public class AssessmentControllerIntegrationTest extends OracleContainerIntegrationTest {

  private static final String TOKEN = "integration-test-token";
  private static final String FIRM_A = "1001";
  private static final String FIRM_B = "1002";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @LocalServerPort private int port;

  @Autowired private AssessmentController assessmentController;

  @Autowired private AssessmentService assessmentService;

  @Autowired private OpaSessionRepository opaSessionRepository;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  protected final String caabUserLoginId = "audit@user.com";

  @BeforeEach
  void setDefaultProviderContext() {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    ProviderContext.setProviderId(FIRM_A);
  }

  @AfterEach
  void clearThreadLocals() {
    ProviderContext.clear();
    currentUserHolder.remove();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  @Transactional
  public void testCreateAssessment_expect201() {
    currentUserHolder.set(caabUserLoginId);

    AssessmentDetail assessment =
        new AssessmentDetail()
            .name("assessment1")
            .providerId(FIRM_A)
            .caseReferenceNumber("1234567890")
            .status("status1");

    ResponseEntity<Void> response =
        assessmentController.createAssessment(caabUserLoginId, assessment);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessment_expect200() {
    final Long id = 26L;
    ResponseEntity<AssessmentDetail> response = assessmentController.getAssessment(id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getCheckpoint());
  }

  private static Stream<Arguments> getAssessmentsArguments() {
    return Stream.of(
        // Means assessment cases (Firm 1001)
        Arguments.of(FIRM_A, List.of("meansAssessment"), FIRM_A, "1234567890", "status1"),
        Arguments.of(FIRM_A, List.of("meansAssessment"), null, null, null),
        Arguments.of(FIRM_A, null, FIRM_A, null, null),
        Arguments.of(FIRM_A, null, null, "1234567890", null),
        Arguments.of(FIRM_A, null, null, null, "status1"),

        // Merits assessment cases (Firm 1002)
        Arguments.of("1002", List.of("meritsAssessment"), "1002", "1234567891", "status2"),
        Arguments.of("1002", List.of("meritsAssessment"), null, null, null),
        Arguments.of("1002", null, "1002", null, null),
        Arguments.of("1002", null, null, "1234567891", null),
        Arguments.of("1002", null, null, null, "status2"),

        // Billing assessment cases (Firm 1003)
        Arguments.of("1003", List.of("billingAssessment"), "1003", "1234567892", "status3"),
        Arguments.of("1003", List.of("billingAssessment"), null, null, null),
        Arguments.of("1003", null, "1003", null, null),
        Arguments.of("1003", null, null, "1234567892", null),
        Arguments.of("1003", null, null, null, "status3"),

        // POA assessment cases (Firm 1004)
        Arguments.of("1004", List.of("poaAssessment"), "1004", "1234567893", "status4"),
        Arguments.of("1004", List.of("poaAssessment"), null, null, null),
        Arguments.of("1004", null, "1004", null, null),
        Arguments.of("1004", null, null, "1234567893", null),
        Arguments.of("1004", null, null, null, "status4"));
  }

  @ParameterizedTest
  @MethodSource("getAssessmentsArguments")
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessments_expect200(
      final String callerProviderId,
      final List<String> names,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {
    ProviderContext.setProviderId(callerProviderId);

    ResponseEntity<AssessmentDetails> response =
        assessmentController.getAssessments(names, providerId, caseReferenceNumber, status);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals(callerProviderId, response.getBody().getContent().getFirst().getProviderId());
  }

  private static Stream<Arguments> getAssessmentsEmptyArguments() {
    return Stream.of(
        Arguments.of("9999", List.of("assessment2"), "9999", "1234567891", "status2"),
        Arguments.of(FIRM_A, List.of("assessment2"), null, null, null),
        Arguments.of("9999", null, "9999", null, null),
        Arguments.of(FIRM_A, null, null, "0987654321", null),
        Arguments.of(FIRM_A, null, null, null, "randomStatus"),
        // Firm A searching for Firm B's case reference with omitted provider-id sees 0 results
        Arguments.of(FIRM_A, null, null, "1234567891", null));
  }

  @ParameterizedTest
  @MethodSource("getAssessmentsEmptyArguments")
  @Sql(scripts = "/sql/assessments_insert.sql")
  public void testGetAssessments_expect200_empty(
      final String callerProviderId,
      final List<String> names,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {
    ProviderContext.setProviderId(callerProviderId);

    ResponseEntity<AssessmentDetails> response =
        assessmentController.getAssessments(names, providerId, caseReferenceNumber, status);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, response.getBody().getContent().size());
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testDeleteAssessment() {
    final String caseReferenceNumber = "1234567890";

    ResponseEntity<AssessmentDetails> beforeDeleteResponse =
        assessmentController.getAssessments(null, null, null, null);

    ResponseEntity<Void> response =
        assessmentController.deleteAssessments("test", FIRM_A, caseReferenceNumber, null, null);

    ResponseEntity<AssessmentDetails> afterDeleteResponse =
        assessmentController.getAssessments(null, null, null, null);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    int beforeSize = beforeDeleteResponse.getBody().getContent().size();
    int afterSize = afterDeleteResponse.getBody().getContent().size();

    assertTrue(beforeSize > afterSize);
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testPatchAssessments_expect200_empty() {
    final Long id = 26L;
    ResponseEntity<AssessmentDetail> beforePatchResponse = assessmentController.getAssessment(id);

    PatchAssessmentDetail patch =
        new PatchAssessmentDetail()
            .caseReferenceNumber("9876543210")
            .name("assessment2")
            .status("status2");

    ResponseEntity<Void> response = assessmentController.patchAssessment(26L, "testUser", patch);

    ResponseEntity<AssessmentDetail> afterPatchResponse = assessmentController.getAssessment(id);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNotEquals(
        beforePatchResponse.getBody().getCaseReferenceNumber(),
        afterPatchResponse.getBody().getCaseReferenceNumber());
    assertNotEquals(
        beforePatchResponse.getBody().getName(), afterPatchResponse.getBody().getName());
    assertNotEquals(
        beforePatchResponse.getBody().getStatus(), afterPatchResponse.getBody().getStatus());
    assertEquals(
        beforePatchResponse.getBody().getProviderId(),
        afterPatchResponse.getBody().getProviderId());
    assertEquals(
        beforePatchResponse.getBody().getEntityTypes(),
        afterPatchResponse.getBody().getEntityTypes());
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

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  @Transactional
  public void testUpdateAssessment() {
    final Long assessmentId = 26L;
    currentUserHolder.set(caabUserLoginId);

    AssessmentDetail expected = buildAssessmentDetail();

    ResponseEntity<Void> response =
        assessmentController.updateAssessment(assessmentId, caabUserLoginId, expected);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    ResponseEntity<AssessmentDetail> updatedResponse =
        assessmentController.getAssessment(expected.getId());
    assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());

    AssessmentDetail actual = updatedResponse.getBody();
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getStatus(), actual.getStatus());
    assertEquals(expected.getProviderId(), actual.getProviderId());
    assertEquals(expected.getCaseReferenceNumber(), actual.getCaseReferenceNumber());

    assertEquals(expected.getEntityTypes().size(), actual.getEntityTypes().size());
    for (AssessmentEntityTypeDetail expectedEntityType : expected.getEntityTypes()) {
      AssessmentEntityTypeDetail actualEntityType =
          actual.getEntityTypes().stream()
              .filter(a -> a.getId().equals(expectedEntityType.getId()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("EntityType not found"));

      assertEquals(expectedEntityType.getName(), actualEntityType.getName());
      assertEquals(expectedEntityType.getId(), actualEntityType.getId());
      assertEquals(expectedEntityType.getEntities().size(), actualEntityType.getEntities().size());

      for (AssessmentEntityDetail expectedEntity : expectedEntityType.getEntities()) {
        AssessmentEntityDetail actualEntity =
            actualEntityType.getEntities().stream()
                .filter(a -> a.getId().equals(expectedEntity.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entity not found"));

        assertEquals(expectedEntity.getName(), actualEntityType.getEntities().getFirst().getName());
        assertEquals(expectedEntity.getName(), actualEntity.getName());
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getAttributes().size(), actualEntity.getAttributes().size());

        for (AssessmentAttributeDetail expectedAttribute : expectedEntity.getAttributes()) {
          AssessmentAttributeDetail actualAttribute =
              actualEntity.getAttributes().stream()
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
          AssessmentRelationshipDetail actualRelation =
              actualEntity.getRelations().stream()
                  .filter(a -> a.getId().equals(expectedRelation.getId()))
                  .findFirst()
                  .orElseThrow(() -> new AssertionError("Relation not found"));

          assertEquals(expectedRelation.getName(), actualRelation.getName());
          assertEquals(expectedRelation.getId(), actualRelation.getId());
          assertEquals(
              expectedRelation.getRelationshipTargets().size(),
              actualRelation.getRelationshipTargets().size());

          for (AssessmentRelationshipTargetDetail expectedTarget :
              expectedRelation.getRelationshipTargets()) {
            AssessmentRelationshipTargetDetail actualTarget =
                actualRelation.getRelationshipTargets().stream()
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

  @Test
  void httpHealthEndpointIsExemptWhileMissingHeaderOnAssessmentsReturns400() throws Exception {
    HttpResponse<String> health =
        httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, health.statusCode());

    HttpResponse<String> unauthenticated =
        httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/assessments"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(401, unauthenticated.statusCode());

    HttpResponse<String> missingHeader = sendHttp("GET", "/assessments", null, null);
    assertEquals(400, missingHeader.statusCode());
    JsonNode body = OBJECT_MAPPER.readTree(missingHeader.body());
    assertEquals(400, body.get("http_status").asInt());
    assertEquals("Missing required header: Caab-Provider-Id", body.get("error_message").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "abc"})
  void httpRejectsBlankOrNonNumericProviderHeaderWith400(final String invalidHeader)
      throws Exception {
    HttpResponse<String> response = sendHttp("GET", "/assessments", invalidHeader, null);
    assertEquals(400, response.statusCode());
    JsonNode body = OBJECT_MAPPER.readTree(response.body());
    assertEquals(400, body.get("http_status").asInt());
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  void httpGetAndDeleteAssessmentsEnforceProviderMatchAndScopeWhenOmitted() throws Exception {
    assertEquals(
        403, sendHttp("GET", "/assessments?provider-id=" + FIRM_B, FIRM_A, null).statusCode());
    assertEquals(
        403,
        sendHttp(
                "DELETE",
                "/assessments?provider-id=" + FIRM_B + "&case-reference-number=1234567891",
                FIRM_A,
                null)
            .statusCode());

    HttpResponse<String> firstFirmList = sendHttp("GET", "/assessments", FIRM_A, null);
    assertEquals(200, firstFirmList.statusCode());
    AssessmentDetails details =
        OBJECT_MAPPER.readValue(firstFirmList.body(), AssessmentDetails.class);
    assertEquals(1, details.getContent().size());
    assertEquals(26L, details.getContent().getFirst().getId());
    assertEquals(FIRM_A, details.getContent().getFirst().getProviderId());

    ProviderContext.setProviderId(FIRM_A);
    assessmentService.deleteAssessments(new AssessmentDetail(), null);
    assertEquals(3L, opaSessionRepository.count());
    assertTrue(opaSessionRepository.findById(32L).isPresent());
    assertTrue(opaSessionRepository.findById(38L).isPresent());
    assertTrue(opaSessionRepository.findById(44L).isPresent());
  }

  @Test
  void httpPostAssessmentRejectsMismatchedOrMissingBodyProviderIdWith403() throws Exception {
    AssessmentDetail mismatched =
        new AssessmentDetail()
            .name("meansAssessment")
            .providerId(FIRM_B)
            .caseReferenceNumber("1234567890")
            .status("INCOMPLETE");

    HttpResponse<String> response =
        sendHttp("POST", "/assessments", FIRM_A, OBJECT_MAPPER.writeValueAsString(mismatched));
    assertEquals(403, response.statusCode());
    assertEquals(0L, opaSessionRepository.count());
  }

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  void httpSingleIdEndpointsReturn403ForOtherFirmAnd404WhenMissing() throws Exception {
    assertEquals(403, sendHttp("GET", "/assessments/32", FIRM_A, null).statusCode());
    assertEquals(404, sendHttp("GET", "/assessments/9999", FIRM_A, null).statusCode());

    String patchBody =
        OBJECT_MAPPER.writeValueAsString(new PatchAssessmentDetail().status("TAMPERED"));
    assertEquals(403, sendHttp("PATCH", "/assessments/32", FIRM_A, patchBody).statusCode());
    assertEquals(404, sendHttp("PATCH", "/assessments/9999", FIRM_A, patchBody).statusCode());

    String patchMutatingProvider =
        OBJECT_MAPPER.writeValueAsString(new PatchAssessmentDetail().providerId(FIRM_B));
    assertEquals(
        403, sendHttp("PATCH", "/assessments/26", FIRM_A, patchMutatingProvider).statusCode());

    String putBodyFirstFirm =
        OBJECT_MAPPER.writeValueAsString(
            new AssessmentDetail()
                .id(32L)
                .name("meansAssessment")
                .providerId(FIRM_A)
                .caseReferenceNumber("1234567891")
                .status("TAMPERED"));
    assertEquals(403, sendHttp("PUT", "/assessments/32", FIRM_A, putBodyFirstFirm).statusCode());
    String putMissing =
        OBJECT_MAPPER.writeValueAsString(
            new AssessmentDetail()
                .id(9999L)
                .name("meansAssessment")
                .providerId(FIRM_A)
                .caseReferenceNumber("1234567891")
                .status("TAMPERED"));
    assertEquals(404, sendHttp("PUT", "/assessments/9999", FIRM_A, putMissing).statusCode());
    String putMutatingProvider =
        OBJECT_MAPPER.writeValueAsString(
            new AssessmentDetail()
                .id(26L)
                .name("meansAssessment")
                .providerId(FIRM_B)
                .caseReferenceNumber("1234567890")
                .status("TAMPERED"));
    assertEquals(403, sendHttp("PUT", "/assessments/26", FIRM_A, putMutatingProvider).statusCode());

    assertEquals(403, sendHttp("DELETE", "/assessments/32/checkpoint", FIRM_A, null).statusCode());
    assertEquals(
        404, sendHttp("DELETE", "/assessments/9999/checkpoint", FIRM_A, null).statusCode());

    HttpResponse<String> secondFirmView = sendHttp("GET", "/assessments/32", FIRM_B, null);
    assertEquals(200, secondFirmView.statusCode());
    AssessmentDetail secondFirmDetail =
        OBJECT_MAPPER.readValue(secondFirmView.body(), AssessmentDetail.class);
    assertEquals("status2", secondFirmDetail.getStatus());
    assertEquals(FIRM_B, secondFirmDetail.getProviderId());
    assertNotNull(secondFirmDetail.getCheckpoint());
  }

  private HttpResponse<String> sendHttp(
      final String method, final String path, final String providerId, final String jsonBody)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Caab-User-Login-Id", caabUserLoginId)
            .header("Accept", MediaType.APPLICATION_JSON_VALUE);
    if (providerId != null) {
      builder.header(ProviderContext.HEADER_NAME, providerId);
    }
    if (jsonBody != null) {
      builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
      builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
    } else {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  public static AssessmentDetail buildAssessmentDetail() {
    return new AssessmentDetail()
        .id(26L)
        .name("new_assessment1")
        .status("new_status1")
        .providerId(FIRM_A)
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
    return new AssessmentRelationshipTargetDetail().id(31L).targetEntityId("new_entity1");
  }

  /** Provides a stub JWT decoder for real HTTP integration testing. */
  @TestConfiguration(proxyBeanMethods = false)
  static class SecurityTestConfiguration {
    @Bean
    JwtDecoder jwtDecoder() {
      return StubJwtDecoder.of(
          new StubJwtToken(TOKEN, "integration-test", new String[] {"ALL"}, null, Map.of()));
    }
  }
}
