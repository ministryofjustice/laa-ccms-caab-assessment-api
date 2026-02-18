package uk.gov.laa.ccms.caab.assessment.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl;

@ExtendWith(MockitoExtension.class)
class AuditAdviceTest {

  @Mock
  private AuditorAwareImpl auditorAware;

  @InjectMocks
  private AuditAdvice auditAdvice;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    // Setup MockMvc with the AuditAdvice
    mockMvc = MockMvcBuilders.standaloneSetup(new MockController())
        .setControllerAdvice(auditAdvice)
        .build();
  }

  @AfterEach
  public void tearDown() {
    // Reset currentUserHolder after each test
    AuditorAwareImpl.currentUserHolder.remove();
  }

  @Test
  void setCurrentUserHolderIfAvailable() throws Exception {
    String caabUserLoginId = "testUser";

    mockMvc.perform(get("/test")
            .header("Caab-User-Login-Id", caabUserLoginId))
        .andDo(print())
        .andExpect(status().isOk());

    assertEquals(caabUserLoginId, AuditorAwareImpl.currentUserHolder.get());
  }

  @Test
  void currentUserHolderIsNullWhenCaabUserLoginIdNotProvided() throws Exception {
    mockMvc.perform(get("/test")) // No Caab-User-Login-Id header
        .andDo(print())
        .andExpect(status().isOk());

    // Assert that currentUserHolder is null or unchanged
    assertNull(AuditorAwareImpl.currentUserHolder.get());
  }

  // Mock controller for testing purposes
  @RestController
  private static class MockController {
    @GetMapping("/test")
    public void testEndpoint() {
      // Test endpoint for AuditAdvice
    }
  }
}
