package uk.gov.laa.ccms.caab.assessment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentControllerTest {

  @MockBean
  private AssessmentService assessmentService;

  @Autowired
  private MockMvc mockMvc;

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
  public void getAssessment_throwsNotFound() throws Exception {
    Long assessmentId = 1L;
    String errorMessage = String.format("Assessment with id %s not found", assessmentId);

    when(assessmentService.getAssessment(assessmentId))
        .thenThrow(
            new ApplicationException(
                errorMessage,
                HttpStatus.NOT_FOUND));

    this.mockMvc.perform(get("/assessments/{assessment-id}", assessmentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error_message").value(errorMessage))
        .andExpect(jsonPath("$.http_status").value(HttpStatus.NOT_FOUND.value()));

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