package uk.gov.laa.ccms.caab.assessment.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

class AuditUserCleanupConfigurationTest {

  private final AuditUserCleanupConfiguration configuration = new AuditUserCleanupConfiguration();

  @Test
  void theCleanupIsRegisteredForRequestsAndErrorDispatches() throws Exception {
    var servletContext = mock(ServletContext.class);
    var dynamic = mock(FilterRegistration.Dynamic.class);
    when(servletContext.addFilter(anyString(), any(Filter.class))).thenReturn(dynamic);

    configuration
        .auditUserCleanupFilterRegistration(new AuditUserCleanupFilter())
        .onStartup(servletContext);

    EnumSet<DispatcherType> dispatchTypes =
        mockingDetails(dynamic).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("addMappingForUrlPatterns"))
            .map(i -> (EnumSet<DispatcherType>) i.getArgument(0))
            .findFirst()
            .orElseThrow();
    assertTrue(dispatchTypes.contains(DispatcherType.REQUEST));
    assertTrue(dispatchTypes.contains(DispatcherType.ERROR));
  }

  @Test
  void theCleanupStaysTheOutermostFilter() {
    FilterRegistrationBean<AuditUserCleanupFilter> registration =
        configuration.auditUserCleanupFilterRegistration(new AuditUserCleanupFilter());

    assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
  }
}
