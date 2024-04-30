package uk.gov.laa.ccms.caab.assessment.controller;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import uk.gov.laa.ccms.caab.assessment.AssessmentApplication;

@SpringBootTest(classes = AssessmentApplication.class)
@SqlMergeMode(MERGE)
@ActiveProfiles("local")
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = "/sql/delete_data.sql")
@Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "/sql/delete_data.sql")
public class AssessmentControllerIntegrationLocalTest
    extends BaseAssessmentControllerIntegrationTest{

  //this runs all tests in BaseAssessmentControllerIntegrationTest, do not add anything here
  //this is an easy way to run the tests if you have the containerised database running locally already

}
