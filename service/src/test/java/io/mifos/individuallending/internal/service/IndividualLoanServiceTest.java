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
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.service.ProductService;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PROCESSING_FEE_ID;

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

    Action getAction() {
      return action;
    }

    LocalDate getLocalDate() {
      return localDate;
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
    private Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAction;
    private Set<String> expectedChargeIdentifiers = new HashSet<>(Arrays.asList(ChargeIdentifiers.INTEREST_ID, ChargeIdentifiers.PAYMENT_ID));
    private Map<ActionDatePair, List<ChargeInstance>> chargeInstancesForActions = new HashMap<>();

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

    TestCase chargeDefinitionsMappedByAction(final Map<String, List<ChargeDefinition>> newVal) {
      this.chargeDefinitionsMappedByAction = newVal;
      return this;
    }

    TestCase expectedChargeIdentifiers(final Set<String> newVal) {
      this.expectedChargeIdentifiers = newVal;
      return this;
    }

    TestCase expectAdditionalChargeIdentifier(final String newVal) {
      this.expectedChargeIdentifiers.add(newVal);
      return this;
    }

    TestCase expectChargeInstancesForActionDatePair(final Action action,
                                                    final LocalDate forDate,
                                                    final List<ChargeInstance> chargeInstances) {
      this.chargeInstancesForActions.put(new ActionDatePair(action, forDate), chargeInstances);
      return this;
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
  private final Product product;


  private static TestCase simpleCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.WEEKS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 0, null, null));

    //I know: this is cheating in a unit test.  But I really didn't want to put this data together by hand.

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAction = new HashMap<>();
    chargeDefinitionsMappedByAction.put(Action.APPLY_INTEREST.name(), getInterestChargeDefinition(0.01, ChronoUnit.YEARS));
    chargeDefinitionsMappedByAction.put(Action.OPEN.name(),
            getFixedSingleChargeDefinition(10.0, Action.OPEN, PROCESSING_FEE_ID, AccountDesignators.PROCESSING_FEE_INCOME));

    return new TestCase("simpleCase")
            .minorCurrencyUnitDigits(2)
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .chargeDefinitionsMappedByAction(chargeDefinitionsMappedByAction)
            .expectAdditionalChargeIdentifier(PROCESSING_FEE_ID)
            .expectChargeInstancesForActionDatePair(Action.OPEN, initialDisbursementDate,
                    Collections.singletonList(new ChargeInstance(
                            AccountDesignators.ENTRY,
                            AccountDesignators.PROCESSING_FEE_INCOME,
                            BigDecimal.valueOf(10).setScale(2, BigDecimal.ROUND_UNNECESSARY))));
  }

  private static TestCase yearLoanTestCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    //final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 0, null, null));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(200000));

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAction = new HashMap<>();
    chargeDefinitionsMappedByAction.put(Action.APPLY_INTEREST.name(), getInterestChargeDefinition(0.10, ChronoUnit.YEARS));

    return new TestCase("yearLoanTestCase")
            .minorCurrencyUnitDigits(2)
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .chargeDefinitionsMappedByAction(chargeDefinitionsMappedByAction);
  }

  private static TestCase chargeDefaultsCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 2, 6);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.MONTHS, 6));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, 1, 0, 0));
    caseParameters.setMaximumBalance(BigDecimal.valueOf(2000));

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAction = new HashMap<>();
    chargeDefinitionsMappedByAction.put(Action.APPLY_INTEREST.name(), getInterestChargeDefinition(0.05, ChronoUnit.YEARS));

    final List<ChargeDefinition> defaultLoanCharges = IndividualLendingPatternFactory.defaultIndividualLoanCharges();
    defaultLoanCharges.forEach(x -> chargeDefinitionsMappedByAction.put(x.getChargeAction(), Collections.singletonList(x)));

    return new TestCase("chargeDefaultsCase")
            .minorCurrencyUnitDigits(2)
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .chargeDefinitionsMappedByAction(chargeDefinitionsMappedByAction)
            .expectedChargeIdentifiers(new HashSet<>(Arrays.asList(PROCESSING_FEE_ID, ChargeIdentifiers.RETURN_DISBURSEMENT_ID, ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID, ChargeIdentifiers.INTEREST_ID, ChargeIdentifiers.PAYMENT_ID)));
  }

  private static List<ChargeDefinition> getInterestChargeDefinition(final double amount, final ChronoUnit forCycleSizeUnit) {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setAmount(BigDecimal.valueOf(amount));
    ret.setIdentifier(ChargeIdentifiers.INTEREST_ID);
    ret.setAccrueAction(Action.APPLY_INTEREST.name());
    ret.setChargeAction(Action.ACCEPT_PAYMENT.name());
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    ret.setFromAccountDesignator(AccountDesignators.CUSTOMER_LOAN);
    ret.setAccrualAccountDesignator(AccountDesignators.INTEREST_ACCRUAL);
    ret.setToAccountDesignator(AccountDesignators.INTEREST_INCOME);
    ret.setForCycleSizeUnit(forCycleSizeUnit);
    return Collections.singletonList(ret);
  }

  private static List<ChargeDefinition> getFixedSingleChargeDefinition(
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
    ret.setFromAccountDesignator(AccountDesignators.ENTRY);
    ret.setToAccountDesignator(feeAccountDesignator);
    ret.setForCycleSizeUnit(null);
    return Collections.singletonList(ret);
  }

  public IndividualLoanServiceTest(final TestCase testCase)
  {
    this.testCase = testCase;

    final ProductService productServiceMock = Mockito.mock(ProductService.class);
    final ChargeDefinitionService chargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    product = new Product();
    product.setMinorCurrencyUnitDigits(testCase.minorCurrencyUnitDigits);
    Mockito.doReturn(Optional.of(product)).when(productServiceMock).findByIdentifier(testCase.productIdentifier);
    Mockito.doReturn(testCase.chargeDefinitionsMappedByAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByChargeAction(testCase.productIdentifier);

    testSubject = new IndividualLoanService(productServiceMock, chargeDefinitionServiceMock, new ScheduledActionService(), new PeriodChargeCalculator());
  }

  @Test
  public void getPlannedPayments() throws Exception {
    final PlannedPaymentPage firstPage = testSubject.getPlannedPaymentsPage(testCase.productIdentifier,
            testCase.caseParameters,
            0,
            20,
            testCase.initialDisbursementDate);

    final List<PlannedPayment> allPlannedPayments =
            Stream.iterate(0, x -> x + 1).limit(firstPage.getTotalPages())
            .map(x -> testSubject.getPlannedPaymentsPage(testCase.productIdentifier,
                    testCase.caseParameters, x, 20, testCase.initialDisbursementDate))
            .flatMap(x -> x.getElements().stream())
            .collect(Collectors.toList());

    //Remaining principal should correspond with the other cost components.
    Stream.iterate(0, x -> x+1).limit(allPlannedPayments.size()-2).forEach(x ->
            {
              final BigDecimal costComponentSum = allPlannedPayments.get(x+1).getCostComponents().stream()
                      .map(CostComponent::getAmount)
                      .reduce(BigDecimal::add)
                      .orElse(BigDecimal.ZERO)
                      .negate();
              final BigDecimal principalDifference = allPlannedPayments.get(x).getRemainingPrincipal().subtract(allPlannedPayments.get(x + 1).getRemainingPrincipal());
              Assert.assertEquals(costComponentSum, principalDifference);
              Assert.assertNotEquals("Remaining principle should always be positive or zero.",
                      allPlannedPayments.get(x).getRemainingPrincipal().signum(), -1);
            }
    );

    //All entries should have the correct scale.
    allPlannedPayments.forEach(x -> {
      x.getCostComponents().forEach(y -> Assert.assertEquals(product.getMinorCurrencyUnitDigits(), y.getAmount().scale()));
      Assert.assertEquals(product.getMinorCurrencyUnitDigits(), x.getRemainingPrincipal().scale());
    });

    //All customer payments should be within one percent of each other.
    final Set<BigDecimal> customerPayments = allPlannedPayments.stream().map(this::getCustomerPayment).collect(Collectors.toSet());
    final Optional<BigDecimal> maxPayment = customerPayments.stream().collect(Collectors.maxBy(BigDecimal::compareTo));
    final Optional<BigDecimal> minPayment = customerPayments.stream().collect(Collectors.minBy(BigDecimal::compareTo));
    Assert.assertTrue(maxPayment.isPresent());
    Assert.assertTrue(minPayment.isPresent());
    final double percentDifference = percentDifference(maxPayment.get(), minPayment.get());
    Assert.assertTrue("Percent difference = " + percentDifference, percentDifference < 0.01);

    //Final balance should be zero.
    Assert.assertEquals(BigDecimal.ZERO.setScale(testCase.minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN),
            allPlannedPayments.get(allPlannedPayments.size()-1).getRemainingPrincipal());

    //All charge identifers should be associated with a name on the returned page.
    final Set<String> resultChargeIdentifiers = firstPage.getChargeNames().stream()
            .map(ChargeName::getIdentifier)
            .collect(Collectors.toSet());

    Assert.assertEquals(testCase.expectedChargeIdentifiers, resultChargeIdentifiers);
  }

  @Test
  public void createChargeInstances() {
    testCase.chargeInstancesForActions.entrySet().forEach(entry ->
            Assert.assertEquals(
                    entry.getValue(),
                    testSubject.getChargeInstances(
                            testCase.productIdentifier,
                            testCase.caseParameters,
                            testCase.caseParameters.getMaximumBalance(),
                            entry.getKey().getAction(),
                            testCase.initialDisbursementDate, entry.getKey().getLocalDate())));
  }

  private double percentDifference(final BigDecimal maxPayment, final BigDecimal minPayment) {
    final BigDecimal difference = maxPayment.subtract(minPayment);
    final BigDecimal percentDifference = difference.divide(maxPayment, 4, BigDecimal.ROUND_UP);
    return percentDifference.doubleValue();
  }

  private BigDecimal getCustomerPayment(final PlannedPayment plannedPayment) {
    final Optional<CostComponent> ret = plannedPayment.getCostComponents().stream()
            .filter(y -> y.getChargeIdentifier().equals(ChargeIdentifiers.PAYMENT_ID))
            .findAny();

    Assert.assertTrue(ret.isPresent());

    return ret.get().getAmount().abs();
  }
}
