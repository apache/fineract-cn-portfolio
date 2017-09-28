package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

public class ApplyInterestPaymentBuilderServiceTest {
  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilderServiceTestCase testCase = new PaymentBuilderServiceTestCase("simple case");
    testCase.runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, testCase.balance.negate());

    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        ApplyInterestPaymentBuilderService::new, testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.APPLY_INTEREST, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);

    Assert.assertEquals(BigDecimal.valueOf(27, 2), paymentBuilder.getBalanceAdjustments().get(AccountDesignators.INTEREST_ACCRUAL));
  }
}
