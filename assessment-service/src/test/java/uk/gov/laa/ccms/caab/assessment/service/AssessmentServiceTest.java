package uk.gov.laa.ccms.caab.assessment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import uk.gov.laa.ccms.caab.assessment.constants.OpaAssessmentLogMap;
import uk.gov.laa.ccms.caab.assessment.entity.OpaAssessmentLog;
import uk.gov.laa.ccms.caab.assessment.entity.OpaCheckpoint;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.repository.OpaAssessmentLogRepository;
import uk.gov.laa.ccms.caab.assessment.repository.OpaSessionRepository;

@SuppressWarnings({"unchecked"})
@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

  @Mock
  private OpaSessionRepository opaSessionRepository;

  @Mock
  private OpaAssessmentLogRepository opaAssessmentLogRepository;

  @Mock
  private AssessmentMapper assessmentMapper;

  @InjectMocks
  private AssessmentService assessmentService;

  @Test
  void testGetAssessments() {

    String providerId = "providerId";
    String caseReferenceNumber = "caseReferenceNumber";
    String name = "name";
    String status = "status";

    AssessmentDetail criteria = new AssessmentDetail()
        .providerId(providerId)
        .caseReferenceNumber(caseReferenceNumber)
        .status(status);

    List<String> names = new ArrayList<>(List.of(name));

    OpaSession session = new OpaSession();

    when(assessmentMapper.toOpaSession(criteria))
        .thenReturn(session);
    when(opaSessionRepository.findAll(any(Specification.class)))
        .thenReturn(List.of(session));
    when(assessmentMapper.toAssessmentDetails(List.of(session)))
        .thenReturn(new AssessmentDetails());

    assessmentService.getAssessments(criteria, names);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(any(Specification.class));
    verify(assessmentMapper).toAssessmentDetails(List.of(session));
  }

  @Test
  void testGetAssessmentFound() {
    Long assessmentId = 1L;
    OpaSession session = new OpaSession();
    AssessmentDetail expectedDetail = new AssessmentDetail();

    when(opaSessionRepository.findById(assessmentId))
        .thenReturn(Optional.of(session));
    when(assessmentMapper.toAssessmentDetail(session))
        .thenReturn(expectedDetail);

    AssessmentDetail result = assessmentService.getAssessment(assessmentId);

    assertNotNull(result);
    assertEquals(expectedDetail, result);
    verify(opaSessionRepository).findById(assessmentId);
    verify(assessmentMapper).toAssessmentDetail(session);
  }

  @Test
  void testGetAssessmentNotFound() {
    Long assessmentId = 1L;

    when(opaSessionRepository.findById(assessmentId))
        .thenReturn(Optional.empty());

    ApplicationException thrown = assertThrows(ApplicationException.class, () -> {
      assessmentService.getAssessment(assessmentId);
    });

    assertEquals(HttpStatus.NOT_FOUND, thrown.getHttpStatus());
    assertTrue(thrown.getMessage().contains("Assessment with id " + assessmentId + " not found"));
    verify(opaSessionRepository).findById(assessmentId);
  }

  @Test
  void testUpdateAssessmentSuccess() {
    final Long assessmentId = 1L;
    final PatchAssessmentDetail patch = new PatchAssessmentDetail();
    final OpaSession opaSession = new OpaSession();

    when(opaSessionRepository.findById(assessmentId)).thenReturn(Optional.of(opaSession));

    assessmentService.patchAssessment(assessmentId, patch);

    verify(opaSessionRepository).findById(assessmentId);
    verify(assessmentMapper).mapIntoOpaSession(opaSession, patch);
    verify(opaSessionRepository).save(opaSession);
  }

  @Test
  void testUpdateAssessmentNotFound() {
    final Long assessmentId = 1L;
    final PatchAssessmentDetail patch = new PatchAssessmentDetail();

    when(opaSessionRepository.findById(assessmentId)).thenReturn(Optional.empty());

    ApplicationException thrown = assertThrows(ApplicationException.class, () -> {
      assessmentService.patchAssessment(assessmentId, patch);
    });

    assertEquals(HttpStatus.NOT_FOUND, thrown.getHttpStatus());
    assertTrue(thrown.getMessage().contains("Assessment with id " + assessmentId + " not found"));
    verify(opaSessionRepository).findById(assessmentId);
    verify(assessmentMapper, never()).mapIntoOpaSession(any(OpaSession.class), any(PatchAssessmentDetail.class));
    verify(opaSessionRepository, never()).save(any(OpaSession.class));
  }

  @Test
  void deleteAssessments_deletesAssessmentsMatchingCriteriaAndNames() {
    AssessmentDetail criteria = new AssessmentDetail()
        .providerId("providerId")
        .caseReferenceNumber("caseReferenceNumber")
        .status("status");
    List<String> names = List.of("name1", "name2");

    OpaSession session = new OpaSession();
    when(assessmentMapper.toOpaSession(criteria)).thenReturn(session);
    when(opaSessionRepository.findAll(any(Specification.class))).thenReturn(List.of(session));

    assessmentService.deleteAssessments(criteria, names);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(any(Specification.class));
    verify(opaSessionRepository).deleteAll(List.of(session));
  }

  @Test
  void deleteAssessments_doesNothingWhenNoMatchingAssessments() {
    AssessmentDetail criteria = new AssessmentDetail()
        .providerId("nonExistentProviderId")
        .caseReferenceNumber("nonExistentCaseReferenceNumber")
        .status("nonExistentStatus");
    List<String> names = List.of("nonExistentName");

    OpaSession session = new OpaSession();
    when(assessmentMapper.toOpaSession(criteria)).thenReturn(session);
    when(opaSessionRepository.findAll(any(Specification.class))).thenReturn(List.of());

    assessmentService.deleteAssessments(criteria, names);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(any(Specification.class));
  }

  @Test
  @DisplayName("deleteAssessments should delete assessments and related logs")
  void deleteAssessments_deletesAssessmentsAndLogs() {
    AssessmentDetail criteria = new AssessmentDetail()
        .providerId("providerId")
        .caseReferenceNumber("caseReferenceNumber")
        .status("status");
    List<String> names = List.of("name1", "name2");

    OpaSession session = new OpaSession();
    session.setAssessment("meritsAssessment");
    session.setTargetId("123");
    List<OpaSession> sessionsToDelete = List.of(session);
    Set<OpaAssessmentLog> logsToDelete = new HashSet<>();
    OpaAssessmentLog log = new OpaAssessmentLog();
    logsToDelete.add(log);

    when(assessmentMapper.toOpaSession(criteria)).thenReturn(session);
    when(opaSessionRepository.findAll(any(Specification.class))).thenReturn(sessionsToDelete);
    when(opaAssessmentLogRepository.findByTargetIdAndAssessment(session.getTargetId(), "MERITS"))
        .thenReturn(List.of(log));
    doNothing().when(opaAssessmentLogRepository).deleteAll(logsToDelete);
    doNothing().when(opaSessionRepository).deleteAll(sessionsToDelete);

    assessmentService.deleteAssessments(criteria, names);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(any(Specification.class));
    verify(opaAssessmentLogRepository).findByTargetIdAndAssessment(session.getTargetId(), "MERITS");
    verify(opaAssessmentLogRepository).deleteAll(logsToDelete);
    verify(opaSessionRepository).deleteAll(sessionsToDelete);
  }

  @Test
  @DisplayName("deleteAssessments does nothing when no sessions or logs found")
  void deleteAssessments_doesNothingWhenNoSessionsOrLogs() {
    AssessmentDetail criteria = new AssessmentDetail()
        .providerId("providerId")
        .caseReferenceNumber("caseReferenceNumber")
        .status("status");
    List<String> names = List.of("name1", "name2");

    OpaSession session = new OpaSession();
    session.setAssessment("meritsAssessment");
    session.setTargetId("123");

    when(assessmentMapper.toOpaSession(criteria)).thenReturn(session);
    when(opaSessionRepository.findAll(any(Specification.class))).thenReturn(List.of());

    assessmentService.deleteAssessments(criteria, names);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(any(Specification.class));
    verify(opaAssessmentLogRepository, never()).deleteAll(anySet());
    verify(opaSessionRepository, never()).deleteAll(anyList());
  }

  @Test
  @DisplayName("collectLogsToDelete should collect logs associated with sessions to delete")
  void collectLogsToDelete_collectsLogs() {
    // Arrange
    OpaSession session = new OpaSession();
    session.setAssessment("meritsAssessment");
    session.setTargetId("123");
    List<OpaSession> sessionsToDelete = List.of(session);
    OpaAssessmentLog log = new OpaAssessmentLog();
    Set<OpaAssessmentLog> expectedLogs = Set.of(log);

    when(opaAssessmentLogRepository.findByTargetIdAndAssessment("123", "MERITS"))
        .thenReturn(List.of(log));

    Set<OpaAssessmentLog> logsToDelete = assessmentService.collectLogsToDelete(sessionsToDelete);

    assertEquals(expectedLogs, logsToDelete);
    verify(opaAssessmentLogRepository).findByTargetIdAndAssessment("123", "MERITS");
  }

  @Test
  @DisplayName("deleteLogs should delete logs when not empty")
  void deleteLogs_deletesLogsWhenNotEmpty() {
    OpaAssessmentLog log = new OpaAssessmentLog();
    Set<OpaAssessmentLog> logsToDelete = Set.of(log);

    assessmentService.deleteLogs(logsToDelete);

    verify(opaAssessmentLogRepository).deleteAll(logsToDelete);
  }

  @Test
  @DisplayName("deleteLogs does nothing when logs to delete are empty")
  void deleteLogs_doesNothingWhenLogsEmpty() {
    Set<OpaAssessmentLog> logsToDelete = Set.of();

    assessmentService.deleteLogs(logsToDelete);

    verify(opaAssessmentLogRepository, never()).deleteAll(anySet());
  }


  @Test
  void deleteCheckpoint_deletesCheckpointWhenExists() {
    Long assessmentId = 1L;
    OpaSession session = new OpaSession();
    session.setCheckpoint(new OpaCheckpoint());

    when(opaSessionRepository.findById(assessmentId)).thenReturn(Optional.of(session));

    assessmentService.deleteCheckpoint(assessmentId);

    verify(opaSessionRepository).findById(assessmentId);
    verify(opaSessionRepository).save(session);
    assertNull(session.getCheckpoint());
  }

  @Test
  void deleteCheckpoint_throwsExceptionWhenCheckpointDoesNotExist() {
    Long assessmentId = 1L;

    when(opaSessionRepository.findById(assessmentId)).thenReturn(Optional.empty());

    ApplicationException thrown = assertThrows(ApplicationException.class, () -> {
      assessmentService.deleteCheckpoint(assessmentId);
    });

    assertEquals(HttpStatus.NOT_FOUND, thrown.getHttpStatus());
    assertTrue(thrown.getMessage().contains("Assessment checkpoint with id: " + assessmentId + " not found"));
    verify(opaSessionRepository).findById(assessmentId);
  }

  @Test
  void updateAssessment_updatesExistingAssessment() {
    Long existingAssessmentId = 1L;
    AssessmentDetail assessmentDetail = new AssessmentDetail();
    assessmentDetail.setId(existingAssessmentId);
    OpaSession existingOpaSession = new OpaSession();
    existingOpaSession.setId(existingAssessmentId);

    when(assessmentMapper.toOpaSession(assessmentDetail)).thenReturn(existingOpaSession);
    when(opaSessionRepository.existsById(existingAssessmentId)).thenReturn(true);

    assessmentService.updateAssessment(existingAssessmentId, assessmentDetail);

    verify(opaSessionRepository).save(existingOpaSession);
  }

  @Test
  void updateAssessment_throwsExceptionWhenAssessmentNotFound() {
    Long nonExistingAssessmentId = 2L;
    AssessmentDetail assessmentDetail = new AssessmentDetail();
    assessmentDetail.setId(nonExistingAssessmentId);
    OpaSession nonExistingOpaSession = new OpaSession();
    nonExistingOpaSession.setId(nonExistingAssessmentId);

    when(opaSessionRepository.existsById(nonExistingAssessmentId)).thenReturn(false);

    Exception exception = assertThrows(ApplicationException.class, () -> {
      assessmentService.updateAssessment(nonExistingAssessmentId, assessmentDetail);
    });

    assertEquals(String.format("Assessment with id %s not found", nonExistingAssessmentId), exception.getMessage());
    verify(opaSessionRepository, never()).save(any());
  }

  @Test
  void testCreateAssessment() {
    AssessmentDetail assessmentDetail = new AssessmentDetail();
    OpaSession session = new OpaSession();
    session.setId(1L);

    when(assessmentMapper.toOpaSession(assessmentDetail)).thenReturn(session);
    when(opaSessionRepository.save(session)).thenReturn(session);

    Long createdId = assessmentService.createAssessment(assessmentDetail);

    assertNotNull(createdId);
    assertEquals(session.getId(), createdId);
    verify(assessmentMapper).toOpaSession(assessmentDetail);
    verify(opaSessionRepository).save(session);
  }

  @Test
  void testBuildQuerySpecification() {
    OpaSession session = new OpaSession();
    Example<OpaSession> example = Example.of(session);
    List<String> names = List.of("name1", "name2");

    Specification<OpaSession> specification = assessmentService.buildQuerySpecification(example, names);

    assertNotNull(specification);
  }


}