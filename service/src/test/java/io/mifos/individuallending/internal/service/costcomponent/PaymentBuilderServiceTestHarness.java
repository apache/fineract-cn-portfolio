package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.DefaultChargeDefinitionsMocker;
import io.mifos.individuallending.internal.service.schedule.ScheduledChargesService;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentRepository;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import org.mockito.Mockito;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.function.Function;

class PaymentBuilderServiceTestHarness {
  static PaymentBuilder constructCallToPaymentBuilder (
      final Function<ScheduledChargesService, PaymentBuilderService> serviceFactory,
      final PaymentBuilderServiceTestCase testCase) {
    final BalanceSegmentRepository balanceSegmentRepository = Mockito.mock(BalanceSegmentRepository.class);
    final ChargeDefinitionService chargeDefinitionService = DefaultChargeDefinitionsMocker.getChargeDefinitionService(Collections.emptyList());
    final ScheduledChargesService scheduledChargesService = new ScheduledChargesService(chargeDefinitionService, balanceSegmentRepository);
    final PaymentBuilderService testSubject = serviceFactory.apply(scheduledChargesService);

    final ProductEntity product = new ProductEntity();
    product.setIdentifier("blah");
    product.setMinorCurrencyUnitDigits(2);
    final CaseEntity customerCase = new CaseEntity();
    customerCase.setEndOfTerm(testCase.endOfTerm);
    customerCase.setInterest(testCase.interestRate);
    final CaseParametersEntity caseParameters = new CaseParametersEntity();
    caseParameters.setPaymentSize(testCase.paymentSize);
    caseParameters.setBalanceRangeMaximum(testCase.balanceRangeMaximum);
    caseParameters.setPaymentCyclePeriod(1);
    caseParameters.setPaymentCycleTemporalUnit(ChronoUnit.MONTHS);
    caseParameters.setCreditWorthinessFactors(Collections.emptySet());

    final DataContextOfAction dataContextOfAction = new DataContextOfAction(
        product,
        customerCase,
        caseParameters,
        Collections.emptyList());
    return testSubject.getPaymentBuilder(
        dataContextOfAction,
        testCase.paymentSize,
        testCase.forDate,
        testCase.runningBalances);
  }
}