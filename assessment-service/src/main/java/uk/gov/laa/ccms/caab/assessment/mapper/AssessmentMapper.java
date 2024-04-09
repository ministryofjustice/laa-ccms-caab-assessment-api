package uk.gov.laa.ccms.caab.assessment.mapper;

import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAttribute;
import uk.gov.laa.ccms.caab.assessment.entity.OpaEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaListEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.BaseAssessmentDetail;

/**
 * Mapper for mapping between assessment entities and models.
 */
@Mapper(componentModel = "spring",
    uses = CommonMapper.class)
public interface AssessmentMapper {

  @Mapping(target = "assessment", source = "name")
  @Mapping(target = "ownerId", source = "providerId")
  @Mapping(target = "targetId", source = "caseReferenceNumber")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opaListEntities", ignore = true)
  OpaSession toOpaSession(AssessmentDetail assessmentDetail);

  @InheritInverseConfiguration(name = "toOpaSession")
  @Mapping(target = "auditDetail", source = "auditTrail")
  @Mapping(target = "entityTypes", source = "opaListEntities")
  AssessmentDetail toAssessmentDetail(OpaSession opaSession);

  @Mapping(target = "entities", source = "opaEntities")
  @Mapping(target = "name", source = "entityType")
  AssessmentEntityTypeDetail toAssessmentEntityTypeDetail(OpaListEntity opaListEntity);

  @Mapping(target = "attributes", source = "opaAttributes")
  @Mapping(target = "name", source = "entityId")
  AssessmentEntityDetail toAssessmentEntityDetail(OpaEntity opaEntity);

  @Mapping(target = "name", source = "attributeId")
  @Mapping(target = "type", source = "attributeType")
  AssessmentAttributeDetail toAssessmentAttributeDetail(OpaAttribute opaAttribute);

  @InheritConfiguration(name = "toOpaSession")
  @Mapping(target = "opaListEntities", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy =  NullValuePropertyMappingStrategy.IGNORE)
  void mapIntoOpaSession(
      @MappingTarget OpaSession opaSession,
      AssessmentDetail assessmentDetail);

  /**
   * Maps a list of {@link OpaSession} entities to an {@link AssessmentDetails} model.
   *
   * @param opaSessions the list of {@link OpaSession} entities to map
   * @return the mapped {@link AssessmentDetails} model
   */
  default AssessmentDetails toAssessmentDetails(List<OpaSession> opaSessions) {
    AssessmentDetails assessmentDetails = new AssessmentDetails();
    if (opaSessions != null) {
      for (OpaSession opaSession : opaSessions) {
        assessmentDetails.addContentItem(toAssessmentDetail(opaSession));
      }
    }
    return assessmentDetails;
  }




}
