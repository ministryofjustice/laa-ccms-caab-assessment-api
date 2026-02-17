package uk.gov.laa.ccms.caab.assessment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

  @Mock
  private AssessmentService assessmentService;

  @InjectMocks
  private AssessmentController applicationController;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(applicationController)
            .build();
  }

  @Test
  public void createAssessment_createsAssessmentSuccessfully() throws Exception {
    Long assessmentId = 1L;
    AssessmentDetail assessmentDetail = new AssessmentDetail();

    when(assessmentService.createAssessment(assessmentDetail)).thenReturn(assessmentId);

    this.mockMvc.perform(post("/assessments")
            .header("caab-User-Login-Id", "TestUser")
            .content(new ObjectMapper().writeValueAsString(assessmentDetail))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/assessments/" + assessmentId));

    verify(assessmentService).createAssessment(assessmentDetail);
  }


  @Test
  public void getAssessment() throws Exception {
    Long assessmentId = 1L;

    AssessmentDetail assessment = new AssessmentDetail();
    when(assessmentService.getAssessment(assessmentId)).thenReturn(assessment);

    this.mockMvc.perform(get("/assessments/{assessment-id}", assessmentId))
        .andExpect(status().isOk());

    verify(assessmentService).getAssessment(assessmentId);
  }

  @Test
  public void getAssessment_throwsNotFound() {
    Long assessmentId = 1L;
    String errorMessage = String.format("Assessment with id %s not found", assessmentId);

    when(assessmentService.getAssessment(assessmentId))
        .thenThrow(
            new ApplicationException(
                errorMessage,
                HttpStatus.NOT_FOUND));

    ServletException ex = assertThrows(ServletException.class, () ->
            this.mockMvc.perform(get("/assessments/{assessment-id}", assessmentId)),
            "Expected ServletException to be thrown, but wasn't.");

    assertTrue(ex.getMessage().contains(errorMessage));
    assertInstanceOf(ApplicationException.class, ex.getRootCause());

    ApplicationException appEx = (ApplicationException) ex.getRootCause();
    assertEquals(HttpStatus.NOT_FOUND, appEx.getHttpStatus());
    assertEquals(errorMessage, appEx.getErrorMessage());

    verify(assessmentService).getAssessment(assessmentId);
  }

  @Test
  public void getAssessments() throws Exception {
    String providerId = "providerId";
    String caseReferenceNumber = "caseReferenceNumber";
    String name = "name";
    String status = "status";

    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .status(status);

    List<String> names = new ArrayList<>(List.of(name));

    AssessmentDetail assessment = new AssessmentDetail();
    when(assessmentService.getAssessments(criteria, names))
        .thenReturn(new AssessmentDetails().addContentItem(assessment));

    this.mockMvc.perform(get("/assessments")
        .param("provider-id", providerId)
        .param("case-reference-number", caseReferenceNumber)
        .param("name", name)
        .param("status", status))
        .andExpect(status().isOk());

    verify(assessmentService).getAssessments(criteria, names);
  }

  @Test
  public void deleteAssessments() throws Exception {
    String providerId = "providerId";
    String caseReferenceNumber = "caseReferenceNumber";
    String status = "status";
    List<String> names = new ArrayList<>(List.of("name"));

    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .status(status);

    this.mockMvc.perform(delete("/assessments")
            .header("caab-User-Login-Id", "TestUser")
            .param("provider-id", providerId)
            .param("case-reference-number", caseReferenceNumber)
            .param("name", String.join(",", names))
            .param("status", status))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteAssessments(criteria, names);
  }

  @Test
  public void deleteAssessments_returnsNoContentWhenNoMatchingAssessments() throws Exception {
    String providerId = "nonExistentProviderId";
    String caseReferenceNumber = "nonExistentCaseReferenceNumber";
    String status = "nonExistentStatus";
    List<String> names = new ArrayList<>(List.of("nonExistentName"));

    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .status(status);

    this.mockMvc.perform(delete("/assessments")
            .header("Caab-User-Login-Id", "TestUser")
            .param("provider-id", providerId)
            .param("case-reference-number", caseReferenceNumber)
            .param("name", String.join(",", names))
            .param("status", status))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteAssessments(criteria, names);
  }

  @Test
  public void deleteAssessmentCheckpoint_returnsNoContent_whenCheckpointExists() throws Exception {
    Long assessmentId = 1L;

    this.mockMvc.perform(delete("/assessments/{assessment-id}/checkpoint", assessmentId)
            .header("caab-User-Login-Id", "TestUser"))
        .andExpect(status().isNoContent());

    verify(assessmentService).deleteCheckpoint(assessmentId);
  }

  @Test
  public void patchAssessment_returnsNoContent_whenPatchIsSuccessful() throws Exception {
    Long assessmentId = 1L;
    PatchAssessmentDetail patch = new PatchAssessmentDetail();

    this.mockMvc.perform(patch("/assessments/{assessment-id}", assessmentId)
            .header("caab-User-Login-Id", "TestUser")
            .content(new ObjectMapper().writeValueAsString(patch))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(assessmentService).patchAssessment(assessmentId, patch);
  }

  @Test
  public void updateAssessment_returnsNoContent_whenUpdateIsSuccessful() throws Exception {
    Long assessmentId = 1L;
    AssessmentDetail assessment = new AssessmentDetail();

    this.mockMvc.perform(put("/assessments/{assessment-id}", assessmentId)
            .header("caab-User-Login-Id", "TestUser")
            .content(new ObjectMapper().writeValueAsString(assessment))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(assessmentService).updateAssessment(assessmentId, assessment);
  }

}