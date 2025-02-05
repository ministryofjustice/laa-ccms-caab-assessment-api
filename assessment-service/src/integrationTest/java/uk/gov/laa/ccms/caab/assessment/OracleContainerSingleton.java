package uk.gov.laa.ccms.caab.assessment;

import org.testcontainers.oracle.OracleContainer;

public class OracleContainerSingleton {

  private static OracleContainerSingleton instance;
  private final OracleContainer oracleContainer;

  private OracleContainerSingleton() {
    oracleContainer = new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
        .withUsername("XXCCMS_PUI");
    oracleContainer.start();
  }

  public static synchronized OracleContainerSingleton getInstance() {
    if (instance == null) {
      instance = new OracleContainerSingleton();
    }
    return instance;
  }

  public OracleContainer getOracleContainer() {
    return oracleContainer;
  }
}

