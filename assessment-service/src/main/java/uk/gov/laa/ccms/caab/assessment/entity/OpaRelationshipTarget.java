package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an Oracle Intelligence Advisor relationship target.
 */
@Entity
@Table(name = "XXCCMS_OPA_RELSHIPTARGET", schema = "XXCCMS_PUI")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPA_RELATIONSHIP_TARGET_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S",
    schema = "XXCCMS_PUI")
@Getter
@Setter
public class OpaRelationshipTarget {

  /**
   * The unique identifier of the relationship target.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPA_RELATIONSHIP_TARGET_S")
  private Long id;

  /**
   * The target entity identifier of the relationship.
   */
  @Column(
      name = "TARGET_ENTITY_ID",
      nullable = false)
  private String targetEntityId;

}
