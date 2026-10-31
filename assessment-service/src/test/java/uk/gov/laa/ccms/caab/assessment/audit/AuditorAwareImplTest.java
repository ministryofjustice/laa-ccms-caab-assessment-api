package uk.gov.laa.ccms.caab.assessment.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuditorAwareImplTest {

  @AfterEach
  void clear() {
    AuditorAwareImpl.currentUserHolder.remove();
  }

  @Test
  void returnsTheCurrentUser() {
    AuditorAwareImpl.currentUserHolder.set("jane");

    assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("jane");
  }

  @Test
  void returnsEmptyWhenNoUserWasSupplied() {
    assertThat(new AuditorAwareImpl().getCurrentAuditor()).isEmpty();
  }
}
