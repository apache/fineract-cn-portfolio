package io.mifos.individuallending.internal.service;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Optional;

public class ChargeRangeTest {
  @Test
  public void amountIsWithinRange() throws Exception {
    final ChargeRange testSubject1 = new ChargeRange(BigDecimal.TEN, Optional.empty());
    Assert.assertFalse(testSubject1.amountIsWithinRange(BigDecimal.ZERO));
    Assert.assertFalse(testSubject1.amountIsWithinRange(BigDecimal.ONE));
    Assert.assertTrue(testSubject1.amountIsWithinRange(BigDecimal.TEN));
    Assert.assertTrue(testSubject1.amountIsWithinRange(BigDecimal.TEN.add(BigDecimal.ONE)));

    final ChargeRange testSubject2 = new ChargeRange(BigDecimal.ZERO, Optional.of(BigDecimal.TEN));
    Assert.assertTrue(testSubject2.amountIsWithinRange(BigDecimal.ZERO));
    Assert.assertTrue(testSubject2.amountIsWithinRange(BigDecimal.ONE));
    Assert.assertFalse(testSubject2.amountIsWithinRange(BigDecimal.TEN));
    Assert.assertFalse(testSubject2.amountIsWithinRange(BigDecimal.TEN.add(BigDecimal.ONE)));
  }

}