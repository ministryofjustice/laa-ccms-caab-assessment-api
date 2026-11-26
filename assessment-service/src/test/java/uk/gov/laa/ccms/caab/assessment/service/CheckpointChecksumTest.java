package uk.gov.laa.ccms.caab.assessment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CheckpointChecksumTest {

  @Test
  void isTheLowerCaseHexSha256OfTheInterviewData() {
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        CheckpointChecksum.of("abc".getBytes(StandardCharsets.UTF_8)));
  }
}
