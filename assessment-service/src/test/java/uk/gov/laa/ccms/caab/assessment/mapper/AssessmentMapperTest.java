package uk.gov.laa.ccms.caab.assessment.mapper;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAttribute;
import uk.gov.laa.ccms.caab.assessment.entity.OpaCheckpoint;
import uk.gov.laa.ccms.caab.assessment.entity.OpaEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaListEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaRelationship;
import uk.gov.laa.ccms.caab.assessment.entity.OpaRelationshipTarget;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentCheckpointDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

@ExtendWith(MockitoExtension.class)
class AssessmentMapperTest {

  @InjectMocks
  private AssessmentMapperImpl assessmentMapper = new AssessmentMapperImpl();

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private CommonMapper commonMapper;

  @Test
  void afterToOpaSessionShouldSetCorrectReferences() {
    AssessmentDetail assessmentDetail = new AssessmentDetail();
    OpaSession opaSession = new OpaSession();
    opaSession.setId(1L);
    opaSession.setCheckpoint(new OpaCheckpoint());

    OpaListEntity opaListEntity = new OpaListEntity();
    OpaEntity opaEntity = new OpaEntity();
    OpaRelationship opaRelationship = new OpaRelationship();
    OpaAttribute opaAttribute = new OpaAttribute();

    opaEntity.setRelations(List.of(opaRelationship));
    opaEntity.setOpaAttributes(List.of(opaAttribute));
    opaListEntity.setOpaEntities(List.of(opaEntity));
    opaSession.setOpaListEntities(List.of(opaListEntity));

    assessmentMapper.afterToOpaSession(opaSession, assessmentDetail);

    assertEquals(opaSession.getId(), opaSession.getCheckpoint().getId());
    assertEquals(opaSession, opaSession.getCheckpoint().getOpaSession());
    assertEquals(opaSession, opaListEntity.getOpaSession());
    assertEquals(opaListEntity, opaEntity.getOpaListEntity());
    assertEquals(opaSession, opaEntity.getOpaSession());
    assertEquals(opaEntity, opaRelationship.getOpaEntity());
    assertEquals(opaEntity, opaAttribute.getOpaEntity());
  }

  @Test
  void afterToOpaSessionShouldHandleNullValues() {
    AssessmentDetail assessmentDetail = new AssessmentDetail();
    OpaSession opaSession = new OpaSession();

    assessmentMapper.afterToOpaSession(opaSession, assessmentDetail);

    assertNull(opaSession.getCheckpoint());
    assertNull(opaSession.getOpaListEntities());
  }

  @Test
  void testToAssessmentDetailsWithNonNullSessions() {
    List<OpaSession> sessions = new ArrayList<>();
    OpaSession sessionOne = new OpaSession();
    sessionOne.setId(1L);
    OpaSession sessionTwo = new OpaSession();
    sessionTwo.setId(2L);
    sessions.add(sessionOne);
    sessions.add(sessionTwo);

    AssessmentDetails details = assessmentMapper.toAssessmentDetails(sessions);

    assertNotNull(details);
    assertEquals(2, details.getContent().size());
  }

  @Test
  void testToAssessmentDetailsWithNullSessions() {
    AssessmentDetails details = assessmentMapper.toAssessmentDetails(null);

    assertNotNull(details);
    assertTrue(details.getContent().isEmpty());
  }

  @Test
  void testToOpaSessionWithNonNullDetail() {
    AssessmentDetail detail = new AssessmentDetail()
        .providerId("providerId")
        .caseReferenceNumber("caseRefNumber")
        .name("name")
        .status("status")
        .id(1L);

    OpaSession session = assessmentMapper.toOpaSession(detail);

    assertNotNull(session);
    assertEquals("name", session.getAssessment());
    assertEquals("providerId", session.getOwnerId());
    assertEquals("caseRefNumber", session.getTargetId());
    assertEquals(Long.valueOf(1), session.getId());
    assertEquals("status", session.getStatus());
  }

  @Test
  void testToOpaSessionWithNullDetail() {
    OpaSession session = assessmentMapper.toOpaSession(null);
    assertNull(session);
  }

  @Test
  void testToAssessmentDetailWithNonNullSession() {
    OpaSession session = new OpaSession();
    session.setAssessment("assessmentName");
    session.setOwnerId("ownerId");
    session.setTargetId("targetId");
    session.setId(1L);
    session.setStatus("status");

    AssessmentDetail detail = assessmentMapper.toAssessmentDetail(session);

    assertNotNull(detail);
    assertEquals("assessmentName", detail.getName());
    assertEquals("ownerId", detail.getProviderId());
    assertEquals("targetId", detail.getCaseReferenceNumber());
    assertEquals(1L, detail.getId());
    assertEquals("status", detail.getStatus());
  }

  @Test
  void testToAssessmentDetailWithNullSession() {
    AssessmentDetail detail = assessmentMapper.toAssessmentDetail((OpaSession) null);
    assertNull(detail);
  }

  @Test
  void testToAssessmentEntityTypeDetailWithNonNullEntity() {
    OpaListEntity opaListEntity = new OpaListEntity();
    opaListEntity.setEntityType("entityType");
    opaListEntity.setId(1L);

    AssessmentEntityTypeDetail detail =
        assessmentMapper.toAssessmentEntityTypeDetail(opaListEntity);

    assertNotNull(detail);
    assertEquals("entityType", detail.getName());
    assertEquals(1L, detail.getId());
  }

  @Test
  void testToAssessmentEntityTypeDetailWithNullEntity() {
    AssessmentEntityTypeDetail detail =
        assessmentMapper.toAssessmentEntityTypeDetail(null);
    assertNull(detail);
  }

  @Test
  void testToAssessmentEntityDetailWithNonNullEntity() {
    OpaEntity opaEntity = new OpaEntity();
    opaEntity.setEntityId("entityId");
    opaEntity.setId(1L);
    opaEntity.setPrepopulated(true);

    AssessmentEntityDetail detail = assessmentMapper.toAssessmentEntityDetail(opaEntity);

    assertNotNull(detail);
    assertEquals("entityId", detail.getName());
    assertEquals(1L, detail.getId());
    assertTrue(detail.getPrepopulated());
  }

  @Test
  void testToAssessmentEntityDetailWithNullEntity() {
    AssessmentEntityDetail detail =
        assessmentMapper.toAssessmentEntityDetail(null);
    assertNull(detail);
  }

  @Test
  void testToAssessmentAttributeDetailWithNonNullAttribute() {
    OpaAttribute opaAttribute = new OpaAttribute();
    opaAttribute.setAttributeId("attributeId");
    opaAttribute.setAttributeType("attributeType");
    opaAttribute.setId(1L);
    opaAttribute.setValue("value");

    AssessmentAttributeDetail detail =
        assessmentMapper.toAssessmentAttributeDetail(opaAttribute);

    assertNotNull(detail);
    assertEquals("attributeId", detail.getName());
    assertEquals("attributeType", detail.getType());
    assertEquals(1L, detail.getId());
    assertEquals("value", detail.getValue());
  }

  @Test
  void testToAssessmentAttributeDetailWithNullAttribute() {
    AssessmentAttributeDetail detail =
        assessmentMapper.toAssessmentAttributeDetail(null);
    assertNull(detail);
  }

  @Test
  void testAuditTrailToAuditDetailWithNullAuditTrail() {
    AuditDetail detail = assessmentMapper.auditTrailToAuditDetail(null);
    assertNull(detail);
  }

  @Test
  void testOpaListEntityListToAssessmentEntityTypeDetailListWithNullList() {
    List<AssessmentEntityTypeDetail> details =
        assessmentMapper.opaListEntityListToAssessmentEntityTypeDetailList(null);
    assertNull(details);
  }

  @Test
  void testOpaListEntityListToAssessmentEntityTypeDetailListWithNonNullList() {
    List<OpaListEntity> opaListEntities = Arrays.asList(
        new OpaListEntity(),
        new OpaListEntity());
    List<AssessmentEntityTypeDetail> details =
        assessmentMapper.opaListEntityListToAssessmentEntityTypeDetailList(opaListEntities);

    assertNotNull(details);
    assertEquals(opaListEntities.size(), details.size());
  }

  @Test
  void testOpaEntityListToAssessmentEntityDetailListWithNullList() {
    List<AssessmentEntityDetail> details = assessmentMapper.opaEntityListToAssessmentEntityDetailList(null);
    assertNull(details);
  }

  @Test
  void testOpaEntityListToAssessmentEntityDetailListWithNonNullList() {
    List<OpaEntity> opaEntities = Arrays.asList(new OpaEntity(), new OpaEntity());
    List<AssessmentEntityDetail> details = assessmentMapper.opaEntityListToAssessmentEntityDetailList(opaEntities);

    assertNotNull(details);
    assertEquals(opaEntities.size(), details.size());
  }

  @Test
  void testOpaAttributeListToAssessmentAttributeDetailListWithNullList() {
    List<AssessmentAttributeDetail> details = assessmentMapper.opaAttributeListToAssessmentAttributeDetailList(null);
    assertNull(details);
  }

  @Test
  void testOpaAttributeListToAssessmentAttributeDetailListWithNonNullList() {
    List<OpaAttribute> opaAttributes = Arrays.asList(new OpaAttribute(), new OpaAttribute());
    List<AssessmentAttributeDetail> details = assessmentMapper.opaAttributeListToAssessmentAttributeDetailList(opaAttributes);

    assertNotNull(details);
    assertEquals(opaAttributes.size(), details.size());
  }

  @Test
  void testOpaRelationshipTargetToAssessmentRelationshipTargetDetailWithNullTarget() {
    AssessmentRelationshipTargetDetail detail = assessmentMapper.opaRelationshipTargetToAssessmentRelationshipTargetDetail(null);
    assertNull(detail);
  }

  @Test
  void testOpaRelationshipTargetSetToAssessmentRelationshipTargetDetailListWithNullSet() {
    List<AssessmentRelationshipTargetDetail> details = assessmentMapper.opaRelationshipTargetSetToAssessmentRelationshipTargetDetailList(null);
    assertNull(details);
  }

  @Test
  void testOpaRelationshipTargetSetToAssessmentRelationshipTargetDetailListWithNonNullSet() {
    Set<OpaRelationshipTarget>
        opaRelationshipTargets = new HashSet<>(
            Arrays.asList(new OpaRelationshipTarget(), new OpaRelationshipTarget()));
    List<AssessmentRelationshipTargetDetail> details =
        assessmentMapper.opaRelationshipTargetSetToAssessmentRelationshipTargetDetailList(
            opaRelationshipTargets);

    assertNotNull(details);
    assertEquals(opaRelationshipTargets.size(), details.size());
  }

  @Test
  void testOpaRelationshipTargetToAssessmentRelationshipTargetDetailWithNonNullTargetAndId() {
    OpaRelationshipTarget opaRelationshipTarget = new OpaRelationshipTarget();
    opaRelationshipTarget.setId(1L);
    opaRelationshipTarget.setTargetEntityId("targetEntityId");

    AssessmentRelationshipTargetDetail detail = assessmentMapper.opaRelationshipTargetToAssessmentRelationshipTargetDetail(opaRelationshipTarget);

    assertNotNull(detail);
    assertEquals(1L, detail.getId());
    assertEquals("targetEntityId", detail.getTargetEntityId());
  }

  @Test
  void testOpaRelationshipToAssessmentRelationshipDetailWithNonNullRelationshipAndId() {
    OpaRelationship opaRelationship = new OpaRelationship();
    opaRelationship.setId(1L);
    opaRelationship.setName("relationshipName");
    opaRelationship.setPrepopulated(true);

    OpaRelationshipTarget target1 = new OpaRelationshipTarget();
    target1.setId(1L);
    OpaRelationshipTarget target2 = new OpaRelationshipTarget();
    target2.setId(2L);
    opaRelationship.setRelationshipTargets(new HashSet<>(Arrays.asList(target1, target2)));

    AssessmentRelationshipDetail detail = assessmentMapper.opaRelationshipToAssessmentRelationshipDetail(opaRelationship);

    assertNotNull(detail);
    assertEquals(1L, detail.getId());
    assertEquals("relationshipName", detail.getName());
    assertTrue(detail.getPrepopulated());
    assertEquals(2, detail.getRelationshipTargets().size());
  }


  @Test
  void testOpaRelationshipToAssessmentRelationshipDetailWithNullRelationship() {
    AssessmentRelationshipDetail detail =
        assessmentMapper.opaRelationshipToAssessmentRelationshipDetail(null);
    assertNull(detail);
  }

  @Test
  void testOpaRelationshipListToAssessmentRelationshipDetailListWithNullList() {
    List<AssessmentRelationshipDetail> details =
        assessmentMapper.opaRelationshipListToAssessmentRelationshipDetailList(null);
    assertNull(details);
  }

  @Test
  void testOpaRelationshipListToAssessmentRelationshipDetailListWithNonNullList() {
    List<OpaRelationship> opaRelationships =
        Arrays.asList(new OpaRelationship(), new OpaRelationship());
    List<AssessmentRelationshipDetail> details =
        assessmentMapper.opaRelationshipListToAssessmentRelationshipDetailList(opaRelationships);

    assertNotNull(details);
    assertEquals(opaRelationships.size(), details.size());
  }

  @Test
  void mapIntoOpaSession_WithFullDetail_MapsAllFields() {
    OpaSession opaSession = new OpaSession();
    PatchAssessmentDetail patch = new PatchAssessmentDetail()
        .name("Assessment Name")
        .providerId("Provider123")
        .caseReferenceNumber("CaseRef456")
        .status("Completed");

    assessmentMapper.mapIntoOpaSession(opaSession, patch);

    assertEquals("Assessment Name", opaSession.getAssessment());
    assertEquals("Provider123", opaSession.getOwnerId());
    assertEquals("CaseRef456", opaSession.getTargetId());
    assertEquals("Completed", opaSession.getStatus());
  }

  @Test
  void mapIntoOpaSession_WithPartialDetail_MapsNonNullFields() {
    OpaSession opaSession = new OpaSession();
    PatchAssessmentDetail patch = new PatchAssessmentDetail()
        .name("Partial Name")
        .caseReferenceNumber("PartialCaseRef123");

    assessmentMapper.mapIntoOpaSession(opaSession, patch);

    assertEquals("Partial Name", opaSession.getAssessment());
    assertNull(opaSession.getOwnerId());
    assertEquals("PartialCaseRef123", opaSession.getTargetId());
    assertNull(opaSession.getId());
    assertNull(opaSession.getStatus());
  }

  @Test
  void opaCheckpointToAssessmentCheckpointDetail_returnsNull_whenOpaCheckpointIsNull() {
    AssessmentCheckpointDetail result = assessmentMapper.opaCheckpointToAssessmentCheckpointDetail(null);
    assertNull(result);
  }

  @Test
  void opaCheckpointToAssessmentCheckpointDetail_returnsAssessmentCheckpointDetail_whenOpaCheckpointIsNotNull() {
    OpaCheckpoint opaCheckpoint = mock(OpaCheckpoint.class);
    when(opaCheckpoint.getUsername()).thenReturn("testUser");
    when(opaCheckpoint.getInterviewData()).thenReturn(new byte[0]);

    AssessmentCheckpointDetail result = assessmentMapper.opaCheckpointToAssessmentCheckpointDetail(opaCheckpoint);

    assertNotNull(result);
    assertEquals("testUser", result.getUsername());
    assertEquals("", result.getInterviewData());
  }

  @Test
  void testToOpaListEntity() {
    AssessmentEntityTypeDetail detail = new AssessmentEntityTypeDetail();
    detail.setName("entityType");
    detail.setId(1L);

    OpaListEntity entity = assessmentMapper.toOpaListEntity(detail);

    assertNotNull(entity);
    assertEquals("entityType", entity.getEntityType());
    assertEquals(1L, entity.getId());
  }

  @Test
  void testToOpaListEntityWithNullInput() {
    OpaListEntity entity = assessmentMapper.toOpaListEntity(null);
    assertNull(entity);
  }

  @Test
  void testToOpaEntity() {
    AssessmentEntityDetail detail = new AssessmentEntityDetail();
    detail.setName("entityName");
    detail.setId(1L);
    detail.setPrepopulated(true);

    OpaEntity entity = assessmentMapper.toOpaEntity(detail);

    assertNotNull(entity);
    assertEquals("entityName", entity.getEntityId());
    assertEquals(1L, entity.getId());
    assertTrue(entity.getPrepopulated());
  }

  @Test
  void testToOpaEntityWithNullInput() {
    OpaEntity entity = assessmentMapper.toOpaEntity(null);
    assertNull(entity);
  }

  @Test
  void testToOpaRelationship() {
    AssessmentRelationshipDetail detail = new AssessmentRelationshipDetail();
    detail.setName("relationshipName");
    detail.setId(1L);
    detail.setPrepopulated(true);

    OpaRelationship relationship = assessmentMapper.toOpaRelationship(detail);

    assertNotNull(relationship);
    assertEquals("relationshipName", relationship.getName());
    assertEquals(1L, relationship.getId());
    assertTrue(relationship.getPrepopulated());
  }

  @Test
  void testToOpaRelationshipWithNullInput() {
    OpaRelationship relationship = assessmentMapper.toOpaRelationship(null);
    assertNull(relationship);
  }

  @Test
  void testToOpaAttribute() {
    AssessmentAttributeDetail detail = new AssessmentAttributeDetail();
    detail.setName("attributeName");
    detail.setType("attributeType");
    detail.setId(1L);
    detail.setValue("attributeValue");
    detail.setPrepopulated(true);
    detail.setAsked(true);

    OpaAttribute attribute = assessmentMapper.toOpaAttribute(detail);

    assertNotNull(attribute);
    assertEquals("attributeName", attribute.getAttributeId());
    assertEquals("attributeType", attribute.getAttributeType());
    assertEquals(1L, attribute.getId());
    assertEquals("attributeValue", attribute.getValue());
    assertTrue(attribute.getPrepopulated());
    assertTrue(attribute.getAsked());
  }

  @Test
  void testToOpaAttributeWithNullInput() {
    OpaAttribute attribute = assessmentMapper.toOpaAttribute(null);
    assertNull(attribute);
  }

  @Test
  void testAssessmentAttributeDetailListToOpaAttributeList() {
    AssessmentAttributeDetail detail1 = new AssessmentAttributeDetail();
    detail1.setName("attribute1");
    detail1.setType("type1");
    detail1.setId(1L);
    detail1.setValue("value1");
    detail1.setPrepopulated(true);
    detail1.setAsked(true);

    AssessmentAttributeDetail detail2 = new AssessmentAttributeDetail();
    detail2.setName("attribute2");
    detail2.setType("type2");
    detail2.setId(2L);
    detail2.setValue("value2");
    detail2.setPrepopulated(false);
    detail2.setAsked(false);

    List<AssessmentAttributeDetail> list = Arrays.asList(detail1, detail2);

    List<OpaAttribute> result = assessmentMapper.assessmentAttributeDetailListToOpaAttributeList(list);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("attribute1", result.get(0).getAttributeId());
    assertEquals("type1", result.get(0).getAttributeType());
    assertEquals(1L, result.get(0).getId());
    assertEquals("value1", result.get(0).getValue());
    assertTrue(result.get(0).getPrepopulated());
    assertTrue(result.get(0).getAsked());
    assertEquals("attribute2", result.get(1).getAttributeId());
    assertEquals("type2", result.get(1).getAttributeType());
    assertEquals(2L, result.get(1).getId());
    assertEquals("value2", result.get(1).getValue());
    assertFalse(result.get(1).getPrepopulated());
    assertFalse(result.get(1).getAsked());
  }

  @Test
  void shouldReturnNullWhenAssessmentAttributeDetailListIsNull() {
    List<OpaAttribute> result = assessmentMapper.assessmentAttributeDetailListToOpaAttributeList(null);
    assertNull(result);
  }

  @Test
  void testAssessmentRelationshipDetailListToOpaRelationshipList() {
    AssessmentRelationshipDetail detail1 = new AssessmentRelationshipDetail();
    detail1.setName("relationship1");
    detail1.setId(1L);
    detail1.setPrepopulated(true);

    AssessmentRelationshipDetail detail2 = new AssessmentRelationshipDetail();
    detail2.setName("relationship2");
    detail2.setId(2L);
    detail2.setPrepopulated(false);

    List<AssessmentRelationshipDetail> list = Arrays.asList(detail1, detail2);

    List<OpaRelationship> result = assessmentMapper.assessmentRelationshipDetailListToOpaRelationshipList(list);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("relationship1", result.get(0).getName());
    assertEquals(1L, result.get(0).getId());
    assertTrue(result.get(0).getPrepopulated());
    assertEquals("relationship2", result.get(1).getName());
    assertEquals(2L, result.get(1).getId());
    assertFalse(result.get(1).getPrepopulated());
  }

  @Test
  void shouldReturnNullWhenAssessmentRelationshipDetailListIsNull() {
    List<OpaRelationship> result = assessmentMapper.assessmentRelationshipDetailListToOpaRelationshipList(null);
    assertNull(result);
  }

  @Test
  void testAssessmentRelationshipTargetDetailToOpaRelationshipTarget() {
    AssessmentRelationshipTargetDetail detail = new AssessmentRelationshipTargetDetail();
    detail.setId(1L);
    detail.setTargetEntityId("entity1");

    OpaRelationshipTarget result = assessmentMapper.assessmentRelationshipTargetDetailToOpaRelationshipTarget(detail);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("entity1", result.getTargetEntityId());
  }

  @Test
  void shouldReturnNullWhenAssessmentRelationshipTargetDetailIsNull() {
    OpaRelationshipTarget result = assessmentMapper.assessmentRelationshipTargetDetailToOpaRelationshipTarget(null);
    assertNull(result);
  }





}