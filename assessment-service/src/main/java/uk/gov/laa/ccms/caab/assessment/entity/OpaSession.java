package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents an Oracle Intelligence Advisor session.
 */
@Entity
@Table(name = "XXCCMS_OPA_SESSION")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPASESSION_GENERATED_ID_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class OpaSession {

  /**
    * The unique identifier of the session.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPASESSION_GENERATED_ID_S")
  private Long id;

  /**
   * Owner of this session, e.g. the user that performs the assessment.
   */
  @Column(
      name = "OWNER_ID",
      nullable = false)
  private String ownerId;

  /**
   * id of the subject in the assessment.
   */
  @Column(
      name = "TARGET_ID",
      nullable = false)
  private String targetId;

  /**
   * Name for an assessment with specific goal(s). The gathered information can be used for new
   * inquiries, however it is advisable not to switch to other rulebases with the same data-set.
   */
  @Column(
      name = "ASSESSMENT",
      nullable = false)
  private String assessment;

  /**
   * Status of this session.
   */
  @Column(
      name = "STATUS",
      length = 20)
  private String status;

  /**
   * The Oracle Intelligence Advisor rule base to be used for this session.
   */
  @OneToMany(
      mappedBy = "opaSession",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER
  )
  private List<OpaListEntity> opaListEntities;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "opaSession")
  private OpaCheckpoint checkpoint;

  /**
   * audit trail info.
   */
  @Embedded
  private AuditTrail auditTrail = new AuditTrail();

}
