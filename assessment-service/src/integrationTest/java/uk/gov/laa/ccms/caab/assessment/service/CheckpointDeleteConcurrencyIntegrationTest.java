package uk.gov.laa.ccms.caab.assessment.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.laa.ccms.caab.assessment.OracleContainerIntegrationTest;
import uk.gov.laa.ccms.caab.assessment.exception.ApplicationException;

/** Overlaps a checkpoint rewrite with a conditional delete in separate Oracle transactions. */
@Sql(executionPhase = BEFORE_TEST_CLASS, scripts = "/sql/assessment_tables_create_schema.sql")
@Sql(executionPhase = AFTER_TEST_CLASS, scripts = "/sql/assessment_tables_drop_schema.sql")
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = "/sql/delete_data.sql")
@SqlMergeMode(MergeMode.MERGE)
class CheckpointDeleteConcurrencyIntegrationTest extends OracleContainerIntegrationTest {

  private static final long ASSESSMENT_ID = 26L;

  // A delete still running this long after it started is waiting on the rewrite's row lock.
  private static final long BLOCKED_DELETE_SECONDS = 3;
  private static final long BOUNDED_WAIT_SECONDS = 30;

  @Autowired private AssessmentService assessmentService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  @Sql(scripts = "/sql/assessments_insert.sql")
  void conditionalDeleteWaitsForAnUncommittedRewriteAndThenKeepsIt() throws Exception {
    final String checksumAsRead = CheckpointChecksum.of(storedInterviewData());
    final byte[] rewritten = "newer interview".getBytes(StandardCharsets.UTF_8);

    final CountDownLatch rewriteHeld = new CountDownLatch(1);
    final CountDownLatch commitRewrite = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      final Future<?> rewrite =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          status -> {
                            jdbcTemplate.update(
                                "UPDATE XXCCMS_PUI.XXCCMS_OPA_CHECKPOINT"
                                    + " SET INTERVIEW_DATA = ? WHERE RESUME_ID = ?",
                                rewritten,
                                ASSESSMENT_ID);
                            rewriteHeld.countDown();
                            awaitOrFail(commitRewrite);
                          }));
      assertTrue(
          rewriteHeld.await(BOUNDED_WAIT_SECONDS, SECONDS), "the rewrite never took its lock");

      final Future<?> delete =
          executor.submit(() -> assessmentService.deleteCheckpoint(ASSESSMENT_ID, checksumAsRead));

      // The delete must wait for the rewrite rather than read the old committed checkpoint.
      assertThrows(TimeoutException.class, () -> delete.get(BLOCKED_DELETE_SECONDS, SECONDS));

      commitRewrite.countDown();
      rewrite.get(BOUNDED_WAIT_SECONDS, SECONDS);

      final ExecutionException failure =
          assertThrows(ExecutionException.class, () -> delete.get(BOUNDED_WAIT_SECONDS, SECONDS));
      final ApplicationException applicationException =
          assertInstanceOf(ApplicationException.class, failure.getCause());
      assertEquals(HttpStatus.PRECONDITION_FAILED, applicationException.getHttpStatus());
      assertArrayEquals(rewritten, storedInterviewData());
    } finally {
      commitRewrite.countDown();
      executor.shutdownNow();
    }
  }

  private byte[] storedInterviewData() {
    return jdbcTemplate.queryForObject(
        "SELECT INTERVIEW_DATA FROM XXCCMS_PUI.XXCCMS_OPA_CHECKPOINT WHERE RESUME_ID = ?",
        (resultSet, rowNumber) -> resultSet.getBytes(1),
        ASSESSMENT_ID);
  }

  private static void awaitOrFail(final CountDownLatch latch) {
    try {
      if (!latch.await(BOUNDED_WAIT_SECONDS, SECONDS)) {
        throw new IllegalStateException("timed out waiting to commit the rewrite");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
