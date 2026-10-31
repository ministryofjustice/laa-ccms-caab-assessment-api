package uk.gov.laa.ccms.caab.assessment.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Writes security outcomes as structured security events. */
@Component
@Slf4j
public class SecurityEventLogger {

  static final String EVENT_TYPE = "event_type";
  static final String OUTCOME = "outcome";

  /** The values of the {@code event_type} log field that the SOC use cases match on. */
  public enum EventType {
    INPUT_REJECTED
  }

  /** The values of the {@code outcome} log field. */
  public enum Outcome {
    DENIED
  }

  /**
   * Records a request the application refused as malformed or not allowed. Only the exception
   * class, method and path are logged, as the values supplied are user input.
   *
   * @param method the HTTP method
   * @param path the request path, without its query string
   * @param exception the exception that described the rejection
   */
  public void inputRejected(String method, String path, Exception exception) {
    log.atWarn()
        .addKeyValue(EVENT_TYPE, EventType.INPUT_REJECTED.name())
        .addKeyValue(OUTCOME, Outcome.DENIED.name())
        .addKeyValue("event_detail", exception.getClass().getSimpleName())
        .addKeyValue("http_method", method)
        .addKeyValue("path", path)
        .log("Request input rejected");
  }
}
