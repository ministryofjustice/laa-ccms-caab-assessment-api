package uk.gov.laa.ccms.caab.assessment.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.laa.ccms.caab.assessment.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.security.ProviderContext;
import uk.gov.laa.ccms.caab.assessment.security.ProviderContextFilter;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

  private static final String PROVIDER_ID = "12345";

  @Mock private AssessmentService assessmentService;

  @InjectMocks private AssessmentController applicationController;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc =
        standaloneSetup(applicationController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new ProviderContextFilter())
            .build();
  }

  @AfterEach
  void tearDown() {
    ProviderContext.clear();
  }

  @Test
  public void createAssessment_createsAssessmentSuccessfully() throws Exception {
    Long assessmentId = 1L;
    AssessmentDetail assessmentDetail = new AssessmentDetail().providerId(PROVIDER_ID);

    when(assessmentService.createAssessment(assessmentDetail)).thenReturn(assessmentId);

    this.mockMvc
        .perform(
            post("/assessments")
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID)
                .content(new ObjectMapper().writeValueAsString(assessmentDetail))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/assessments/" + assessmentId));

    verify(assessmentService).createAssessment(assessmentDetail);
  }

  @Test
  public void getAssessment() throws Exception {
    Long assessmentId = 1L;

    AssessmentDetail assessment = new AssessmentDetail().providerId(PROVIDER_ID);
    when(assessmentService.getAssessment(assessmentId)).thenReturn(assessment);

    this.mockMvc
        .perform(
            get("/assessments/{assessment-id}", assessmentId)
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID))
        .andExpect(status().isOk());

    verify(assessmentService).getAssessment(assessmentId);
  }

  @Test
  public void getAssessment_throwsNotFound() throws Exception {
    Long assessmentId = 1L;
    String errorMessage = String.format("Assessment with id %s not found", assessmentId);

    when(assessmentService.getAssessment(assessmentId))
        .thenThrow(new ApplicationException(errorMessage, HttpStatus.NOT_FOUND));

    this.mockMvc
        .perform(
            get("/assessments/{assessment-id}", assessmentId)
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.http_status").value(404))
        .andExpect(jsonPath("$.error_message").value(errorMessage));

    verify(assessmentService).getAssessment(assessmentId);
  }

  @Test
  public void getAssessment_returnsForbiddenWhenOwnedByAnotherFirm() throws Exception {
    Long assessmentId = 1L;
    when(assessmentService.getAssessment(assessmentId))
        .thenThrow(new ApplicationException("Access denied for assessment", HttpStatus.FORBIDDEN));

    this.mockMvc
        .perform(
            get("/assessments/{assessment-id}", assessmentId)
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.http_status").value(403));

    verify(assessmentService).getAssessment(assessmentId);
  }

  @Test
  public void getAssessments_rejectsMissingProviderHeaderWith400() throws Exception {
    this.mockMvc
        .perform(get("/assessments"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.http_status").value(400))
        .andExpect(jsonPath("$.error_message").value("Missing required header: Caab-Provider-Id"));

    verifyNoInteractions(assessmentService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "abc"})
  public void getAssessments_rejectsBlankOrNonNumericProviderHeaderWith400(final String headerValue)
      throws Exception {
    this.mockMvc
        .perform(get("/assessments").header(ProviderContext.HEADER_NAME, headerValue))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.http_status").value(400));

    verifyNoInteractions(assessmentService);
  }

  @Test
  public void getAssessments() throws Exception {
    String caseReferenceNumber = "caseReferenceNumber";
    String name = "name";
    String status = "status";

    AssessmentDetail criteria =
        new AssessmentDetail()
            .providerId(PROVIDER_ID)
            .caseReferenceNumber(caseReferenceNumber)
            .status(status);

    List<String> names = new ArrayList<>(List.of(name));

    AssessmentDetail assessment = new AssessmentDetail();
    when(assessmentService.getAssessments(criteria, names))
        .thenReturn(new AssessmentDetails().addContentItem(assessment));

    this.mockMvc
        .perform(
            get("/assessments")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID)
                .param("provider-id", PROVIDER_ID)
                .param("case-reference-number", caseReferenceNumber)
                .param("name", name)
                .param("status", status))
        .andExpect(status().isOk());

    verify(assessmentService).getAssessments(criteria, names);
  }

  @Test
  public void deleteAssessments() throws Exception {
    String caseReferenceNumber = "caseReferenceNumber";
    String status = "status";
    List<String> names = new ArrayList<>(List.of("name"));

    AssessmentDetail criteria =
        new AssessmentDetail()
            .providerId(PROVIDER_ID)
            .caseReferenceNumber(caseReferenceNumber)
            .status(status);

    this.mockMvc
        .perform(
            delete("/assessments")
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID)
                .param("provider-id", PROVIDER_ID)
                .param("case-reference-number", caseReferenceNumber)
                .param("name", String.join(",", names))
                .param("status", status))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteAssessments(criteria, names);
  }

  @Test
  public void deleteAssessments_returnsNoContentWhenNoMatchingAssessments() throws Exception {
    String nonExistentProviderId = "99999";
    String caseReferenceNumber = "nonExistentCaseReferenceNumber";
    String status = "nonExistentStatus";
    List<String> names = new ArrayList<>(List.of("nonExistentName"));

    AssessmentDetail criteria =
        new AssessmentDetail()
            .providerId(nonExistentProviderId)
            .caseReferenceNumber(caseReferenceNumber)
            .status(status);

    this.mockMvc
        .perform(
            delete("/assessments")
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, nonExistentProviderId)
                .param("provider-id", nonExistentProviderId)
                .param("case-reference-number", caseReferenceNumber)
                .param("name", String.join(",", names))
                .param("status", status))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteAssessments(criteria, names);
  }

  @Test
  public void deleteAssessmentCheckpoint_returnsNoContent_whenCheckpointExists() throws Exception {
    Long assessmentId = 1L;

    this.mockMvc
        .perform(
            delete("/assessments/{assessment-id}/checkpoint", assessmentId)
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteCheckpoint(assessmentId);
  }

  @Test
  public void deleteAssessmentCheckpoint_returnsForbidden_whenOwnedByAnotherFirm()
      throws Exception {
    Long assessmentId = 1L;
    doThrow(new ApplicationException("Access denied for assessment", HttpStatus.FORBIDDEN))
        .when(assessmentService)
        .deleteCheckpoint(assessmentId);

    this.mockMvc
        .perform(
            delete("/assessments/{assessment-id}/checkpoint", assessmentId)
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.http_status").value(403));
  }

  @Test
  public void patchAssessment_returnsNoContent_whenPatchIsSuccessful() throws Exception {
    Long assessmentId = 1L;
    PatchAssessmentDetail patch = new PatchAssessmentDetail();

    this.mockMvc
        .perform(
            patch("/assessments/{assessment-id}", assessmentId)
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID)
                .content(new ObjectMapper().writeValueAsString(patch))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(assessmentService).patchAssessment(assessmentId, patch);
  }

  @Test
  public void updateAssessment_returnsNoContent_whenUpdateIsSuccessful() throws Exception {
    Long assessmentId = 1L;
    AssessmentDetail assessment = new AssessmentDetail().providerId(PROVIDER_ID);

    this.mockMvc
        .perform(
            put("/assessments/{assessment-id}", assessmentId)
                .header("Caab-User-Login-Id", "TestUser")
                .header(ProviderContext.HEADER_NAME, PROVIDER_ID)
                .content(new ObjectMapper().writeValueAsString(assessment))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(assessmentService).updateAssessment(assessmentId, assessment);
  }
}
