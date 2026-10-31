package uk.gov.laa.ccms.caab.assessment.audit;

import static uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl.currentUserHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Clears the auditing user once a request is finished with it, so a pooled thread cannot carry one
 * caller's login id into a later request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditUserCleanupFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } finally {
      currentUserHolder.remove();
    }
  }
}
