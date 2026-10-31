package uk.gov.laa.ccms.caab.assessment.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class SecurityEventLoggerTest {

  private final SecurityEventLogger logger = new SecurityEventLogger();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger target = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);

  @BeforeEach
  void attach() {
    appender.start();
    target.addAppender(appender);
  }

  @AfterEach
  void detach() {
    target.detachAppender(appender);
  }

  @Test
  void inputRejectedLogsClassMethodAndPathOnly() {
    logger.inputRejected("POST", "/assessments", new MaxUploadSizeExceededException(1L));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("INPUT_REJECTED", keyValue(event, "event_type"));
    assertEquals("DENIED", keyValue(event, "outcome"));
    assertEquals("MaxUploadSizeExceededException", keyValue(event, "event_detail"));
    assertEquals("POST", keyValue(event, "http_method"));
    assertEquals("/assessments", keyValue(event, "path"));
    assertEquals("Request input rejected", event.getFormattedMessage());
    assertEquals(Level.WARN, event.getLevel());
  }

  private static Object keyValue(ILoggingEvent event, String key) {
    return event.getKeyValuePairs().stream()
        .filter(kv -> kv.key.equals(key))
        .findFirst()
        .map(kv -> kv.value)
        .orElse(null);
  }
}
