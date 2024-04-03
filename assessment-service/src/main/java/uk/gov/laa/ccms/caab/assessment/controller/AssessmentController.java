package uk.gov.laa.ccms.caab.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.laa.ccms.caab.assessment.api.AssessmentApi;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

/**
 * Controller handling assessment requests.
 */
@Controller
@RequiredArgsConstructor
public class AssessmentController implements AssessmentApi {

  private final AssessmentService assessmentService;

  @Override
  public ResponseEntity<AssessmentDetail> getAssessment(
      final Long assessmentId) {

    return ResponseEntity.ok(assessmentService.getAssessment(assessmentId));
  }

  @Override
  public ResponseEntity<AssessmentDetails> getAssessments(
      final String name,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {

    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .name(name)
        .status(status);

    return ResponseEntity.ok(assessmentService.getAssessments(criteria));
  }
}
