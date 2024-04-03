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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;
import org.hibernate.type.NumericBooleanConverter;

/**
 * Represents an Oracle Intelligence Advisor entity.
 */
@Entity
@Table(name = "XXCCMS_OPA_ENTITY")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPA_ENTITY_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S")
@Data
public class OpaEntity {

  /**
   * The unique identifier of the entity.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPA_ENTITY_S")
  private Long id;

  /**
   * The associated Oracle Intelligence Advisor list entity.
   */
  @ManyToOne
  @JoinColumn(
      name = "FK_OPA_LIST_ENTITY",
      nullable = false,
      referencedColumnName = "ID")
  private OpaListEntity opaListEntity;

  /**
   * The associated Oracle Intelligence Advisor session.
   */
  @ManyToOne
  @JoinColumn(
      name = "FK_OPA_SESSION",
      nullable = false,
      referencedColumnName = "ID")
  private OpaSession opaSession;

  /**
   * The identifier of the entity.
   */
  @Column(name = "ENTITY_ID")
  private String entityId;

  /**
   * The attributes of the entity.
   */
  @OneToMany(
      mappedBy = "opaEntity",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<OpaAttribute> opaAttributes;

  /**
   * The relationships of the entity.
   */
  @OneToMany(
      mappedBy = "opaEntity",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<OpaRelationship> relations;

  /**
   * Indicates if the entity is prepopulated.
   */
  @Column(name = "PREPOPULATED")
  @Convert(converter = NumericBooleanConverter.class)
  private Boolean prepopulated;


}
