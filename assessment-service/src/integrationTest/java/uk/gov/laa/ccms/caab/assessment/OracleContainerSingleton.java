package uk.gov.laa.ccms.caab.assessment;

import org.testcontainers.containers.OracleContainer;

public class OracleContainerSingleton {

  private static OracleContainerSingleton instance;
  private final OracleContainer oracleContainer;

  private OracleContainerSingleton() {
    oracleContainer = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart");
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

