package uk.gov.laa.ccms.caab.assessment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the {@code Caab-Provider-Id} request header on provider-owned endpoints and clears the
 * thread-local provider context when the request completes.
 */
@Component
public class ProviderContextFilter extends OncePerRequestFilter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    String path = resolveApplicationPath(request);
    if (path == null || path.contains("..")) {
      return false;
    }
    return matchesSegmentPrefix(path, "/actuator")
        || "/swagger-ui.html".equals(path)
        || matchesSegmentPrefix(path, "/swagger-ui")
        || matchesSegmentPrefix(path, "/v3/api-docs")
        || matchesSegmentPrefix(path, "/api-docs")
        || "/open-api-specification.yml".equals(path)
        || "/favicon.ico".equals(path)
        || matchesSegmentPrefix(path, "/error");
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    try {
      List<String> values = Collections.list(request.getHeaders(ProviderContext.HEADER_NAME));
      if (values.isEmpty() || values.getFirst() == null || values.getFirst().isBlank()) {
        writeBadRequest(response, "Missing required header: " + ProviderContext.HEADER_NAME);
        return;
      }
      if (values.size() > 1) {
        writeBadRequest(
            response, "Multiple " + ProviderContext.HEADER_NAME + " headers are not allowed");
        return;
      }
      String trimmed = values.getFirst().trim();
      if (!ProviderContext.isValidProviderId(trimmed)) {
        writeBadRequest(
            response, "Invalid " + ProviderContext.HEADER_NAME + " header: must be numeric");
        return;
      }
      ProviderContext.setProviderId(trimmed);
      filterChain.doFilter(request, response);
    } finally {
      ProviderContext.clear();
    }
  }

  private static String resolveApplicationPath(final HttpServletRequest request) {
    String servletPath = request.getServletPath();
    if (servletPath != null && !servletPath.isEmpty()) {
      return servletPath;
    }
    String requestUri = request.getRequestURI();
    if (requestUri == null) {
      return null;
    }
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
      return requestUri.substring(contextPath.length());
    }
    return requestUri;
  }

  private static boolean matchesSegmentPrefix(final String path, final String prefix) {
    return path.equals(prefix) || path.startsWith(prefix + "/");
  }

  private static void writeBadRequest(final HttpServletResponse response, final String message)
      throws IOException {
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error_message", message);
    body.put("http_status", HttpStatus.BAD_REQUEST.value());
    OBJECT_MAPPER.writeValue(response.getWriter(), body);
  }
}
