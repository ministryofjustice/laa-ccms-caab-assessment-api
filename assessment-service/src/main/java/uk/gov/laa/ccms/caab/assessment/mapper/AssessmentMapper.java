package uk.gov.laa.ccms.caab.assessment.mapper;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAttribute;
import uk.gov.laa.ccms.caab.assessment.entity.OpaCheckpoint;
import uk.gov.laa.ccms.caab.assessment.entity.OpaEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaListEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaRelationship;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentCheckpointDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

/**
 * Mapper for mapping between assessment entities and models.
 */
@Mapper(componentModel = "spring",
    uses = CommonMapper.class)
public interface AssessmentMapper {

  @Mapping(target = "assessment", source = "name")
  @Mapping(target = "ownerId", source = "providerId")
  @Mapping(target = "targetId", source = "caseReferenceNumber")
  @Mapping(target = "opaListEntities", source = "entityTypes")
  @Mapping(target = "auditTrail", ignore = true)
  OpaSession toOpaSession(AssessmentDetail assessmentDetail);

  /**
   * Post-processes the OpaSession after mapping from AssessmentDetail.
   *
   * @param opaSession the mapped OpaSession
   * @param assessmentDetail the source assessment details
   */
  @AfterMapping
  default void afterToOpaSession(
      @MappingTarget OpaSession opaSession,
      AssessmentDetail assessmentDetail) {
    if (opaSession.getCheckpoint() != null) {
      opaSession.getCheckpoint().setId(opaSession.getId());
      opaSession.getCheckpoint().setOpaSession(opaSession);
    }

    if (opaSession.getOpaListEntities() != null) {
      for (OpaListEntity opaListEntity : opaSession.getOpaListEntities()) {
        opaListEntity.setOpaSession(opaSession);

        if (opaListEntity.getOpaEntities() != null) {
          for (OpaEntity opaEntity : opaListEntity.getOpaEntities()) {
            opaEntity.setOpaListEntity(opaListEntity);
            opaEntity.setOpaSession(opaSession);

            if (opaEntity.getRelations() != null) {
              for (OpaRelationship opaRelationship : opaEntity.getRelations()) {
                opaRelationship.setOpaEntity(opaEntity);
              }
            }

            if (opaEntity.getOpaAttributes() != null) {
              for (OpaAttribute opaAttribute : opaEntity.getOpaAttributes()) {
                opaAttribute.setOpaEntity(opaEntity);
              }
            }
          }
        }
      }
    }
  }

  @Mapping(target = "opaEntities", source = "entities")
  @Mapping(target = "entityType", source = "name")
  @Mapping(target = "opaSession", ignore = true)
  OpaListEntity toOpaListEntity(AssessmentEntityTypeDetail assessmentEntityTypeDetail);

  @Mapping(target = "entityId", source = "name")
  @Mapping(target = "opaAttributes", source = "attributes")
  @Mapping(target = "opaListEntity", ignore = true)
  @Mapping(target = "opaSession", ignore = true)
  OpaEntity toOpaEntity(AssessmentEntityDetail assessmentEntityDetail);

  @Mapping(target = "opaEntity", ignore = true)
  OpaRelationship toOpaRelationship(AssessmentRelationshipDetail assessmentRelationshipDetail);

  @Mapping(target = "attributeId", source = "name")
  @Mapping(target = "attributeType", source = "type")
  @Mapping(target = "inferencingType", ignore = true)
  @Mapping(target = "opaEntity", ignore = true)
  OpaAttribute toOpaAttribute(AssessmentAttributeDetail assessmentAttributeDetail);

  @InheritInverseConfiguration(name = "toOpaSession")
  @Mapping(target = "auditDetail", source = "auditTrail")
  @Mapping(target = "entityTypes", source = "opaListEntities")
  @Mapping(target = "checkpoint", source = "checkpoint")
  AssessmentDetail toAssessmentDetail(OpaSession opaSession);

  @InheritInverseConfiguration(name = "toOpaListEntity")
  AssessmentEntityTypeDetail toAssessmentEntityTypeDetail(OpaListEntity opaListEntity);

  @Mapping(target = "attributes", source = "opaAttributes")
  @Mapping(target = "name", source = "entityId")
  AssessmentEntityDetail toAssessmentEntityDetail(OpaEntity opaEntity);

  @Mapping(target = "name", source = "attributeId")
  @Mapping(target = "type", source = "attributeType")
  AssessmentAttributeDetail toAssessmentAttributeDetail(OpaAttribute opaAttribute);

  @Mapping(target = "assessment", source = "name")
  @Mapping(target = "ownerId", source = "providerId")
  @Mapping(target = "targetId", source = "caseReferenceNumber")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opaListEntities", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "checkpoint", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy =  NullValuePropertyMappingStrategy.IGNORE)
  void mapIntoOpaSession(
      @MappingTarget OpaSession opaSession,
      PatchAssessmentDetail patch);

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
