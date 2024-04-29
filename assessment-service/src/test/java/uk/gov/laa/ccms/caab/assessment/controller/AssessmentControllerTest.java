package uk.gov.laa.ccms.caab.assessment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.laa.ccms.caab.assessment.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

@WebMvcTest(AssessmentController.class)
@Import(GlobalExceptionHandler.class)
class AssessmentControllerTest {

  @MockBean
  private AssessmentService assessmentService;

  @Autowired
  private MockMvc mockMvc;

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

}