package uk.gov.laa.ccms.caab.assessment.advice;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;

/**
 * Controller advice class responsible for handling exceptions globally and providing appropriate
 * error responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles the {@link ApplicationException} by logging the error and returning an
   * appropriate error response.
   *
   * @param e the ApplicationException
   * @return the response entity with the status code and error response body.
   */
  @ExceptionHandler(value = {ApplicationException.class})
  public ResponseEntity<Object> handleApplicationException(
      final ApplicationException e) {

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("error_message", e.getErrorMessage());
    responseBody.put("http_status", e.getHttpStatus().value());

    return ResponseEntity.status(e.getHttpStatus()).body(responseBody);
  }
}
