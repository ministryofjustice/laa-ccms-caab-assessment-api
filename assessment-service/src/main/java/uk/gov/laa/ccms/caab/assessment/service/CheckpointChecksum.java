package uk.gov.laa.ccms.caab.assessment.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Checksum identifying the content of an assessment checkpoint. */
public final class CheckpointChecksum {

  private CheckpointChecksum() {}

  /**
   * Returns the SHA-256 of the checkpoint's interview data as lower-case hex.
   *
   * @param interviewData the stored interview data
   * @return the checksum
   */
  public static String of(final byte[] interviewData) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(interviewData));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
