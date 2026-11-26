package uk.gov.laa.ccms.caab.assessment.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.laa.ccms.caab.assessment.entity.OpaCheckpoint;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;

/**
 * Repository interface for managing {@link uk.gov.laa.ccms.caab.assessment.entity.OpaSession}
 * entities.
 *
 * <p>This interface provides CRUD (Create, Read, Update, Delete) operations for the {@link
 * uk.gov.laa.ccms.caab.assessment.entity.OpaSession} entity, leveraging the power of Spring Data
 * JPA.
 */
@Repository
public interface OpaSessionRepository
    extends JpaRepository<OpaSession, Long>, JpaSpecificationExecutor<OpaSession> {

  /**
   * Loads an assessment's checkpoint and locks its row for the rest of the transaction, so a
   * checkpoint written meanwhile waits rather than being deleted unseen.
   *
   * @param assessmentId the ID of the assessment the checkpoint belongs to
   * @return the locked checkpoint, or empty when the assessment has none
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from OpaCheckpoint c where c.id = :assessmentId")
  Optional<OpaCheckpoint> findCheckpointForUpdate(@Param("assessmentId") Long assessmentId);
}
