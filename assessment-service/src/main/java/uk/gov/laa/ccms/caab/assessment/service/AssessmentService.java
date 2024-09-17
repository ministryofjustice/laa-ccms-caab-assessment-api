package uk.gov.laa.ccms.caab.assessment.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.assessment.constants.OpaAssessmentLogMap;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAssessmentLog;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.repository.OpaAssessmentLogRepository;
import uk.gov.laa.ccms.caab.assessment.repository.OpaSessionRepository;

/**
 * Service class for handling assessment requests.
 */
@Service
@RequiredArgsConstructor
public class AssessmentService {

  private final OpaSessionRepository opaSessionRepository;
  private final OpaAssessmentLogRepository opaAssessmentLogRepository;

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
   * Creates an assessment and saves it to the repository.
   *
   * @param assessmentDetail the details of the assessment to be created
   * @return the ID of the created assessment
   */
  @Transactional
  public Long createAssessment(
      final AssessmentDetail assessmentDetail) {

    OpaSession assessment = assessmentMapper.toOpaSession(assessmentDetail);
    opaSessionRepository.save(assessment);

    return assessment.getId();
  }

  /**
   * Deletes OpaSession entities and their associated logs from the repository based on the
   * specified assessment criteria and a list of assessment types.
   *
   * @param criteria the assessment details used as the deletion criteria.
   * @param names a list of assessment types used to further filter which assessments to delete.
   */
  public void deleteAssessments(
      final AssessmentDetail criteria,
      final List<String> names) {

    OpaSession example = assessmentMapper.toOpaSession(criteria);

    List<OpaSession> sessionsToDelete = opaSessionRepository.findAll(
        buildQuerySpecification(Example.of(example), names));

    Set<OpaAssessmentLog> logsToDelete = collectLogsToDelete(sessionsToDelete);

    deleteLogs(logsToDelete);
    if (!sessionsToDelete.isEmpty()) {
      opaSessionRepository.deleteAll(sessionsToDelete);
    }
  }

  /**
   * Collects assessment logs that are associated with the sessions to be deleted.
   *
   * @param sessionsToDelete the list of sessions to delete
   * @return a set of assessment logs to delete
   */
  protected Set<OpaAssessmentLog> collectLogsToDelete(List<OpaSession> sessionsToDelete) {
    Set<OpaAssessmentLog> logsToDelete = new HashSet<>();

    for (OpaSession session : sessionsToDelete) {
      String sessionAssessmentType = session.getAssessment();

      String logAssessmentType = OpaAssessmentLogMap
          .findLogAssessmentTypeBySessionAssessmentType(sessionAssessmentType);

      if (logAssessmentType != null) {
        List<OpaAssessmentLog> assessmentLogs = opaAssessmentLogRepository
            .findByTargetIdAndAssessment(session.getTargetId(), logAssessmentType);
        logsToDelete.addAll(assessmentLogs);
      }
    }

    return logsToDelete;
  }

  /**
   * Deletes the provided assessment logs from the repository.
   *
   * @param logsToDelete the set of assessment logs to delete
   */
  protected void deleteLogs(Set<OpaAssessmentLog> logsToDelete) {
    if (!logsToDelete.isEmpty()) {
      opaAssessmentLogRepository.deleteAll(logsToDelete);
    }
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
   * Updates an assessment in the database.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param assessment The new details to be applied to the assessment.
   * @throws ApplicationException if the assessment with the specified ID
   *         does not exist.
   */
  @Transactional
  public void updateAssessment(
      final Long assessmentId,
      final AssessmentDetail assessment) {

    if (opaSessionRepository.existsById(assessmentId)) {
      OpaSession session = assessmentMapper.toOpaSession(assessment);
      opaSessionRepository.save(session);
    } else {
      throw new ApplicationException(
          String.format("Assessment with id %s not found", assessmentId), HttpStatus.NOT_FOUND);
    }
  }


  /**
   * Patches an assessment's details in the database.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param patch The new details to be applied to the assessment.
   * @throws ApplicationException if the assessment with the specified ID
   *         does not exist.
   */
  @Transactional
  public void patchAssessment(
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
