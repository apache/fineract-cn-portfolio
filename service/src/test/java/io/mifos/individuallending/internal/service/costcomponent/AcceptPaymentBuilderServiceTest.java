package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class AcceptPaymentBuilderServiceTest {
  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilderServiceTestCase testCase = new PaymentBuilderServiceTestCase("simple case");
    testCase.runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, testCase.balance.negate());
    testCase.runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_INTEREST, testCase.accruedInterest.negate());
    testCase.runningBalances.adjustBalance(AccountDesignators.INTEREST_ACCRUAL, testCase.accruedInterest);

    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        AcceptPaymentBuilderService::new, testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.ACCEPT_PAYMENT, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(testCase.accruedInterest, mappedCostComponents.get(ChargeIdentifiers.INTEREST_ID).getAmount());
    Assert.assertEquals(testCase.accruedInterest, mappedCostComponents.get(ChargeIdentifiers.REPAY_INTEREST_ID).getAmount());
    Assert.assertEquals(testCase.paymentSize.subtract(testCase.accruedInterest), mappedCostComponents.get(ChargeIdentifiers.REPAY_PRINCIPAL_ID).getAmount());
  }
}