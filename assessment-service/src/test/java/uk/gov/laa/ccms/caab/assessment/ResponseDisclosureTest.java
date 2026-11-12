package uk.gov.laa.ccms.caab.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.endpoint.web.WebMvcHealthEndpointExtensionAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.laa.ccms.caab.assessment.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;
import uk.gov.laa.springboot.oauth2.SecurityFilterChainAutoConfiguration;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtDecoder;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtToken;

/**
 * Exercises the production YAML through real HTTP, including the container's error dispatch. Only
 * infrastructure auto-configuration is imported: no database, downstream service or auth mock.
 */
@SpringBootTest(
    classes = ResponseDisclosureTest.Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.config.location=file:src/main/resources/application.yml",
      "OAUTH2_ISSUER_URI=https://login.microsoftonline.com/disclosure-test-tenant/v2.0",
      "OAUTH2_AUDIENCE=api://laa-ccms-caab-assessment-api",
      "OAUTH2_ROLES=[{\"name\":\"ALL\",\"URIs\":[\"/**\"]}]",
      "OAUTH2_UNPROTECTED_URIS=/actuator/health/**"
    })
class ResponseDisclosureTest {

  private static final String TOKEN = "synthetic-disclosure-token";
  private static final String DIAGNOSTIC = "synthetic-private-diagnostic";
  private static final String BUSINESS_MESSAGE = "The requested record is not available";
  private final HttpClient client = HttpClient.newHttpClient();
  private final JsonMapper mapper = JsonMapper.builder().build();

  @LocalServerPort private int port;
  @Autowired private AtomicBoolean failed;
  @Autowired private ConfigurableApplicationContext context;

  @Test
  void healthAndProbesHideDiagnosticsAndPreserveFailureSemantics() throws Exception {
    assertHealthAndProbes(port, failed, context, false);
  }

  @Nested
  @TestPropertySource(properties = "OAUTH2_UNPROTECTED_URIS=/not-health")
  class AuthenticatedHealthTests {
    @LocalServerPort private int protectedPort;
    @Autowired private AtomicBoolean protectedFailed;
    @Autowired private ConfigurableApplicationContext protectedContext;

    @Test
    void authenticatedHealthAndProbesHideDiagnosticsAndPreserveFailureSemantics() throws Exception {
      for (String suffix : new String[] {"", "/liveness", "/readiness"}) {
        String path = "/actuator/health" + suffix;
        assertThat(get(protectedPort, path, "application/json", null).statusCode()).isIn(401, 403);
        assertThat(get(protectedPort, path, "application/json", "invalid-token").statusCode())
            .isIn(401, 403);
      }
      assertHealthAndProbes(protectedPort, protectedFailed, protectedContext, true);
    }
  }

  private void assertHealthAndProbes(
      int serverPort,
      AtomicBoolean failed,
      ConfigurableApplicationContext context,
      boolean authenticated)
      throws Exception {
    try {
      assertHealth(serverPort, "", 200, "UP", authenticated);
      assertHealth(serverPort, "/liveness", 200, "UP", authenticated);
      assertHealth(serverPort, "/readiness", 200, "UP", authenticated);

      failed.set(true);
      assertHealth(serverPort, "", 503, "DOWN", authenticated);
      // A failed dependency must not turn a live, ready application into a failed probe.
      assertHealth(serverPort, "/liveness", 200, "UP", authenticated);
      assertHealth(serverPort, "/readiness", 200, "UP", authenticated);

      AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
      AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
      assertHealth(serverPort, "/liveness", 503, "DOWN", authenticated);
      assertHealth(serverPort, "/readiness", 503, "OUT_OF_SERVICE", authenticated);
    } finally {
      failed.set(false);
      AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
      AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", "text/html"})
  void fallbackNeverDisclosesDiagnosticsEvenWhenRequested(String accept) throws Exception {
    for (String path :
        new String[] {"/failure", "/binding?value=" + DIAGNOSTIC, "/number?value=not-a-number"}) {
      String separator = path.contains("?") ? "&" : "?";
      HttpResponse<String> response =
          get(path + separator + "trace=true&message=true&errors=true", accept);
      int expected = path.equals("/failure") ? 500 : 400;
      assertThat(response.statusCode()).isEqualTo(expected);
      assertThat(response.headers().firstValue("Content-Type").orElseThrow()).contains(accept);
      assertThat(response.body())
          .doesNotContain(
              DIAGNOSTIC,
              "IllegalStateException",
              "BindException",
              "NumberFormatException",
              "typeMismatch",
              "not-a-number",
              "Failed to convert");
      if (accept.equals("application/json")) {
        JsonNode body = mapper.readTree(response.body());
        assertThat(body.get("status").asInt()).isEqualTo(expected);
        assertThat(body.has("message")).isFalse();
        assertThat(body.has("exception")).isFalse();
        assertThat(body.has("trace")).isFalse();
        assertThat(body.has("errors")).isFalse();
      }
    }
  }

  @Test
  void intentionalBusinessErrorAndValidInputAreUnchanged() throws Exception {
    assertThat(get("/number?value=42", "application/json", false).statusCode()).isIn(401, 403);
    HttpResponse<String> business = get("/business", "application/json");
    assertThat(business.statusCode()).isEqualTo(400);
    assertThat(mapper.readTree(business.body()).get("error_message").asText())
        .isEqualTo(BUSINESS_MESSAGE);
    HttpResponse<String> valid = get("/number?value=42", "application/json");
    assertThat(valid.statusCode()).isEqualTo(200);
    assertThat(valid.body()).isEqualTo("42");
  }

  private void assertHealth(
      int serverPort, String suffix, int status, String health, boolean authenticated)
      throws Exception {
    // Boot advertises probe group names at the root independently of diagnostic visibility.
    String groups = suffix.isEmpty() ? ",\"groups\":[\"liveness\",\"readiness\"]" : "";
    HttpResponse<String> response =
        get(
            serverPort,
            "/actuator/health" + suffix,
            "application/json",
            authenticated ? TOKEN : null);
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(mapper.readTree(response.body()))
        .isEqualTo(mapper.readTree("{\"status\":\"" + health + "\"" + groups + "}"));
  }

  private HttpResponse<String> get(String path, String accept) throws Exception {
    return get(path, accept, true);
  }

  private HttpResponse<String> get(String path, String accept, boolean authenticated)
      throws Exception {
    return get(port, path, accept, authenticated ? TOKEN : null);
  }

  private HttpResponse<String> get(int serverPort, String path, String accept, String token)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + path))
            .header("Accept", accept);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  @TestComponent
  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({
    TomcatServletWebServerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class,
    SecurityFilterChainAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    EndpointAutoConfiguration.class,
    WebEndpointAutoConfiguration.class,
    HealthContributorRegistryAutoConfiguration.class,
    HealthEndpointAutoConfiguration.class,
    WebMvcHealthEndpointExtensionAutoConfiguration.class,
    ServletManagementContextAutoConfiguration.class,
    ManagementContextAutoConfiguration.class,
    ApplicationAvailabilityAutoConfiguration.class,
    AvailabilityHealthContributorAutoConfiguration.class,
    AvailabilityProbesAutoConfiguration.class
  })
  @Import({FailureController.class, GlobalExceptionHandler.class})
  static class Application {
    @Bean
    AtomicBoolean failed() {
      return new AtomicBoolean();
    }

    /** Stands in for the tenant resolver: this test is about what responses disclose. */
    @Bean
    JwtDecoder jwtDecoder() {
      return StubJwtDecoder.of(
          new StubJwtToken(TOKEN, "disclosure-test", new String[] {"ALL"}, null, Map.of()));
    }

    @Bean
    HealthIndicator syntheticHealthIndicator(AtomicBoolean failed) {
      return () ->
          (failed.get() ? Health.down(new IllegalStateException(DIAGNOSTIC)) : Health.up())
              .withDetail("privatePath", "/synthetic/private/path")
              .build();
    }
  }

  @TestComponent
  @RestController
  static class FailureController {
    @GetMapping("/failure")
    void failure() {
      throw new IllegalStateException(DIAGNOSTIC);
    }

    @GetMapping("/binding")
    int binding(@ModelAttribute Input input) {
      return input.getValue();
    }

    @GetMapping("/number")
    int number(@RequestParam int value) {
      return value;
    }

    @GetMapping("/business")
    void business() {
      throw new ApplicationException(BUSINESS_MESSAGE, HttpStatus.BAD_REQUEST);
    }

    static class Input {
      private int value;

      public int getValue() {
        return value;
      }

      public void setValue(int value) {
        this.value = value;
      }
    }
  }
}
