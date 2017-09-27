package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplyInterestPaymentBuilderServiceTest {
  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilderServiceTestCase testCase = new PaymentBuilderServiceTestCase("simple case");
    testCase.runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, testCase.balance.negate());

    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        ApplyInterestPaymentBuilderService::new, testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.APPLY_INTEREST, Collections.emptySet(), testCase.forDate);
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(BigDecimal.valueOf(27, 2), mappedCostComponents.get(ChargeIdentifiers.INTEREST_ID).getAmount());
  }
}
