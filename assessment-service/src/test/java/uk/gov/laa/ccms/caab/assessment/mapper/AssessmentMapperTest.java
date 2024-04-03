package uk.gov.laa.ccms.caab.assessment.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAttribute;
import uk.gov.laa.ccms.caab.assessment.entity.OpaEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaListEntity;
import uk.gov.laa.ccms.caab.assessment.entity.OpaRelationship;
import uk.gov.laa.ccms.caab.assessment.entity.OpaRelationshipTarget;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;

@ExtendWith(MockitoExtension.class)
class AssessmentMapperTest {

  @InjectMocks
  private AssessmentMapperImpl assessmentMapper = new AssessmentMapperImpl();

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private CommonMapper commonMapper;

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
        .id("1");

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
    assertEquals("1", detail.getId());
    assertEquals("status", detail.getStatus());
  }

  @Test
  void testToAssessmentDetailWithNullSession() {
    AssessmentDetail detail = assessmentMapper.toAssessmentDetail(null);
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
    assertEquals("1", detail.getId());
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
    assertEquals("1", detail.getId());
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
    assertEquals("1", detail.getId());
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
    assertEquals("1", detail.getId());
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
    assertEquals("1", detail.getId());
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


}