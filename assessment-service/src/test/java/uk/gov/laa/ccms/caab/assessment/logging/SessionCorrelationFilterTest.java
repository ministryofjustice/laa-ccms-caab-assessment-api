package uk.gov.laa.ccms.caab.assessment.logging;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class SessionCorrelationFilterTest {
  @Test
  void statelessRequestHasCorrelationWithoutCreatingSessionAndCleansUpOnFailure() {
    var request = new MockHttpServletRequest();
    var filter = new SessionCorrelationFilter();
    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                  assertNotNull(MDC.get("sessionID"));
                  assertNull(request.getSession(false));
                  throw new ServletException("synthetic failure");
                }));
    assertNull(MDC.get("sessionID"));
  }

  @Test
  void reusesOpaqueSessionCorrelationAndRestoresPreviousContext() throws Exception {
    var session = new MockHttpSession();
    String[] ids = new String[2];
    MDC.put("sessionID", "outer-context");
    try {
      for (int i = 0; i < 2; i++) {
        int index = i;
        var request = new MockHttpServletRequest();
        request.setSession(session);
        new SessionCorrelationFilter()
            .doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> ids[index] = MDC.get("sessionID"));
        assertEquals("outer-context", MDC.get("sessionID"));
      }
      assertEquals(ids[0], ids[1]);
      assertNotEquals(session.getId(), ids[0]);
    } finally {
      MDC.remove("sessionID");
    }
  }

  @Test
  void reusesRequestCorrelationOnAsyncRedispatchAndCleansEachThreadScope() throws Exception {
    var request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var response = new MockHttpServletResponse();
    var filter = new SessionCorrelationFilter();
    String[] ids = new String[2];
    filter.doFilter(
        request,
        response,
        (req, res) -> {
          ids[0] = MDC.get("sessionID");
          request.startAsync();
        });
    assertNull(MDC.get("sessionID"));
    request.setDispatcherType(DispatcherType.ASYNC);
    filter.doFilter(request, response, (req, res) -> ids[1] = MDC.get("sessionID"));
    assertNotNull(ids[0]);
    assertEquals(ids[0], ids[1]);
    assertNull(MDC.get("sessionID"));
    assertNull(request.getSession(false));
  }

  @Test
  void setsFreshRequestIdInMdcAndResponseHeaderIgnoringInboundHeader() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "attacker-supplied");
    var response = new MockHttpServletResponse();
    String[] seen = new String[1];
    new SessionCorrelationFilter()
        .doFilter(request, response, (req, res) -> seen[0] = MDC.get("request_id"));

    assertNotNull(seen[0]);
    assertNotEquals("attacker-supplied", seen[0]);
    assertEquals(seen[0], response.getHeader("X-Request-Id"));
    assertNull(MDC.get("request_id"));
  }

  @Test
  void requestIdIsReusedOnAsyncRedispatchAndPreviousValueRestoredOnFailure() throws Exception {
    var request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var response = new MockHttpServletResponse();
    var filter = new SessionCorrelationFilter();
    String[] ids = new String[2];
    filter.doFilter(
        request,
        response,
        (req, res) -> {
          ids[0] = MDC.get("request_id");
          request.startAsync();
        });
    request.setDispatcherType(DispatcherType.ASYNC);
    filter.doFilter(request, response, (req, res) -> ids[1] = MDC.get("request_id"));
    assertEquals(ids[0], ids[1]);

    MDC.put("request_id", "outer");
    try {
      assertThrows(
          ServletException.class,
          () ->
              filter.doFilter(
                  new MockHttpServletRequest(),
                  new MockHttpServletResponse(),
                  (req, res) -> {
                    throw new ServletException("synthetic failure");
                  }));
      assertEquals("outer", MDC.get("request_id"));
    } finally {
      MDC.remove("request_id");
    }
  }

  @Test
  void logsThePeerAddressAndTheRightMostForwardedAddress() throws Exception {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("10.1.2.3");
    request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
    String[] seen = new String[2];

    new SessionCorrelationFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> {
              seen[0] = MDC.get("source.ip");
              seen[1] = MDC.get("network.forwarded_ip");
            });

    assertEquals("10.1.2.3", seen[0]);
    assertEquals("203.0.113.9", seen[1]);
    assertNull(MDC.get("source.ip"));
    assertNull(MDC.get("network.forwarded_ip"));
  }

  @Test
  void ignoresAForgedLeadingForwardedEntry() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
    String[] seen = new String[1];

    new SessionCorrelationFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> seen[0] = MDC.get("network.forwarded_ip"));

    assertEquals("203.0.113.9", seen[0]);
  }

  @Test
  void restoresAddressesAlreadyInTheLoggingContext() throws Exception {
    MDC.put("source.ip", "outer");
    try {
      var request = new MockHttpServletRequest();
      request.setRemoteAddr("10.1.2.3");
      new SessionCorrelationFilter()
          .doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
      assertEquals("outer", MDC.get("source.ip"));
    } finally {
      MDC.remove("source.ip");
    }
  }

  @Test
  void neverLogsAForwardedHeaderThatIsNotAnIpAddress() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "1.2.3.4, <script>alert(1)</script>");
    String[] seen = new String[1];

    new SessionCorrelationFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> seen[0] = MDC.get("network.forwarded_ip"));

    assertNull(seen[0]);
  }
}
