package uk.gov.laa.ccms.caab.assessment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
public abstract class OracleContainerIntegrationTest {

  @Container
  @ServiceConnection
  static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
      .withUsername("XXCCMS_PUI")
      .withPassword("XXCCMS_PUI")
      .withReuse(true);
}

