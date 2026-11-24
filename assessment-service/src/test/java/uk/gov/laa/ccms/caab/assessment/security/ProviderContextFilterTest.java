package uk.gov.laa.ccms.caab.assessment.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;

class ProviderContextFilterTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ProviderContextFilter filter = new ProviderContextFilter();
  private final ProviderContextConfiguration configuration = new ProviderContextConfiguration();

  @BeforeEach
  @AfterEach
  void clearProviderContext() {
    ProviderContext.clear();
  }

  @Test
  void acceptsTrimmedNumericProviderIdAndClearsAfterRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments");
    request.addHeader(ProviderContext.HEADER_NAME, "  12345  ");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenProviderId = new AtomicReference<>();

    filter.doFilter(
        request, response, (req, res) -> seenProviderId.set(ProviderContext.getProviderId()));

    assertEquals("12345", seenProviderId.get());
    assertNull(ProviderContext.getProviderId());
  }

  @Test
  void clearsProviderContextWhenFilterChainThrows() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments");
    request.addHeader(ProviderContext.HEADER_NAME, "12345");

    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                  assertEquals("12345", ProviderContext.getRequiredProviderId());
                  throw new ServletException("synthetic failure");
                }));

    assertNull(ProviderContext.getProviderId());
  }

  @Test
  void rejectsMissingHeaderOnAssessmentsWith400JsonFormat() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

    assertFalse(chainInvoked.get());
    assertEquals(400, response.getStatus());
    JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
    assertEquals(400, body.get("http_status").asInt());
    assertEquals("Missing required header: Caab-Provider-Id", body.get("error_message").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void rejectsBlankHeaderOnAssessmentsWith400(final String blankValue) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments/1");
    request.addHeader(ProviderContext.HEADER_NAME, blankValue);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

    assertFalse(chainInvoked.get());
    assertEquals(400, response.getStatus());
    JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
    assertEquals(400, body.get("http_status").asInt());
    assertEquals("Missing required header: Caab-Provider-Id", body.get("error_message").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "-1", "12a34", "12.34", "+123"})
  void rejectsNonNumericHeaderWithoutReflectingInput(final String invalidValue) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments");
    request.addHeader(ProviderContext.HEADER_NAME, invalidValue);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

    assertFalse(chainInvoked.get());
    assertEquals(400, response.getStatus());
    String content = response.getContentAsString();
    assertFalse(content.contains(invalidValue));
    JsonNode body = OBJECT_MAPPER.readTree(content);
    assertEquals(400, body.get("http_status").asInt());
    assertEquals(
        "Invalid Caab-Provider-Id header: must be numeric", body.get("error_message").asText());
  }

  @Test
  void rejectsMultipleProviderIdHeadersWith400() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assessments");
    request.addHeader(ProviderContext.HEADER_NAME, "1001");
    request.addHeader(ProviderContext.HEADER_NAME, "1002");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

    assertFalse(chainInvoked.get());
    assertEquals(400, response.getStatus());
    JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
    assertEquals(400, body.get("http_status").asInt());
    assertEquals(
        "Multiple Caab-Provider-Id headers are not allowed", body.get("error_message").asText());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator",
        "/actuator/health",
        "/actuator/health/liveness",
        "/actuator/prometheus",
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/v3/api-docs",
        "/v3/api-docs/swagger-config",
        "/api-docs",
        "/api-docs/swagger-config",
        "/open-api-specification.yml",
        "/favicon.ico",
        "/error"
      })
  void exemptRoutesDoNotRequireProviderHeader(final String exemptPath) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", exemptPath);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

    assertTrue(chainInvoked.get());
    assertEquals(200, response.getStatus());
  }

  @Test
  void contextPathAndServletPathResolutionWorksAndRejectsTraversal() throws Exception {
    MockHttpServletRequest contextRequest =
        new MockHttpServletRequest("GET", "/api/actuator/health");
    contextRequest.setContextPath("/api");
    AtomicBoolean exemptInvoked = new AtomicBoolean(false);
    filter.doFilter(
        contextRequest, new MockHttpServletResponse(), (r, s) -> exemptInvoked.set(true));
    assertTrue(exemptInvoked.get());

    MockHttpServletRequest servletPathRequest = new MockHttpServletRequest("GET", "/assessments");
    servletPathRequest.setServletPath("/assessments");
    MockHttpServletResponse servletPathResponse = new MockHttpServletResponse();
    filter.doFilter(servletPathRequest, servletPathResponse, (r, s) -> {});
    assertEquals(400, servletPathResponse.getStatus());

    MockHttpServletRequest traversalRequest =
        new MockHttpServletRequest("GET", "/actuator/../assessments");
    MockHttpServletResponse traversalResponse = new MockHttpServletResponse();
    filter.doFilter(traversalRequest, traversalResponse, (r, s) -> {});
    assertEquals(400, traversalResponse.getStatus());
  }

  @Test
  void providerContextRejectsInvalidDirectSetAndMissingRequiredLookup() {
    ApplicationException missingEx =
        assertThrows(ApplicationException.class, ProviderContext::getRequiredProviderId);
    assertEquals(HttpStatus.BAD_REQUEST, missingEx.getHttpStatus());

    ApplicationException invalidEx =
        assertThrows(ApplicationException.class, () -> ProviderContext.setProviderId("abc"));
    assertEquals(HttpStatus.BAD_REQUEST, invalidEx.getHttpStatus());
    assertFalse(ProviderContext.isValidProviderId(null));
  }

  @Test
  void filterRegistrationRunsAfterSpringSecurityForRequestDispatches() throws Exception {
    ServletContext servletContext = mock(ServletContext.class);
    FilterRegistration.Dynamic dynamic = mock(FilterRegistration.Dynamic.class);
    when(servletContext.addFilter(anyString(), any(Filter.class))).thenReturn(dynamic);

    FilterRegistrationBean<ProviderContextFilter> registration =
        configuration.providerContextFilterRegistration(filter);
    registration.onStartup(servletContext);

    assertEquals(ProviderContextConfiguration.AFTER_SECURITY_FILTER_ORDER, registration.getOrder());
    assertTrue(registration.getOrder() > -100);

    @SuppressWarnings("unchecked")
    EnumSet<DispatcherType> dispatchTypes =
        mockingDetails(dynamic).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("addMappingForUrlPatterns"))
            .map(i -> (EnumSet<DispatcherType>) i.getArgument(0))
            .findFirst()
            .orElseThrow();
    assertEquals(EnumSet.of(DispatcherType.REQUEST), dispatchTypes);
  }
}
