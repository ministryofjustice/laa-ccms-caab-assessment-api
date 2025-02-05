package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the OPA Assessment Log.
 */
@Entity
@Table(name = "XXCCMS_OPA_ASSESSMENT_LOG", schema = "XXCCMS_PUI")
@Getter
@Setter
public class OpaAssessmentLog {

  @Id
  @Column(name = "ID")
  private Long id;

  @Column(name = "TARGET_ID")
  private String targetId;

  @Column(name = "ASSESSMENT")
  private String assessment;

  @Column(name = "OWNER_ID")
  private String ownerId;

  @Column(name = "ACTION")
  private String action;

  @Column(name = "TTL")
  private String ttl;

  @Column(name = "CREATED")
  private LocalDateTime created;

  @Column(name = "CREATED_BY")
  private String createdBy;

}

