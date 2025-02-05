package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an Oracle Intelligence Advisor Checkpoint.
 */
@Entity
@Table(name = "XXCCMS_OPA_CHECKPOINT", schema = "XXCCMS_PUI")
@Getter
@Setter
public class OpaCheckpoint {

  @Id
  @Column(name = "RESUME_ID")
  private Long id;

  @Column(name = "USER_NAME")
  private String username;

  @Column(name = "INTERVIEW_DATA")
  @Lob
  private byte[] interviewData;

  @OneToOne(cascade = {
      CascadeType.PERSIST,
      CascadeType.MERGE
  })
  @MapsId
  @JoinColumn(name = "RESUME_ID")
  private OpaSession opaSession;

}
