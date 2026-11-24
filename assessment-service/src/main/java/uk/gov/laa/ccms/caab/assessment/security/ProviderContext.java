package uk.gov.laa.ccms.caab.assessment.security;

import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;

/** Holds the validated caller provider identifier for the current request thread. */
public final class ProviderContext {

  public static final String HEADER_NAME = "Caab-Provider-Id";

  private static final Pattern NUMERIC_PROVIDER_ID = Pattern.compile("^\\d+$");

  private static final ThreadLocal<String> CURRENT_PROVIDER_ID = new ThreadLocal<>();

  private ProviderContext() {}

  /**
   * Checks whether a candidate provider identifier consists solely of numeric digits after
   * trimming.
   *
   * @param value the candidate header or provider identifier value
   * @return {@code true} if non-null and matching {@code ^\d+$} after trimming
   */
  public static boolean isValidProviderId(final String value) {
    return value != null && NUMERIC_PROVIDER_ID.matcher(value.trim()).matches();
  }

  /**
   * Stores the validated, trimmed provider identifier for the current request thread.
   *
   * @param providerId the provider identifier to store
   * @throws ApplicationException if the value is null, blank, or non-numeric
   */
  public static void setProviderId(final String providerId) {
    if (!isValidProviderId(providerId)) {
      throw new ApplicationException(
          "Missing or invalid required header: " + HEADER_NAME, HttpStatus.BAD_REQUEST);
    }
    CURRENT_PROVIDER_ID.set(providerId.trim());
  }

  /**
   * Returns the current request's provider identifier, or {@code null} if none is set.
   *
   * @return the current provider identifier or {@code null}
   */
  public static String getProviderId() {
    return CURRENT_PROVIDER_ID.get();
  }

  /**
   * Returns the current request's validated provider identifier, throwing a 400 Bad Request {@link
   * ApplicationException} if absent or invalid.
   *
   * @return the validated provider identifier
   * @throws ApplicationException if no valid provider identifier is present
   */
  public static String getRequiredProviderId() {
    String providerId = CURRENT_PROVIDER_ID.get();
    if (!isValidProviderId(providerId)) {
      throw new ApplicationException(
          "Missing required header: " + HEADER_NAME, HttpStatus.BAD_REQUEST);
    }
    return providerId;
  }

  /** Clears the provider identifier bound to the current thread. */
  public static void clear() {
    CURRENT_PROVIDER_ID.remove();
  }
}
