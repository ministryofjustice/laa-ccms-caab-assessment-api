package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

/**
 * Represents an Oracle Intelligence Advisor relationship.
 */
@Entity
@Table(name = "XXCCMS_OPA_RELATIONSHIP", schema = "XXCCMS_PUI")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPA_RELATIONSHIP_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S",
    schema = "XXCCMS_PUI")
@Getter
@Setter
public class OpaRelationship {

  /**
   * The unique identifier of the relationship.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPA_RELATIONSHIP_S")
  private Long id;

  /**
   * The associated Oracle Intelligence Advisor entity.
   */
  @ManyToOne
  @JoinColumn(
      name = "FK_OPA_ENTITY",
      nullable = false,
      referencedColumnName = "ID")
  private OpaEntity opaEntity;


  /**
   * The associated Oracle Intelligence Advisor relationship targets.
   */
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JoinColumn(
      name = "FK_OPA_RELATIONSHIP",
      referencedColumnName = "ID",
      nullable = false)
  @OrderBy("targetEntityId asc ")
  private Set<OpaRelationshipTarget> relationshipTargets;

  /**
   * The name of the relationship.
   */
  @Column(name = "NAME")
  private String name;

  /**
   * Indicates if the relationship is prepopulated.
   */
  @Column(name = "PREPOPULATED")
  @Convert(converter = NumericBooleanConverter.class)
  private Boolean prepopulated;
}
