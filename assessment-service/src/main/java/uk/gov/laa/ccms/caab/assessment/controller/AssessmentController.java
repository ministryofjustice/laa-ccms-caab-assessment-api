package uk.gov.laa.ccms.caab.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.laa.ccms.caab.assessment.api.AssessmentsApi;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.service.AssessmentService;

/**
 * Controller handling assessment requests.
 */
@RestController
@RequiredArgsConstructor
public class AssessmentController implements AssessmentsApi {

  private final AssessmentService assessmentService;

  /**
   * Retrieves a single assessment by its ID.
   *
   * @param assessmentId The ID of the assessment to retrieve.
   * @return ResponseEntity containing the requested AssessmentDetail.
   */
  @Override
  public ResponseEntity<AssessmentDetail> getAssessment(
      final Long assessmentId) {

    return ResponseEntity.ok(assessmentService.getAssessment(assessmentId));
  }

  /**
   * Retrieves a list of assessments matching the given criteria.
   *
   * @param name The name to search for.
   * @param providerId The provider ID to search for.
   * @param caseReferenceNumber The case reference number to search for.
   * @param status The status to search for.
   * @return ResponseEntity containing the list of matching AssessmentDetails.
   */
  @Override
  public ResponseEntity<AssessmentDetails> getAssessments(
      final List<String> name,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {


    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .status(status);

    return ResponseEntity.ok(assessmentService.getAssessments(criteria, name));
  }

  /**
   * Updates an assessment with the provided details.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param caabUserLoginId The CAAB user login ID performing the update.
   * @param patch The details to update the assessment with.
   * @return ResponseEntity with the status of the update operation.
   */
  @Override
  public ResponseEntity<AssessmentDetail> updateAssessment(
      final Long assessmentId,
      final String caabUserLoginId,
      final PatchAssessmentDetail patch) {

    assessmentService.updateAssessment(assessmentId, patch);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
