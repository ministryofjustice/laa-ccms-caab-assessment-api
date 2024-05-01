package uk.gov.laa.ccms.caab.assessment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
public interface IntegrationTestInterface {

  OracleContainerSingleton oracleContainerSingleton = OracleContainerSingleton.getInstance();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", oracleContainerSingleton.getOracleContainer()::getJdbcUrl);
    registry.add("spring.datasource.username", oracleContainerSingleton.getOracleContainer()::getUsername);
    registry.add("spring.datasource.password", oracleContainerSingleton.getOracleContainer()::getPassword);
  }
}

