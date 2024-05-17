package uk.gov.laa.ccms.caab.assessment.audit;


import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

/**
 * Auditor provider implementation for the application. This class is responsible for storing the
 * current user in a thread local variable, and returning it when requested.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

  public static final ThreadLocal<String> currentUserHolder = new ThreadLocal<String>();

  @Override
  public Optional getCurrentAuditor() {
    return Optional.of(currentUserHolder.get());
  }

}
