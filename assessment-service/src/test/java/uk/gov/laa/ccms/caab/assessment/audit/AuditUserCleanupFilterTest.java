package uk.gov.laa.ccms.caab.assessment.audit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl.currentUserHolder;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditUserCleanupFilterTest {

  private final AuditUserCleanupFilter filter = new AuditUserCleanupFilter();

  @BeforeEach
  @AfterEach
  void clearCurrentUser() {
    currentUserHolder.remove();
  }

  @Test
  void clearsCurrentUserAfterRequest() throws Exception {
    filter.doFilter(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        (req, res) -> currentUserHolder.set("SOMEUSER"));

    assertNull(currentUserHolder.get());
  }

  @Test
  void clearsCurrentUserWhenChainThrows() {
    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (req, res) -> {
                  currentUserHolder.set("SOMEUSER");
                  throw new ServletException("synthetic failure");
                }));

    assertNull(currentUserHolder.get());
  }

  @Test
  void laterRequestOnSameThreadDoesNotInheritPreviousUser() throws Exception {
    filter.doFilter(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        (req, res) -> currentUserHolder.set("FIRSTUSER"));

    String[] seen = new String[] {"unset"};
    filter.doFilter(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        (req, res) -> seen[0] = currentUserHolder.get());

    assertNull(seen[0]);
  }
}
