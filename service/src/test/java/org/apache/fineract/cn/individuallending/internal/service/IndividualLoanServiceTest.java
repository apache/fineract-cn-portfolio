/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.individuallending.internal.service;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.ChargeName;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.mapper.CaseParametersMapper;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargesService;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.PaymentCycle;
import org.apache.fineract.cn.portfolio.api.v1.domain.TermRange;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class IndividualLoanServiceTest {

  private static class ActionDatePair {
    final Action action;
    final LocalDate localDate;

    ActionDatePair(final Action action, final LocalDate localDate) {
      this.action = action;
      this.localDate = localDate;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ActionDatePair that = (ActionDatePair) o;
      return action == that.action &&
              Objects.equals(localDate, that.localDate);
    }

    @Override
    public int hashCode() {
      return Objects.hash(action, localDate);
    }

    @Override
    public String toString() {
      return "ActionDatePair{" +
              "action=" + action +
              ", localDate=" + localDate +
              '}';
    }
  }


  private static class TestCase {
    private final String description;
    private String productIdentifier = "blah";
    private int minorCurrencyUnitDigits = 2;
    private CaseParameters caseParameters;
    private LocalDate initialDisbursementDate;
    private List<ChargeDefinition> chargeDefinitions = Collections.emptyList();
    private BigDecimal interest;
    private Set<String> expectedChargeIdentifiers = new HashSet<>(Arrays.asList(
        PROCESSING_FEE_ID,
        LOAN_ORIGINATION_FEE_ID,
        INTEREST_ID,
        DISBURSEMENT_FEE_ID,
        REPAY_PRINCIPAL_ID,
        REPAY_FEES_ID,
        REPAY_INTEREST_ID,
        DISBURSE_PAYMENT_ID,
        LATE_FEE_ID
        ));
    private Map<ActionDatePair, List<ChargeDefinition>> chargeDefinitionsForActions = new HashMap<>();
    //This is an abuse of the ChargeDefinition since everywhere else it's intended to contain account identifiers and not
    //account designators.  Don't copy the code around charge instances in this test without thinking about what you're
    //doing carefully first.

    TestCase(final String description) {
      this.description = description;
    }

    TestCase minorCurrencyUnitDigits(final int newVal) {
      this.minorCurrencyUnitDigits = newVal;
      return this;
    }

    TestCase caseParameters(final CaseParameters newVal) {
      this.caseParameters = newVal;
      return this;
    }

    TestCase initialDisbursementDate(final LocalDate newVal) {
      this.initialDisbursementDate = newVal;
      return this;
    }

    TestCase chargeDefinitions(final List<ChargeDefinition> newVal) {
      this.chargeDefinitions = newVal;
      return this;
    }

    TestCase interest(final BigDecimal newVal) {
      this.interest = newVal;
      return this;
    }

    TestCase expectChargeInstancesForActionDatePair(final Action action,
                                                    final LocalDate forDate,
                                                    final List<ChargeDefinition> chargeDefinitions) {
      this.chargeDefinitionsForActions.put(new ActionDatePair(action, forDate), chargeDefinitions);
      return this;
    }

    DataContextOfAction getDataContextOfAction() {

      final ProductEntity product = new ProductEntity();
      product.setMinorCurrencyUnitDigits(minorCurrencyUnitDigits);
      product.setIdentifier(productIdentifier);
      final CaseEntity customerCase = new CaseEntity();
      customerCase.setInterest(interest);

      return new DataContextOfAction(
          product,
          customerCase,
          CaseParametersMapper.map(1L, caseParameters),
          Collections.emptyList());
    }

    @Override
    public String toString() {
      return "TestCase{" +
              "description='" + description + '\'' +
              '}';
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<TestCase> ret = new ArrayList<>();
    ret.add(simpleCase());
    ret.add(yearLoanTestCase());
    ret.add(chargeDefaultsCase());
    return ret;
  }

  private final TestCase testCase;
  private final IndividualLoanService testSubject;
  private final ScheduledChargesService scheduledChargesService;


  private static TestCase simpleCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.WEEKS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 0, null, null));

    final ChargeDefinition processingFeeCharge = getFixedSingleChargeDefinition(10.0, Action.DISBURSE, PROCESSING_FEE_ID, AccountDesignators.PROCESSING_FEE_INCOME);
    final ChargeDefinition loanOriginationFeeCharge = getFixedSingleChargeDefinition(100.0, Action.DISBURSE, LOAN_ORIGINATION_FEE_ID, AccountDesignators.ORIGINATION_FEE_INCOME);


    return new TestCase("simpleCase")
        .minorCurrencyUnitDigits(2)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .chargeDefinitions(Arrays.asList(processingFeeCharge, loanOriginationFeeCharge))
        .interest(BigDecimal.valueOf(1))
        .expectChargeInstancesForActionDatePair(Action.DISBURSE, initialDisbursementDate, Arrays.asList(processingFeeCharge, loanOriginationFeeCharge));
  }

  private static TestCase yearLoanTestCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 0, null, null));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(200000));


    return new TestCase("yearLoanTestCase")
        .minorCurrencyUnitDigits(3)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .interest(BigDecimal.valueOf(10));
  }

  private static TestCase chargeDefaultsCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 2, 6);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.MONTHS, 6));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 1, 0, 0));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(2000));


    return new TestCase("chargeDefaultsCase")
        .minorCurrencyUnitDigits(2)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .interest(BigDecimal.valueOf(5));
  }

  private static ChargeDefinition getFixedSingleChargeDefinition(
          final double amount,
          final Action action,
          final String chargeIdentifier,
          final String feeAccountDesignator) {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setAmount(BigDecimal.valueOf(amount));
    ret.setIdentifier(chargeIdentifier);
    ret.setAccrueAction(null);
    ret.setChargeAction(action.name());
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    ret.setProportionalTo(null);
    ret.setFromAccountDesignator(AccountDesignators.CUSTOMER_LOAN_FEES);
    ret.setToAccountDesignator(feeAccountDesignator);
    ret.setForCycleSizeUnit(null);
    return ret;
  }

  public IndividualLoanServiceTest(final TestCase testCase)
  {
    this.testCase = testCase;

    final BalanceSegmentRepository balanceSegmentRepositoryMock = Mockito.mock(BalanceSegmentRepository.class);
    Mockito.doReturn(Stream.empty()).when(balanceSegmentRepositoryMock).findByProductIdentifierAndSegmentSetIdentifier(Matchers.anyString(), Matchers.anyString());

    scheduledChargesService = new ScheduledChargesService(DefaultChargeDefinitionsMocker.getChargeDefinitionService(testCase.chargeDefinitions), balanceSegmentRepositoryMock);

    testSubject = new IndividualLoanService(scheduledChargesService);
  }

  @Test
  public void getPlannedPayments() throws Exception {
    final PlannedPaymentPage firstPage = testSubject.getPlannedPaymentsPage(testCase.getDataContextOfAction(),
            new IndividualLoanService.PlannedPaymentWindow(0,
            20,
            Optional.of(testCase.initialDisbursementDate)));

    Assert.assertFalse(firstPage.getElements().size() == 0);

    final List<PlannedPayment> allPlannedPayments =
        Stream.iterate(0, x -> x + 1).limit(firstPage.getTotalPages())
            .map(x -> testSubject.getPlannedPaymentsPage(testCase.getDataContextOfAction(),
                    new IndividualLoanService.PlannedPaymentWindow(x,
                20,
                Optional.of(testCase.initialDisbursementDate))))
            .flatMap(x -> x.getElements().stream())
            .collect(Collectors.toList());

    //Remaining principal should correspond with the other cost components.
    final Set<BigDecimal> customerRepayments = Stream.iterate(1, x -> x + 1).limit(allPlannedPayments.size() - 1)
        .map(x ->
        {
          final BigDecimal valueOfRepayPrincipalCostComponent = allPlannedPayments.get(x).getPayment().getCostComponents().stream()
              .filter(costComponent -> costComponent.getChargeIdentifier().equals(ChargeIdentifiers.REPAY_PRINCIPAL_ID))
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
          final BigDecimal valueOfRepayFeeCostComponent = allPlannedPayments.get(x).getPayment().getCostComponents().stream()
              .filter(costComponent -> costComponent.getChargeIdentifier().equals(ChargeIdentifiers.REPAY_FEES_ID))
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
          final BigDecimal valueOfRepayInterestCostComponent = allPlannedPayments.get(x).getPayment().getCostComponents().stream()
              .filter(costComponent -> costComponent.getChargeIdentifier().equals(ChargeIdentifiers.REPAY_INTEREST_ID))
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
          final BigDecimal valueOfInterestCostComponent = allPlannedPayments.get(x).getPayment().getCostComponents().stream()
              .filter(costComponent -> costComponent.getChargeIdentifier().equals(ChargeIdentifiers.INTEREST_ID))
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);

          final BigDecimal interestAccrualBalance = allPlannedPayments.get(x).getPayment().getBalanceAdjustments().getOrDefault(AccountDesignators.INTEREST_ACCRUAL, BigDecimal.ZERO);
          final BigDecimal lateFeeAccrualBalance = allPlannedPayments.get(x).getPayment().getBalanceAdjustments().getOrDefault(AccountDesignators.LATE_FEE_ACCRUAL, BigDecimal.ZERO);
          final BigDecimal principalDifference =
              getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, x - 1)
                  .subtract(
                      getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, x));
          Assert.assertEquals(valueOfRepayInterestCostComponent, valueOfInterestCostComponent);
          Assert.assertEquals(BigDecimal.ZERO, interestAccrualBalance);
          Assert.assertEquals(BigDecimal.ZERO, lateFeeAccrualBalance);
          Assert.assertEquals("Checking payment " + x, valueOfRepayPrincipalCostComponent, principalDifference);
          Assert.assertNotEquals("Remaining principle should always be positive or zero.",
              getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, x).signum(), -1);
          final boolean containsLateFee = allPlannedPayments.get(x).getPayment().getCostComponents().stream().anyMatch(y -> y.getChargeIdentifier().equals(LATE_FEE_ID));
          Assert.assertFalse("Late fee should not be included in planned payments", containsLateFee);
          return valueOfRepayPrincipalCostComponent.add(valueOfRepayInterestCostComponent).add(valueOfRepayFeeCostComponent);
        }
    ).collect(Collectors.toSet());

    //All entries should have the correct scale.
    allPlannedPayments.forEach(x -> {
      x.getPayment().getCostComponents().forEach(y -> Assert.assertEquals(testCase.minorCurrencyUnitDigits, y.getAmount().scale()));
      Assert.assertEquals(testCase.minorCurrencyUnitDigits, x.getBalances().get(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL).scale());
      final int uniqueChargeIdentifierCount = x.getPayment().getCostComponents().stream()
          .map(CostComponent::getChargeIdentifier)
          .collect(Collectors.toSet())
          .size();
      Assert.assertEquals("There should be only one cost component per charge per planned payment.",
          x.getPayment().getCostComponents().size(), uniqueChargeIdentifierCount);
    });

    Assert.assertEquals("Final principal balance should be zero.",
        BigDecimal.ZERO.setScale(testCase.minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN),
        getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, allPlannedPayments.size() - 1));

    Assert.assertEquals("Final interest balance should be zero.",
        BigDecimal.ZERO.setScale(testCase.minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN),
        getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_INTEREST, allPlannedPayments.size() - 1));

    Assert.assertEquals("Final fees balance should be zero.",
        BigDecimal.ZERO.setScale(testCase.minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN),
        getBalanceForPayment(allPlannedPayments, AccountDesignators.CUSTOMER_LOAN_FEES, allPlannedPayments.size() - 1));

    //All customer payments should be within one percent of each other.
    final Optional<BigDecimal> maxPayment = customerRepayments.stream().max(BigDecimal::compareTo);
    final Optional<BigDecimal> minPayment = customerRepayments.stream().min(BigDecimal::compareTo);
    Assert.assertTrue(maxPayment.isPresent());
    Assert.assertTrue(minPayment.isPresent());
    final double percentDifference = percentDifference(maxPayment.get(), minPayment.get());
    Assert.assertTrue("Percent difference = " + percentDifference + ", max = " + maxPayment.get() + ", min = " + minPayment.get(),
        percentDifference < 0.01);

    //All charge identifiers should be associated with a name on the returned page.
    final Set<String> resultChargeIdentifiers = firstPage.getChargeNames().stream()
            .map(ChargeName::getIdentifier)
            .collect(Collectors.toSet());

    Assert.assertEquals(testCase.expectedChargeIdentifiers, resultChargeIdentifiers);
  }

  private BigDecimal getBalanceForPayment(
      final List<PlannedPayment> allPlannedPayments,
      final String accountDesignator,
      int index) {
    return allPlannedPayments.get(index).getBalances().get(accountDesignator);
  }

  @Test
  public void getScheduledCharges() {
    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(testCase.initialDisbursementDate, testCase.caseParameters);
    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(testCase.productIdentifier,
        scheduledActions);

    final List<LocalDate> interestCalculationDates = scheduledCharges.stream()
        .filter(scheduledCharge -> scheduledCharge.getScheduledAction().getAction() == Action.APPLY_INTEREST)
        .map(scheduledCharge -> scheduledCharge.getScheduledAction().getWhen())
        .collect(Collectors.toList());

    final List<LocalDate> allTheDaysAfterTheInitialDisbursementDate
        = Stream.iterate(testCase.initialDisbursementDate.plusDays(1), interestDay -> interestDay.plusDays(1))
        .limit(interestCalculationDates.size())
        .collect(Collectors.toList());

    Assert.assertEquals(interestCalculationDates, allTheDaysAfterTheInitialDisbursementDate);

    /*final List<LocalDate> acceptPaymentDates = scheduledCharges.stream()
        .filter(scheduledCharge -> scheduledCharge.getScheduledAction().getAction() == Action.ACCEPT_PAYMENT)
        .map(scheduledCharge -> scheduledCharge.getScheduledAction().getWhen())
        .collect(Collectors.toList());
    final long expectedAcceptPayments = scheduledActions.stream()
        .filter(x -> x.getAction() == Action.ACCEPT_PAYMENT).count();
    final List<ChargeDefinition> chargeDefinitionsMappedToAcceptPayment = chargeDefinitionsByChargeAction.get(Action.ACCEPT_PAYMENT.name());
    final int numberOfChangeDefinitionsMappedToAcceptPayment = chargeDefinitionsMappedToAcceptPayment == null ? 0 : chargeDefinitionsMappedToAcceptPayment.size();
    Assert.assertEquals("check for correct number of scheduled charges for accept payment",
        expectedAcceptPayments*numberOfChangeDefinitionsMappedToAcceptPayment,
        acceptPaymentDates.size());*/

    final Map<ActionDatePair, Set<ChargeDefinition>> searchableScheduledCharges = scheduledCharges.stream()
        .collect(
            Collectors.groupingBy(scheduledCharge ->
                new ActionDatePair(scheduledCharge.getScheduledAction().getAction(), scheduledCharge.getScheduledAction().getWhen()),
                Collectors.mapping(ScheduledCharge::getChargeDefinition, Collectors.toSet())));

    testCase.chargeDefinitionsForActions.forEach((key, value) ->
        value.forEach(x -> Assert.assertTrue(searchableScheduledCharges.get(key).contains(x))));
  }

  private double percentDifference(final BigDecimal maxPayment, final BigDecimal minPayment) {
    final BigDecimal difference = maxPayment.subtract(minPayment);
    final BigDecimal percentDifference = difference.divide(maxPayment, 4, BigDecimal.ROUND_UP);
    return percentDifference.doubleValue();
  }
}
