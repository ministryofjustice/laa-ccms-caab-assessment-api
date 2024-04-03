package uk.gov.laa.ccms.caab.assessment.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.repository.OpaSessionRepository;

/**
 * Service class for handling assessment requests.
 */
@Service
@RequiredArgsConstructor
public class AssessmentService {

  private final OpaSessionRepository opaSessionRepository;

  private final AssessmentMapper assessmentMapper;

  /**
   * Get assessments based on the criteria.
   *
   * @param criteria the criteria to filter the assessments
   * @return the list of assessments
   */
  public AssessmentDetails getAssessments(AssessmentDetail criteria) {
    OpaSession example = assessmentMapper.toOpaSession(criteria);
    List<OpaSession> opaSessions = opaSessionRepository.findAll(Example.of(example));

    return assessmentMapper.toAssessmentDetails(opaSessions);
  }

  /**
   * Get assessment based on the assessment id.
   *
   * @param assessmentId the assessment id
   * @return the assessment
   * @throws ApplicationException if the assessment is not found
   */
  public AssessmentDetail getAssessment(Long assessmentId) {
    return opaSessionRepository.findById(assessmentId)
        .map(assessmentMapper::toAssessmentDetail)
        .orElseThrow(() -> new ApplicationException(
            String.format("Assessment with id %s not found", assessmentId),
            HttpStatus.NOT_FOUND));
  }



}
