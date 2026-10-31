package uk.gov.laa.ccms.caab.assessment.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds an opaque logging identifier without exposing authentication session IDs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SessionCorrelationFilter extends OncePerRequestFilter {

  private static final String ATTRIBUTE = SessionCorrelationFilter.class.getName() + ".sessionID";
  private static final String REQUEST_ID_ATTRIBUTE =
      SessionCorrelationFilter.class.getName() + ".requestId";
  private static final String REQUEST_ID_KEY = "request_id";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String previous = MDC.get("sessionID");
    String previousRequestId = MDC.get(REQUEST_ID_KEY);
    try {
      String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);
      if (requestId == null) {
        requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
      }
      MDC.put(REQUEST_ID_KEY, requestId);
      response.setHeader(REQUEST_ID_HEADER, requestId);
      String id = (String) request.getAttribute(ATTRIBUTE);
      if (id == null) {
        id = correlationId(request);
        request.setAttribute(ATTRIBUTE, id);
      }
      MDC.put("sessionID", id);
      chain.doFilter(request, response);
    } finally {
      if (previous == null) {
        MDC.remove("sessionID");
      } else {
        MDC.put("sessionID", previous);
      }
      if (previousRequestId == null) {
        MDC.remove(REQUEST_ID_KEY);
      } else {
        MDC.put(REQUEST_ID_KEY, previousRequestId);
      }
    }
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  private String correlationId(HttpServletRequest request) {
    // Stateless API calls must not create an HTTP session just for logging.
    HttpSession session = request.getSession(false);
    if (session == null) {
      return UUID.randomUUID().toString();
    }
    synchronized (session) {
      String id = (String) session.getAttribute(ATTRIBUTE);
      if (id == null) {
        id = UUID.randomUUID().toString();
        session.setAttribute(ATTRIBUTE, id);
      }
      return id;
    }
  }
}
