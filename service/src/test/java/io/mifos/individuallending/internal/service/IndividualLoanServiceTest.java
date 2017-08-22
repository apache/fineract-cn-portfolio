/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.ChargeName;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentRepository;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
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

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

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
    private List<ChargeDefinition> chargeDefinitions;
    private BigDecimal interest;
    private Set<String> expectedChargeIdentifiers = new HashSet<>(Arrays.asList(
        PROCESSING_FEE_ID,
        LOAN_FUNDS_ALLOCATION_ID,
        RETURN_DISBURSEMENT_ID,
        LOAN_ORIGINATION_FEE_ID,
        INTEREST_ID,
        DISBURSEMENT_FEE_ID,
        REPAYMENT_ID,
        TRACK_DISBURSAL_PAYMENT_ID,
        TRACK_RETURN_PRINCIPAL_ID,
        DISBURSE_PAYMENT_ID
        ));
    private Map<ActionDatePair, List<ChargeDefinition>> chargeDefinitionsForActions = new HashMap<>();
    //This is an abuse of the ChargeInstance since everywhere else it's intended to contain account identifiers and not
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

      return new DataContextOfAction(product, customerCase, CaseParametersMapper.map(1L, caseParameters), Collections.emptyList());
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
  private final Map<String, List<ChargeDefinition>> chargeDefinitionsByChargeAction;
  private final Map<String, List<ChargeDefinition>> chargeDefinitionsByAccrueAction;


  private static TestCase simpleCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.WEEKS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 0, null, null));

    final ChargeDefinition processingFeeCharge = getFixedSingleChargeDefinition(10.0, Action.OPEN, PROCESSING_FEE_ID, AccountDesignators.PROCESSING_FEE_INCOME);
    final ChargeDefinition loanOriginationFeeCharge = getFixedSingleChargeDefinition(100.0, Action.APPROVE, LOAN_ORIGINATION_FEE_ID, AccountDesignators.ORIGINATION_FEE_INCOME);
    final List<ChargeDefinition> defaultChargesWithFeesReplaced =
    charges().stream().map(x -> {
      switch (x.getIdentifier()) {
        case PROCESSING_FEE_ID:
          return processingFeeCharge;
        case LOAN_ORIGINATION_FEE_ID:
          return loanOriginationFeeCharge;
        default:
          return x;
      }
    }).collect(Collectors.toList());

    return new TestCase("simpleCase")
        .minorCurrencyUnitDigits(2)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .chargeDefinitions(defaultChargesWithFeesReplaced)
        .interest(BigDecimal.valueOf(1))
        .expectChargeInstancesForActionDatePair(Action.OPEN, initialDisbursementDate, Collections.singletonList(processingFeeCharge))
        .expectChargeInstancesForActionDatePair(Action.APPROVE, initialDisbursementDate,
            Collections.singletonList(loanOriginationFeeCharge));
  }

  private static TestCase yearLoanTestCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 0, null, null));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(200000));

    final List<ChargeDefinition> charges = charges();

    return new TestCase("yearLoanTestCase")
        .minorCurrencyUnitDigits(3)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .chargeDefinitions(charges)
        .interest(BigDecimal.valueOf(10));
  }

  private static TestCase chargeDefaultsCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 2, 6);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.MONTHS, 6));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 1, 0, 0));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(2000));

    final List<ChargeDefinition> charges = charges();

    return new TestCase("chargeDefaultsCase")
        .minorCurrencyUnitDigits(2)
        .caseParameters(caseParameters)
        .initialDisbursementDate(initialDisbursementDate)
        .chargeDefinitions(charges)
        .interest(BigDecimal.valueOf(5));
  }

  private static List<ChargeDefinition> charges() {
    return IndividualLendingPatternFactory.defaultIndividualLoanCharges();
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
    ret.setFromAccountDesignator(AccountDesignators.ENTRY);
    ret.setToAccountDesignator(feeAccountDesignator);
    ret.setForCycleSizeUnit(null);
    return ret;
  }

  public IndividualLoanServiceTest(final TestCase testCase)
  {
    this.testCase = testCase;

    final ChargeDefinitionService chargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    chargeDefinitionsByChargeAction = testCase.chargeDefinitions.stream()
        .collect(Collectors.groupingBy(ChargeDefinition::getChargeAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    chargeDefinitionsByAccrueAction = testCase.chargeDefinitions.stream()
        .filter(x -> x.getAccrueAction() != null)
        .collect(Collectors.groupingBy(ChargeDefinition::getAccrueAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    Mockito.doReturn(chargeDefinitionsByChargeAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByChargeAction(testCase.productIdentifier);
    Mockito.doReturn(chargeDefinitionsByAccrueAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByAccrueAction(testCase.productIdentifier);

    final BalanceSegmentRepository balanceSegmentRepositoryMock = Mockito.mock(BalanceSegmentRepository.class);
    Mockito.doReturn(Stream.empty()).when(balanceSegmentRepositoryMock).findByProductIdentifierAndSegmentSetIdentifier(Matchers.anyString(), Matchers.anyString());

    scheduledChargesService = new ScheduledChargesService(chargeDefinitionServiceMock, balanceSegmentRepositoryMock);

    testSubject = new IndividualLoanService(scheduledChargesService);
  }

  @Test
  public void getPlannedPayments() throws Exception {
    final PlannedPaymentPage firstPage = testSubject.getPlannedPaymentsPage(testCase.getDataContextOfAction(),
            0,
            20,
            testCase.initialDisbursementDate);

    Assert.assertFalse(firstPage.getElements().size() == 0);

    final List<PlannedPayment> allPlannedPayments =
        Stream.iterate(0, x -> x + 1).limit(firstPage.getTotalPages())
            .map(x -> testSubject.getPlannedPaymentsPage(testCase.getDataContextOfAction(),
                x,
                20,
                testCase.initialDisbursementDate))
            .flatMap(x -> x.getElements().stream())
            .collect(Collectors.toList());

    //Remaining principal should correspond with the other cost components.
    final Set<BigDecimal> customerRepayments = Stream.iterate(1, x -> x + 1).limit(allPlannedPayments.size() - 1).map(x ->
        {
          final BigDecimal costComponentSum = allPlannedPayments.get(x).getCostComponents().stream()
              .filter(this::includeCostComponentsInSumCheck)
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
          final BigDecimal valueOfPrincipleTrackingCostComponent = allPlannedPayments.get(x).getCostComponents().stream()
              .filter(costComponent -> costComponent.getChargeIdentifier().equals(ChargeIdentifiers.TRACK_RETURN_PRINCIPAL_ID))
              .map(CostComponent::getAmount)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
          final BigDecimal principalDifference = allPlannedPayments.get(x-1).getRemainingPrincipal().subtract(allPlannedPayments.get(x).getRemainingPrincipal());
          Assert.assertEquals(valueOfPrincipleTrackingCostComponent, principalDifference);
          Assert.assertNotEquals("Remaining principle should always be positive or zero.",
              allPlannedPayments.get(x).getRemainingPrincipal().signum(), -1);
          return costComponentSum;
        }
    ).collect(Collectors.toSet());

    //All entries should have the correct scale.
    allPlannedPayments.forEach(x -> {
      x.getCostComponents().forEach(y -> Assert.assertEquals(testCase.minorCurrencyUnitDigits, y.getAmount().scale()));
      Assert.assertEquals(testCase.minorCurrencyUnitDigits, x.getRemainingPrincipal().scale());
      final int uniqueChargeIdentifierCount = x.getCostComponents().stream()
          .map(CostComponent::getChargeIdentifier)
          .collect(Collectors.toSet())
          .size();
      Assert.assertEquals("There should be only one cost component per charge per planned payment.",
          x.getCostComponents().size(), uniqueChargeIdentifierCount);
    });

    //All customer payments should be within one percent of each other.
    final Optional<BigDecimal> maxPayment = customerRepayments.stream().max(BigDecimal::compareTo);
    final Optional<BigDecimal> minPayment = customerRepayments.stream().min(BigDecimal::compareTo);
    Assert.assertTrue(maxPayment.isPresent());
    Assert.assertTrue(minPayment.isPresent());
    final double percentDifference = percentDifference(maxPayment.get(), minPayment.get());
    Assert.assertTrue("Percent difference = " + percentDifference + ", max = " + maxPayment.get() + ", min = " + minPayment.get(),
        percentDifference < 0.01);

    //Final balance should be zero.
    Assert.assertEquals(BigDecimal.ZERO.setScale(testCase.minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN),
            allPlannedPayments.get(allPlannedPayments.size()-1).getRemainingPrincipal());

    //All charge identifiers should be associated with a name on the returned page.
    final Set<String> resultChargeIdentifiers = firstPage.getChargeNames().stream()
            .map(ChargeName::getIdentifier)
            .collect(Collectors.toSet());

    Assert.assertEquals(testCase.expectedChargeIdentifiers, resultChargeIdentifiers);
  }

  private boolean includeCostComponentsInSumCheck(CostComponent costComponent) {
    switch (costComponent.getChargeIdentifier()) {
      case ChargeIdentifiers.INTEREST_ID:
      case ChargeIdentifiers.DISBURSEMENT_FEE_ID:
      case ChargeIdentifiers.TRACK_DISBURSAL_PAYMENT_ID:
      case ChargeIdentifiers.LATE_FEE_ID:
      case ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID:
      case ChargeIdentifiers.TRACK_RETURN_PRINCIPAL_ID:
      case ChargeIdentifiers.PROCESSING_FEE_ID:
        return true;
      default:
        return false;

    }
  }

  @Test
  public void getScheduledCharges() {
    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(testCase.initialDisbursementDate, testCase.caseParameters);
    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(testCase.productIdentifier,
        scheduledActions);

    final List<LocalDate> interestCalculationDates = scheduledCharges.stream()
        .filter(scheduledCharge -> scheduledCharge.getScheduledAction().action == Action.APPLY_INTEREST)
        .map(scheduledCharge -> scheduledCharge.getScheduledAction().when)
        .collect(Collectors.toList());

    final List<LocalDate> allTheDaysAfterTheInitialDisbursementDate
        = Stream.iterate(testCase.initialDisbursementDate.plusDays(1), interestDay -> interestDay.plusDays(1))
        .limit(interestCalculationDates.size())
        .collect(Collectors.toList());

    Assert.assertEquals(interestCalculationDates, allTheDaysAfterTheInitialDisbursementDate);

    final List<LocalDate> acceptPaymentDates = scheduledCharges.stream()
        .filter(scheduledCharge -> scheduledCharge.getScheduledAction().action == Action.ACCEPT_PAYMENT)
        .map(scheduledCharge -> scheduledCharge.getScheduledAction().when)
        .collect(Collectors.toList());
    final long expectedAcceptPayments = scheduledActions.stream()
        .filter(x -> x.action == Action.ACCEPT_PAYMENT).count();
    final List<ChargeDefinition> chargeDefinitionsMappedToAcceptPayment = chargeDefinitionsByChargeAction.get(Action.ACCEPT_PAYMENT.name());
    final int numberOfChangeDefinitionsMappedToAcceptPayment = chargeDefinitionsMappedToAcceptPayment == null ? 0 : chargeDefinitionsMappedToAcceptPayment.size();
    Assert.assertEquals("check for correct number of scheduled charges for accept payment",
        expectedAcceptPayments*numberOfChangeDefinitionsMappedToAcceptPayment,
        acceptPaymentDates.size());

    final Map<ActionDatePair, Set<ChargeDefinition>> searchableScheduledCharges = scheduledCharges.stream()
        .collect(
            Collectors.groupingBy(scheduledCharge ->
                new ActionDatePair(scheduledCharge.getScheduledAction().action, scheduledCharge.getScheduledAction().when),
                Collectors.mapping(ScheduledCharge::getChargeDefinition, Collectors.toSet())));

    testCase.chargeDefinitionsForActions.forEach((key, value) -> value.forEach(x -> Assert.assertTrue(searchableScheduledCharges.get(key).contains(x))));
  }

  private double percentDifference(final BigDecimal maxPayment, final BigDecimal minPayment) {
    final BigDecimal difference = maxPayment.subtract(minPayment);
    final BigDecimal percentDifference = difference.divide(maxPayment, 4, BigDecimal.ROUND_UP);
    return percentDifference.doubleValue();
  }
}
