package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * Represents an audit trail for various entities within the CCMS system.
 */
@Embeddable
@Getter
@Setter
public class AuditTrail {

  /**
   * created date.
   */
  @Column(name = "CREATED", updatable = false)
  @CreationTimestamp
  private Date created;

  /**
   * modified date.
   */
  @Column(name = "MODIFIED")
  @UpdateTimestamp
  private Date lastSaved;

  /**
   * modified by.
   */
  @LastModifiedBy
  @Column(name = "MODIFIED_BY")
  private String lastSavedBy;

  /**
   * created by.
   */
  @CreatedBy
  @Column(name = "CREATED_BY", updatable = false)
  private String createdBy;
}
