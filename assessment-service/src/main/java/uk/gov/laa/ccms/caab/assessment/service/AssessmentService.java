package uk.gov.laa.ccms.caab.assessment.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
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
  public AssessmentDetails getAssessments(
      final AssessmentDetail criteria,
      final List<String> names) {
    OpaSession example = assessmentMapper.toOpaSession(criteria);

    List<OpaSession> opaSessions =
        opaSessionRepository.findAll(buildQuerySpecification(Example.of(example), names));

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

  /**
   * Deletes assessments from the repository based on specified criteria and a list of names.
   *
   * @param criteria the details of the assessment to use as deletion criteria.
   * @param names a list of assessment types to further filter the assessments to be deleted.
   */
  public void deleteAssessments(
      final AssessmentDetail criteria,
      final List<String> names) {

    OpaSession example = assessmentMapper.toOpaSession(criteria);

    opaSessionRepository.deleteAll(
        opaSessionRepository.findAll(buildQuerySpecification(Example.of(example), names)));
  }

  /**
   * Deletes a checkpoint from an assessment.
   *
   * @param assessmentId the ID of the assessment to delete the checkpoint from
   * @throws ApplicationException if the checkpoint with the specified ID
   *         does not exist.
   */
  public void deleteCheckpoint(
      final Long assessmentId) {

    opaSessionRepository.findById(assessmentId)
        .ifPresentOrElse(
            assessment -> {
              assessment.getCheckpoint().setOpaSession(null);
              assessment.setCheckpoint(null);
              opaSessionRepository.save(assessment);
            }, () -> {
              throw new ApplicationException(
                  String.format("Assessment checkpoint with id: %s not found", assessmentId),
                  HttpStatus.NOT_FOUND);
            }
        );
  }



  /**
   * Updates an assessment's details in the database.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param patch The new details to be applied to the assessment.
   * @throws ApplicationException if the assessment with the specified ID
   *         does not exist.
   */
  @Transactional
  public void updateAssessment(
      final Long assessmentId,
      final PatchAssessmentDetail patch) {
    OpaSession opaSession = opaSessionRepository.findById(assessmentId)
        .orElseThrow(() -> new ApplicationException(
            String.format("Assessment with id %s not found", assessmentId),
            HttpStatus.NOT_FOUND));

    assessmentMapper.mapIntoOpaSession(opaSession, patch);
    opaSessionRepository.save(opaSession);
  }

  /**
   * Constructs a query specification based on a provided example and a list of names.
   *
   * @param assessment the example of OpaSession to filter query results.
   * @param names a list of names to include in the query; may be null or empty.
   * @return the Specification object that constructs the predicate for querying.
   */
  protected Specification<OpaSession> buildQuerySpecification(
      final Example<OpaSession> assessment,
      final List<String> names) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (names != null && !names.isEmpty()) {
        predicates.add(root.get("assessment").in(names));
      }

      Predicate examplePredicate = QueryByExamplePredicateBuilder
          .getPredicate(root, builder, assessment);

      if (examplePredicate != null) {
        predicates.add(examplePredicate);
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

}
