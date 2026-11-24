package uk.gov.laa.ccms.caab.assessment.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers {@link ProviderContextFilter} after Spring Security authentication. */
@Configuration(proxyBeanMethods = false)
public class ProviderContextConfiguration {

  static final int AFTER_SECURITY_FILTER_ORDER = -90;

  @Bean
  FilterRegistrationBean<ProviderContextFilter> providerContextFilterRegistration(
      final ProviderContextFilter filter) {
    FilterRegistrationBean<ProviderContextFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setName("providerContextFilter");
    registration.setOrder(AFTER_SECURITY_FILTER_ORDER);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    return registration;
  }
}
