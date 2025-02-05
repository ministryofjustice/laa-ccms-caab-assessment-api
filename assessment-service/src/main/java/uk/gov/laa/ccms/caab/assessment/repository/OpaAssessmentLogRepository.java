package uk.gov.laa.ccms.caab.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAssessmentLog;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;

/**
 * Repository interface for managing {@link uk.gov.laa.ccms.caab.assessment.entity.OpaAssessmentLog}
 * entities.
 *
 */
@Repository
public interface OpaAssessmentLogRepository extends JpaRepository<OpaAssessmentLog, Long> {

  /**
   * Finds a list of OpaAssessmentLog by the targetId and assessment.
   *
   * @param targetId the ID of the target
   * @param assessment the type of assessment
   * @return a list of OpaAssessmentLog matching the targetId and assessment
   */
  List<OpaAssessmentLog> findByTargetIdAndAssessment(String targetId, String assessment);

}
