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
import uk.gov.laa.ccms.caab.assessment.security.ProviderContext;

/** Service class for handling assessment requests. */
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
      final AssessmentDetail criteria, final List<String> names) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    AssessmentDetail scopedCriteria = scopeCriteriaToCallerProvider(criteria, callerProviderId);
    OpaSession example = assessmentMapper.toOpaSession(scopedCriteria);
    if (example != null) {
      example.setOwnerId(callerProviderId);
    }

    List<OpaSession> opaSessions =
        opaSessionRepository.findAll(buildQuerySpecification(Example.of(example), names));

    return assessmentMapper.toAssessmentDetails(opaSessions);
  }

  /**
   * Get assessment based on the assessment id.
   *
   * @param assessmentId the assessment id
   * @return the assessment
   * @throws ApplicationException if the assessment is not found or owned by another provider
   */
  public AssessmentDetail getAssessment(Long assessmentId) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    OpaSession session =
        opaSessionRepository
            .findById(assessmentId)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        String.format("Assessment with id %s not found", assessmentId),
                        HttpStatus.NOT_FOUND));
    verifySessionOwnership(session, callerProviderId);
    return assessmentMapper.toAssessmentDetail(session);
  }

  /**
   * Creates an assessment and saves it to the repository.
   *
   * @param assessmentDetail the details of the assessment to be created
   * @return the ID of the created assessment
   */
  @Transactional
  public Long createAssessment(final AssessmentDetail assessmentDetail) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    if (assessmentDetail == null
        || assessmentDetail.getProviderId() == null
        || !assessmentDetail.getProviderId().trim().equals(callerProviderId)) {
      throw new ApplicationException("Access denied for provider", HttpStatus.FORBIDDEN);
    }
    assessmentDetail.setId(null);
    assessmentDetail.setProviderId(callerProviderId);

    OpaSession assessment = assessmentMapper.toOpaSession(assessmentDetail);
    assessment.setOwnerId(callerProviderId);
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
  @Transactional
  public void deleteAssessments(final AssessmentDetail criteria, final List<String> names) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    AssessmentDetail scopedCriteria = scopeCriteriaToCallerProvider(criteria, callerProviderId);

    OpaSession example = assessmentMapper.toOpaSession(scopedCriteria);
    if (example != null) {
      example.setOwnerId(callerProviderId);
    }

    List<OpaSession> sessionsToDelete =
        opaSessionRepository.findAll(buildQuerySpecification(Example.of(example), names));

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

      String logAssessmentType =
          OpaAssessmentLogMap.findLogAssessmentTypeBySessionAssessmentType(sessionAssessmentType);

      if (logAssessmentType != null) {
        List<OpaAssessmentLog> assessmentLogs =
            opaAssessmentLogRepository.findByTargetIdAndAssessment(
                session.getTargetId(), logAssessmentType);
        for (OpaAssessmentLog assessmentLog : assessmentLogs) {
          if (session.getOwnerId() == null
              || assessmentLog.getOwnerId() == null
              || session.getOwnerId().trim().equals(assessmentLog.getOwnerId().trim())) {
            logsToDelete.add(assessmentLog);
          }
        }
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
   * @throws ApplicationException if the checkpoint with the specified ID does not exist or is owned
   *     by another provider.
   */
  public void deleteCheckpoint(final Long assessmentId) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    OpaSession assessment =
        opaSessionRepository
            .findById(assessmentId)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        String.format("Assessment checkpoint with id: %s not found", assessmentId),
                        HttpStatus.NOT_FOUND));
    verifySessionOwnership(assessment, callerProviderId);
    if (assessment.getCheckpoint() != null) {
      assessment.getCheckpoint().setOpaSession(null);
      assessment.setCheckpoint(null);
    }
    opaSessionRepository.save(assessment);
  }

  /**
   * Updates an assessment in the database.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param assessment The new details to be applied to the assessment.
   * @throws ApplicationException if the assessment does not exist or is owned by another provider.
   */
  @Transactional
  public void updateAssessment(final Long assessmentId, final AssessmentDetail assessment) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    OpaSession existingSession =
        opaSessionRepository
            .findById(assessmentId)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        String.format("Assessment with id %s not found", assessmentId),
                        HttpStatus.NOT_FOUND));
    verifySessionOwnership(existingSession, callerProviderId);
    if (assessment == null) {
      throw new ApplicationException("Assessment request body is required", HttpStatus.BAD_REQUEST);
    }
    if (assessment.getProviderId() != null
        && !assessment.getProviderId().trim().equals(callerProviderId)) {
      throw new ApplicationException("Access denied for provider", HttpStatus.FORBIDDEN);
    }
    if (assessment.getId() != null && !assessment.getId().equals(assessmentId)) {
      throw new ApplicationException(
          "Assessment id in body does not match path parameter", HttpStatus.BAD_REQUEST);
    }
    assessment.setId(assessmentId);
    assessment.setProviderId(callerProviderId);
    OpaSession session = assessmentMapper.toOpaSession(assessment);
    session.setId(assessmentId);
    session.setOwnerId(callerProviderId);
    opaSessionRepository.save(session);
  }

  /**
   * Patches an assessment's details in the database.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param patch The new details to be applied to the assessment.
   * @throws ApplicationException if the assessment does not exist or is owned by another provider.
   */
  @Transactional
  public void patchAssessment(final Long assessmentId, final PatchAssessmentDetail patch) {
    String callerProviderId = ProviderContext.getRequiredProviderId();
    OpaSession opaSession =
        opaSessionRepository
            .findById(assessmentId)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        String.format("Assessment with id %s not found", assessmentId),
                        HttpStatus.NOT_FOUND));
    verifySessionOwnership(opaSession, callerProviderId);
    if (patch != null
        && patch.getProviderId() != null
        && !patch.getProviderId().trim().equals(callerProviderId)) {
      throw new ApplicationException("Access denied for provider", HttpStatus.FORBIDDEN);
    }
    if (patch != null) {
      assessmentMapper.mapIntoOpaSession(opaSession, patch);
    }
    opaSession.setOwnerId(callerProviderId);
    opaSessionRepository.save(opaSession);
  }

  private static AssessmentDetail scopeCriteriaToCallerProvider(
      final AssessmentDetail criteria, final String callerProviderId) {
    AssessmentDetail effectiveCriteria = criteria != null ? criteria : new AssessmentDetail();
    if (effectiveCriteria.getProviderId() != null
        && !effectiveCriteria.getProviderId().trim().equals(callerProviderId)) {
      throw new ApplicationException("Access denied for provider", HttpStatus.FORBIDDEN);
    }
    effectiveCriteria.setProviderId(callerProviderId);
    return effectiveCriteria;
  }

  private static void verifySessionOwnership(
      final OpaSession session, final String callerProviderId) {
    if (session.getOwnerId() == null || !session.getOwnerId().trim().equals(callerProviderId)) {
      throw new ApplicationException("Access denied for assessment", HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Constructs a query specification based on a provided example and a list of names.
   *
   * @param assessment the example of OpaSession to filter query results.
   * @param names a list of names to include in the query; may be null or empty.
   * @return the Specification object that constructs the predicate for querying.
   */
  protected Specification<OpaSession> buildQuerySpecification(
      final Example<OpaSession> assessment, final List<String> names) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (names != null && !names.isEmpty()) {
        predicates.add(root.get("assessment").in(names));
      }

      Predicate examplePredicate =
          QueryByExamplePredicateBuilder.getPredicate(root, builder, assessment);

      if (examplePredicate != null) {
        predicates.add(examplePredicate);
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
