package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

/**
 * Represents an Oracle Intelligence Advisor list entity.
 */
@Entity
@Table(name = "XXCCMS_OPA_LISTENTITY")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPA_LIST_ENTITY_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S")
@Data
public class OpaListEntity {


  /**
   * The unique identifier of the list entity.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPA_LIST_ENTITY_S")
  private Long id;

  /**
   * The associated Oracle Intelligence Advisor session.
   */
  @ManyToOne
  @JoinColumn(
      name = "FK_OPA_SESSION",
      referencedColumnName = "ID")
  private OpaSession opaSession;

  /**
   * The type of the entity.
   */
  @Column(
      name = "ENTITY_TYPE",
      nullable = false)
  private String entityType;

  /**
   * The entities of this list entity.
   */
  @OneToMany(
      mappedBy = "opaListEntity",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<OpaEntity> opaEntities;


}
