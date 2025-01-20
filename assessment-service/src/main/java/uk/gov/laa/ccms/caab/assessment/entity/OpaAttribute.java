package uk.gov.laa.ccms.caab.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

/**
 * Represents an attribute of an Oracle Intelligence Advisor entity.
 */
@Entity
@Table(name = "XXCCMS_OPA_ATTRIBUTE", schema = "XXCCMS_PUI")
@SequenceGenerator(
    allocationSize = 1,
    name = "XXCCMS_OPA_ATTRIBUTE_S",
    sequenceName = "XXCCMS_OPA_GENERATED_ID_S",
    schema = "XXCCMS_PUI")
@Getter
@Setter
public class OpaAttribute {

  /**
   * The unique identifier of the attribute.
   */
  @Id
  @GeneratedValue(generator = "XXCCMS_OPA_ATTRIBUTE_S")
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
   * The identifier of the attribute.
   */
  @Column(
      name = "ATTRIBUTE_ID",
      nullable = false,
      length = 100)
  private String attributeId;

  /**
   * The type of the attribute.
   */
  @Column(
      name = "ATTRIBUTE_TYPE",
      nullable = false)
  private String attributeType;

  /**
   * The value of the attribute.
   */
  @Column(name = "ATTRIBUTE_VALUE")
  @Lob
  private String value;

  /**
   * The type of inferencing used for the attribute.
   */
  @Column(name = "INFERENCING_TYPE")
  private String inferencingType;

  /**
   * Indicates if the attribute is prepopulated.
   */
  @Column(name = "PREPOPULATED", nullable = false)
  @Convert(converter = NumericBooleanConverter.class)
  private Boolean prepopulated;

  /**
   * Indicates if the attribute was asked in the session.
   */
  @Column(name = "ASKED")
  @Convert(converter = NumericBooleanConverter.class)
  private Boolean asked;


}
