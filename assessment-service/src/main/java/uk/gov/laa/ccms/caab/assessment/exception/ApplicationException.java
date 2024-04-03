package uk.gov.laa.ccms.caab.assessment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The exception thrown for specific application exceptions.
 */
@Getter
public class ApplicationException extends RuntimeException {

  private final String errorMessage;

  private final HttpStatus httpStatus;

  /**
   * The exception thrown for specific application exceptions.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *                {@link #getMessage()} method.
   */
  public ApplicationException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorMessage = message;
  }
}
