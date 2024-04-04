package uk.gov.laa.ccms.caab.assessment.advice;

import static uk.gov.laa.ccms.caab.assessment.audit.AuditorAwareImpl.currentUserHolder;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Controller advice class responsible for setting the current user holder if available.
 */
@ControllerAdvice
public class AuditAdvice {

  /**
   * Sets the current user holder if available from the request header.
   *
   * @param caabUserLoginId the caab user login id
   */
  @ModelAttribute
  public void setCurrentUserHolderIfAvailable(
      @RequestHeader(value = "Caab-User-Login-Id", required = false) String caabUserLoginId) {
    if (caabUserLoginId != null) {
      currentUserHolder.set(caabUserLoginId);
    }
  }
}
