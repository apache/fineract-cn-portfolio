package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.DefaultChargeDefinitionsMocker;
import io.mifos.individuallending.internal.service.schedule.ScheduledChargesService;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Payment;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentRepository;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class AcceptPaymentBuilderServiceTest {
  @Test
  public void getPaymentBuilder() throws Exception {
    final LocalDate startOfTerm = LocalDate.of(2015, 1, 15);
    final LocalDateTime endOfTerm = LocalDate.of(2015, 8, 15).atStartOfDay();
    final LocalDate forDate = startOfTerm.plusMonths(1);
    final BigDecimal paymentSize = BigDecimal.valueOf(100_00, 2);
    final BigDecimal balance = BigDecimal.valueOf(2000_00, 2);
    final BigDecimal balanceRangeMaximum = BigDecimal.valueOf(1000_00, 2);
    final BigDecimal accruedInterest = BigDecimal.valueOf(10_00, 2);

    final BalanceSegmentRepository balanceSegmentRepository = Mockito.mock(BalanceSegmentRepository.class);
    final ChargeDefinitionService chargeDefinitionService = DefaultChargeDefinitionsMocker.getChargeDefinitionService(Collections.emptyList());
    final ScheduledChargesService scheduledChargesService = new ScheduledChargesService(chargeDefinitionService, balanceSegmentRepository);
    final AccountingAdapter accountingAdapter = Mockito.mock(AccountingAdapter.class);
    final AcceptPaymentBuilderService testSubject = new AcceptPaymentBuilderService(
        scheduledChargesService,
        accountingAdapter);
    final SimulatedRunningBalances runningBalances  = new SimulatedRunningBalances(startOfTerm);
    runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, balance.negate());
    runningBalances.adjustBalance(AccountDesignators.INTEREST_ACCRUAL, accruedInterest);


    final ProductEntity product = new ProductEntity();
    product.setIdentifier("blah");
    product.setMinorCurrencyUnitDigits(2);
    final CaseEntity customerCase = new CaseEntity();
    customerCase.setEndOfTerm(endOfTerm);
    final CaseParametersEntity caseParameters = new CaseParametersEntity();
    caseParameters.setPaymentSize(paymentSize);
    caseParameters.setBalanceRangeMaximum(balanceRangeMaximum);
    caseParameters.setPaymentCyclePeriod(1);
    caseParameters.setPaymentCycleTemporalUnit(ChronoUnit.MONTHS);
    caseParameters.setCreditWorthinessFactors(Collections.emptySet());

    final DataContextOfAction dataContextOfAction = new DataContextOfAction(product, customerCase, caseParameters, Collections.emptyList());
    final PaymentBuilder paymentBuilder = testSubject.getPaymentBuilderHelper(
        dataContextOfAction,
        paymentSize,
        forDate,
        runningBalances);
    final Payment payment = paymentBuilder.buildPayment(Action.ACCEPT_PAYMENT, Collections.emptySet());
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(accruedInterest, mappedCostComponents.get(ChargeIdentifiers.INTEREST_ID).getAmount());
    Assert.assertEquals(accruedInterest, mappedCostComponents.get(ChargeIdentifiers.REPAY_INTEREST_ID).getAmount());
    Assert.assertEquals(paymentSize.subtract(accruedInterest), mappedCostComponents.get(ChargeIdentifiers.REPAY_PRINCIPAL_ID).getAmount());
  }
}