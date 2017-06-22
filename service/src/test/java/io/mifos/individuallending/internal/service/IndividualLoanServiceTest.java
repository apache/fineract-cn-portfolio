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

import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.ChargeName;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.service.ProductService;
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

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class IndividualLoanServiceTest {
  private static class TestCase {
    private final String description;
    private String productIdentifier = "blah";
    private int minorCurrencyUnitDigits = 2;
    private CaseParameters caseParameters;
    private LocalDate initialDisbursementDate;
    private Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAction;
    private Set<String> expectedChargeIdentifiers = new HashSet<>(Arrays.asList(ChargeIdentifiers.INTEREST_ID, ChargeIdentifiers.PAYMENT_ID));

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

    return new TestCase("simpleCase")
            .minorCurrencyUnitDigits(2)
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .chargeDefinitionsMappedByAction(chargeDefinitionsMappedByAction);
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
            .expectedChargeIdentifiers(new HashSet<>(Arrays.asList(ChargeIdentifiers.RETURN_DISBURSEMENT_ID, ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID, ChargeIdentifiers.INTEREST_ID, ChargeIdentifiers.PAYMENT_ID)));
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

  public IndividualLoanServiceTest(final TestCase testCase)
  {
    this.testCase = testCase;
  }

  @Test
  public void getPlannedPayments() throws Exception {
    final ProductService productServiceMock = Mockito.mock(ProductService.class);
    final ChargeDefinitionService chargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    final Product product = new Product();
    product.setMinorCurrencyUnitDigits(testCase.minorCurrencyUnitDigits);
    Mockito.doReturn(Optional.of(product)).when(productServiceMock).findByIdentifier(testCase.productIdentifier);
    Mockito.doReturn(testCase.chargeDefinitionsMappedByAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByChargeAction(testCase.productIdentifier);

    final IndividualLoanService testSubject = new IndividualLoanService(productServiceMock, chargeDefinitionServiceMock, new ScheduledActionService(), new PeriodChargeCalculator());
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
