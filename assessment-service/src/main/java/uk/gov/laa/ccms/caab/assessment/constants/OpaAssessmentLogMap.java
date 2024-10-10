package uk.gov.laa.ccms.caab.assessment.constants;

import java.util.Arrays;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Enum representing the mapping between session assessment types and log assessment types.
 */
@Getter
@Slf4j
public enum OpaAssessmentLogMap {
  MEANS("meansAssessment", "MEANS"),
  MEANS_PREPOP("meansAssessment_PREPOP", "MEANS"),
  MERITS("meritsAssessment", "MERITS"),
  MERITS_PREPOP("meritsAssessment_PREPOP",  "MERITS"),
  BILLING("billingAssessment", "BILL"),
  BILLING_PREPOP("billingAssessment_PREPOP", "BILL"),
  POA("poaAssessment", "POA"),
  POA_PREPOP("poaAssessment_PREPOP", "POA");


  private final String sessionAssessmentType;
  private final String logAssessmentType;

  OpaAssessmentLogMap(final String sessionAssessmentType, final String logAssessmentType) {
    this.sessionAssessmentType = sessionAssessmentType;
    this.logAssessmentType = logAssessmentType;
  }

  /**
   * Finds the log assessment type based on the session assessment type.
   *
   * @param sessionAssessmentType the session assessment type
   * @return the corresponding log assessment type, or null if not found
   */
  public static String findLogAssessmentTypeBySessionAssessmentType(
      final String sessionAssessmentType) {
    return Arrays.stream(OpaAssessmentLogMap.values())
        .filter(assessmentLogMap -> assessmentLogMap.getSessionAssessmentType()
            .equalsIgnoreCase(sessionAssessmentType))
        .map(OpaAssessmentLogMap::getLogAssessmentType)
        .findFirst()
        .orElseGet(() -> {
          log.warn("Invalid session assessment type: {}", sessionAssessmentType);
          return null;
        });
  }


}
