package uk.gov.laa.ccms.caab.assessment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import uk.gov.laa.ccms.caab.assessment.entity.OpaSession;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.ccms.caab.assessment.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.repository.OpaSessionRepository;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

  @Mock
  private OpaSessionRepository opaSessionRepository;

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
        .name(name)
        .status(status);

    OpaSession session = new OpaSession();

    when(assessmentMapper.toOpaSession(criteria))
        .thenReturn(session);
    when(opaSessionRepository.findAll(Example.of(session)))
        .thenReturn(List.of(session));
    when(assessmentMapper.toAssessmentDetails(List.of(session)))
        .thenReturn(new AssessmentDetails());

    assessmentService.getAssessments(criteria);

    verify(assessmentMapper).toOpaSession(criteria);
    verify(opaSessionRepository).findAll(Example.of(session));
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




}