package uk.gov.laa.ccms.caab.assessment.audit;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers the audit user cleanup for requests and for the error page dispatch. */
@Configuration(proxyBeanMethods = false)
public class AuditUserCleanupConfiguration {

  @Bean
  FilterRegistrationBean<AuditUserCleanupFilter> auditUserCleanupFilterRegistration(
      AuditUserCleanupFilter filter) {
    FilterRegistrationBean<AuditUserCleanupFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setName("auditUserCleanupFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
    return registration;
  }
}
