package uk.gov.laa.ccms.caab.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;

/**
 * Repository interface for managing {@link uk.gov.laa.ccms.caab.assessment.entity.OpaSession}
 * entities.
 *
 * <p>This interface provides CRUD (Create, Read, Update, Delete) operations
 * for the {@link uk.gov.laa.ccms.caab.assessment.entity.OpaSession} entity, leveraging the power of
 * Spring Data JPA.
 */
@Repository
public interface OpaSessionRepository extends JpaRepository<OpaSession, Long>,
    JpaSpecificationExecutor<OpaSession> {

}
