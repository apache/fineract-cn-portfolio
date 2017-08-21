package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR;
import static io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR;

@RunWith(Parameterized.class)
public class CostComponentServiceTest {
  private static class TestCase {
    final String description;
    ChargeProportionalDesignator chargeProportionalDesignator = ChargeProportionalDesignator.NOT_PROPORTIONAL;
    BigDecimal maximumBalance = BigDecimal.ZERO;
    BigDecimal runningBalance = BigDecimal.ZERO;
    BigDecimal loanPaymentSize = BigDecimal.ZERO;
    BigDecimal expectedAmount = BigDecimal.ZERO;

    private TestCase(String description) {
      this.description = description;
    }

    TestCase chargeProportionalDesignator(ChargeProportionalDesignator chargeProportionalDesignator) {
      this.chargeProportionalDesignator = chargeProportionalDesignator;
      return this;
    }

    TestCase maximumBalance(BigDecimal maximumBalance) {
      this.maximumBalance = maximumBalance;
      return this;
    }

    TestCase runningBalance(BigDecimal runningBalance) {
      this.runningBalance = runningBalance;
      return this;
    }

    TestCase loanPaymentSize(BigDecimal loanPaymentSize) {
      this.loanPaymentSize = loanPaymentSize;
      return this;
    }

    TestCase expectedAmount(BigDecimal expectedAmount) {
      this.expectedAmount = expectedAmount;
      return this;
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<CostComponentServiceTest.TestCase> ret = new ArrayList<>();
    ret.add(new TestCase("simple"));
    ret.add(new TestCase("distribution fee")
        .chargeProportionalDesignator(PRINCIPAL_ADJUSTMENT_DESIGNATOR)
        .maximumBalance(BigDecimal.valueOf(2000))
        .loanPaymentSize(BigDecimal.valueOf(-2000))
        .expectedAmount(BigDecimal.valueOf(2000)));
    ret.add(new TestCase("origination fee")
        .chargeProportionalDesignator(RUNNING_BALANCE_DESIGNATOR)
        .runningBalance(BigDecimal.valueOf(5000))
        .expectedAmount(BigDecimal.valueOf(5000)));
    return ret;
  }

  private final CostComponentServiceTest.TestCase testCase;

  public CostComponentServiceTest(final CostComponentServiceTest.TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getAmountProportionalTo() {
    final BigDecimal amount = CostComponentService.getAmountProportionalTo(
        testCase.chargeProportionalDesignator,
        testCase.maximumBalance,
        testCase.runningBalance,
        testCase.loanPaymentSize,
        Collections.emptyMap());

    Assert.assertEquals(testCase.expectedAmount, amount);
  }

}