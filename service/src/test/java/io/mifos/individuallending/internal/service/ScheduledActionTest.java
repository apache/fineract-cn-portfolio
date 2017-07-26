package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.workflow.Action;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

public class ScheduledActionTest {
  @Test
  public void actionIsOnOrBefore() {
    final LocalDate today = LocalDate.now();
    final LocalDate tomorrow = today.plusDays(1);
    final LocalDate yesterday = today.minusDays(1);
    final ScheduledAction testSubject = new ScheduledAction(Action.APPLY_INTEREST, today);
    Assert.assertFalse(testSubject.actionIsOnOrAfter(today));
    Assert.assertFalse(testSubject.actionIsOnOrAfter(tomorrow));
    Assert.assertTrue(testSubject.actionIsOnOrAfter(yesterday));
  }
}