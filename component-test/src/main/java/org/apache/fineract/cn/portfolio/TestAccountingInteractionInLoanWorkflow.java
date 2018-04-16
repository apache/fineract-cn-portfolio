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
package org.apache.fineract.cn.portfolio;

import static org.apache.fineract.cn.portfolio.Fixture.MINOR_CURRENCY_UNIT_DIGITS;

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionConfiguration;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanCommandEvent;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CaseStatus;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.ImportParameters;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.api.v1.domain.PaymentCycle;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.api.v1.events.BalanceSegmentSetEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.ChargeDefinitionEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.Creditor;
import org.apache.fineract.cn.accounting.api.v1.domain.Debtor;
import org.apache.fineract.cn.api.util.ApiFactory;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.rhythm.spi.v1.client.BeatListener;
import org.apache.fineract.cn.rhythm.spi.v1.domain.BeatPublish;
import org.apache.fineract.cn.rhythm.spi.v1.events.BeatPublishEvent;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Myrle Krantz
 */
public class TestAccountingInteractionInLoanWorkflow extends AbstractPortfolioTest {
  private static final BigDecimal PROCESSING_FEE_AMOUNT = BigDecimal.valueOf(50_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal LOAN_ORIGINATION_FEE_AMOUNT = BigDecimal.valueOf(50_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal DISBURSEMENT_FEE_LOWER_RANGE_AMOUNT = BigDecimal.valueOf(10_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal DISBURSEMENT_FEE_UPPER_RANGE_AMOUNT = BigDecimal.valueOf(1_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal IMPORTED_NEXT_REPAYMENT_AMOUNT = BigDecimal.valueOf(300_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final String DISBURSEMENT_RANGES = "disbursement_ranges";
  private static final String DISBURSEMENT_LOWER_RANGE = "smaller";
  private static final String DISBURSEMENT_UPPER_RANGE = "larger";
  private static final String UPPER_RANGE_DISBURSEMENT_FEE_ID = ChargeIdentifiers.DISBURSEMENT_FEE_ID + "2";

  private BeatListener portfolioBeatListener;

  private Product product = null;
  private Case customerCase = null;
  private TaskDefinition taskDefinition = null;
  private String customerLoanPrincipalIdentifier = null;
  private String customerLoanInterestIdentifier = null;
  private String customerLoanFeeIdentifier = null;

  private BigDecimal expectedCurrentPrincipal = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
  private BigDecimal interestAccrued = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
  private BigDecimal nonLateFees = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
  private BigDecimal lateFees = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
  private BigDecimal productLossAllowance = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);


  @Before
  public void prepBeatListener() {
    portfolioBeatListener = new ApiFactory(logger).create(BeatListener.class, testEnvironment.serverURI());
  }

  @Test
  public void workflowTerminatingInApplicationDenial() throws InterruptedException {
    final LocalDateTime today = midnightToday();
    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4DenyCase(today);
  }

  @Test
  public void workflowImportingLoanFromExternalSystem() throws InterruptedException {
    final LocalDateTime initialDisbursalDate = LocalDateTime.of(2017,11,8,0,0);
    final LocalDateTime today = LocalDateTime.of(2017,11,22,0,0);

    step1CreateProduct();
    step2CreateCase();
    step3IImportCaseWhenAccountsDontExistYet(initialDisbursalDate);

    final List<PlannedPayment> plannedPayments = individualLending.getPaymentScheduleForCaseStream(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        null)
        .collect(Collectors.toList());

    int week = 3;
    while (expectedCurrentPrincipal.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Simulating week {}. Expected current principal {}.", week, expectedCurrentPrincipal);
      step6CalculateInterestAndCheckForLatenessForWeek(today, week);
      final BigDecimal interestAccruedBeforePayment = interestAccrued;
      final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today.plusDays((week+1)*7));
      final BigDecimal totalDue = expectedCurrentPrincipal.add(interestAccrued).add(nonLateFees);
      if (totalDue.compareTo(IMPORTED_NEXT_REPAYMENT_AMOUNT) >= 0)
        Assert.assertEquals(IMPORTED_NEXT_REPAYMENT_AMOUNT, nextRepaymentAmount);
      else
        Assert.assertEquals(totalDue, nextRepaymentAmount);
      final Payment payment = step7PaybackPartialAmount(
          nextRepaymentAmount,
          today.plusDays((week + 1) * 7),
          BigDecimal.ZERO,
          nextRepaymentAmount,
          AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
      final BigDecimal interestAccrual = payment.getBalanceAdjustments().remove(AccountDesignators.INTEREST_ACCRUAL); //Don't compare these with planned payment.
      final BigDecimal customerLoanInterest = payment.getBalanceAdjustments().remove(AccountDesignators.CUSTOMER_LOAN_INTEREST);
      Assert.assertEquals("week " + week, interestAccrual.negate(), customerLoanInterest);
      Assert.assertEquals("week " + week, interestAccruedBeforePayment, customerLoanInterest);
      //TODO: Assert.assertEquals("week " + week, plannedPayments.get(week+1).getPayment(), payment);
      week++;
    }

    step8Close(DateConverter
        .fromIsoString(plannedPayments.get(plannedPayments.size()-1).getPayment().getDate()));
  }


  @Test
  public void workflowImportingLoanFromExternalSystemConnectingToExistingAccounts() throws InterruptedException {
    final LocalDateTime initialDisbursalDate = LocalDateTime.of(2017,11,8,0,0);
    final LocalDateTime today = LocalDateTime.of(2017,11,22,0,0);

    step1CreateProduct();
    step2CreateCase();
    step3IImportCaseWhenAccountsExist(initialDisbursalDate);

    final BigDecimal payment = expectedCurrentPrincipal.add(nonLateFees).add(interestAccrued);
    step7PaybackPartialAmount(
        payment,
        today,
        BigDecimal.ZERO,
        payment,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step8Close(today);
  }

  @Test
  public void cantChangeDeniedCase() throws InterruptedException {
    final LocalDateTime today = midnightToday();
    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4DenyCase(today);

    try {
      customerCase.setInterest(BigDecimal.ONE);
      portfolioManager.changeCase(product.getIdentifier(), customerCase.getIdentifier(), customerCase);
      Assert.fail("Changing a denied case should fail.");
    }
    catch (IllegalArgumentException ignored) {

    }
  }

  @Test
  public void cantChangeApprovedCase() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);

    try {
      customerCase.setInterest(BigDecimal.ONE);
      portfolioManager.changeCase(product.getIdentifier(), customerCase.getIdentifier(), customerCase);
      Assert.fail("Changing a denied case should fail.");
    }
    catch (IllegalArgumentException ignored) {

    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantRequestCostComponentsForInvalidAction() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    portfolioManager.getCostComponentsForAction(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE.name(),
        Collections.emptySet(),
        BigDecimal.TEN,
        DateConverter.toIsoString(today)
    );
  }

  //TODO: once we've upgraded to junit 5 replace workflowTerminatingInEarlyLoanPayoff,
  // and workflowDisbursalAndPayoffFromTellerAccount with one test annotated with
  //@ParameterizedTest
  //@ValueSource(strings = { AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT, AccountingFixture.TELLER_ONE_ACCOUNT })
  @Test
  public void workflowTerminatingInEarlyLoanPayoff() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), BigDecimal.ZERO);
    final BigDecimal payment = expectedCurrentPrincipal.add(nonLateFees).add(interestAccrued);
    step7PaybackPartialAmount(
        payment,
        today,
        BigDecimal.ZERO,
        payment,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step8Close(today);
  }

  @Test
  public void workflowDisbursalAndPayoffFromTellerAccount() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        AccountingFixture.TELLER_ONE_ACCOUNT);
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), BigDecimal.ZERO);
    final BigDecimal payment = expectedCurrentPrincipal.add(nonLateFees).add(interestAccrued);
    step7PaybackPartialAmount(
        payment,
        today,
        BigDecimal.ZERO,
        BigDecimal.ZERO, //payback should work even if there's *nothing* in the teller account at initiation.
        AccountingFixture.TELLER_ONE_ACCOUNT);
    step8Close(today);
  }

  @Test
  public void workflowWithTwoUnequalDisbursals() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);
    step5Disburse(
        BigDecimal.valueOf(500_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        ChargeIdentifiers.DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(10_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step5Disburse(
        BigDecimal.valueOf(1_500_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(15_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), BigDecimal.ZERO);
    final BigDecimal payment = expectedCurrentPrincipal.add(nonLateFees).add(interestAccrued);
    step7PaybackPartialAmount(
        payment,
        today,
        BigDecimal.ZERO,
        payment,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step8Close(today);
  }

  @Test
  public void workflowWithTwoNearlyEqualRepayments() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), BigDecimal.ZERO);
    final BigDecimal repayment1 = expectedCurrentPrincipal.divide(BigDecimal.valueOf(2), BigDecimal.ROUND_HALF_EVEN)
        .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);
    step7PaybackPartialAmount(
        repayment1,
        today,
        BigDecimal.ZERO,
        repayment1,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step7PaybackPartialAmount(
        expectedCurrentPrincipal,
        today,
        BigDecimal.ZERO,
        expectedCurrentPrincipal,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
    step8Close(today);
  }

  @Test
  public void workflowWithNegativePaymentSize() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);
    try {
      step5Disburse(
          BigDecimal.valueOf(-2).setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN),
          today,
          UPPER_RANGE_DISBURSEMENT_FEE_ID,
          BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS,BigDecimal.ROUND_HALF_EVEN),
          BigDecimal.ZERO,
          AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
      Assert.fail("Expected an IllegalArgumentException.");
    }
    catch (IllegalArgumentException ignored) { }
  }

  @Test
  public void workflowWithNormalRepayment() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);

    final List<PlannedPayment> plannedPayments = individualLending.getPaymentScheduleForCaseStream(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        null)
        .collect(Collectors.toList());

    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);

    int week = 0;
    while (expectedCurrentPrincipal.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Simulating week {}. Expected current principal {}.", week, expectedCurrentPrincipal);
      step6CalculateInterestAndCheckForLatenessForWeek(today, week);
      final BigDecimal interestAccruedBeforePayment = interestAccrued;
      final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today.plusDays((week+1)*7));
      final Payment payment = step7PaybackPartialAmount(
          nextRepaymentAmount,
          today.plusDays((week + 1) * 7),
          BigDecimal.ZERO,
          nextRepaymentAmount,
          AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
      final BigDecimal interestAccrual = payment.getBalanceAdjustments().remove(AccountDesignators.INTEREST_ACCRUAL); //Don't compare these with planned payment.
      final BigDecimal customerLoanInterest = payment.getBalanceAdjustments().remove(AccountDesignators.CUSTOMER_LOAN_INTEREST);
      Assert.assertEquals("week " + week, interestAccrual.negate(), customerLoanInterest);
      Assert.assertEquals("week " + week, interestAccruedBeforePayment, customerLoanInterest);
      Assert.assertEquals("week " + week, plannedPayments.get(week+1).getPayment(), payment);
      week++;
    }

    Assert.assertEquals(week+1, plannedPayments.size());

    step8Close(DateConverter.fromIsoString(plannedPayments.get(plannedPayments.size()-1).getPayment().getDate()));
  }

  @Test
  public void workflowWithOneLateRepayment() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);

    final List<PlannedPayment> plannedPayments = individualLending.getPaymentScheduleForCaseStream(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        null)
        .collect(Collectors.toList());

    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);

    int week = 0;
    final int weekOfLateRepayment = 3;
    while (expectedCurrentPrincipal.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Simulating week {}. Expected current balance {}.", week, expectedCurrentPrincipal);
      if (week == weekOfLateRepayment) {
        final BigDecimal lateFee = BigDecimal.valueOf(15_36, MINOR_CURRENCY_UNIT_DIGITS); //??? TODO: check the late fee value.
        step6CalculateInterestAndCheckForLatenessForRangeOfDays(
            today,
            (week * 7) + 1,
            (week + 1) * 7 + 2,
            7,
            lateFee);
        final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today.plusDays((week + 1) * 7 + 2));
        step7PaybackPartialAmount(
            nextRepaymentAmount,
            today.plusDays((week + 1) * 7 + 2),
            lateFee,
            nextRepaymentAmount,
            AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
      }
      else {
        step6CalculateInterestAndCheckForLatenessForWeek(today, week);
        final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today.plusDays((week + 1) * 7));
        final Payment payment = step7PaybackPartialAmount(
            nextRepaymentAmount,
            today.plusDays((week + 1) * 7),
            BigDecimal.ZERO,
            nextRepaymentAmount,
            AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);
        final BigDecimal interestAccrual = payment.getBalanceAdjustments().remove(AccountDesignators.INTEREST_ACCRUAL); //Don't compare these with planned payment.
        final BigDecimal customerLoanInterest = payment.getBalanceAdjustments().remove(AccountDesignators.CUSTOMER_LOAN_INTEREST);
        Assert.assertEquals(interestAccrual.negate(), customerLoanInterest);
        //Assert.assertEquals(plannedPayments.get(week+1).getPayment(), payment);
      }
      week++;
    }

    step8Close(DateConverter.fromIsoString(plannedPayments.get(plannedPayments.size()-1).getPayment().getDate()));
  }

  @Test
  public void workflowTerminatingInWriteOff() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase(today);
    step4ApproveCase(today);

    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        today,
        UPPER_RANGE_DISBURSEMENT_FEE_ID,
        BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS),
        BigDecimal.ZERO,
        AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT);

    final BigDecimal lateFee = BigDecimal.valueOf(15_36, MINOR_CURRENCY_UNIT_DIGITS); //??? TODO: check the late fee value.
    step6CalculateInterestAndCheckForLatenessForRangeOfDays(
        today,
        1,
        8,
        7,
        lateFee);
    step6ICalculateInterestAndLossAllowancesForLateLoanForRangeOfDays(
        today,
        new LossProvisionStep(0, BigDecimal.valueOf(1)),
        new LossProvisionStep(1, BigDecimal.valueOf(9)),
        new LossProvisionStep(30, BigDecimal.valueOf(30)),
        new LossProvisionStep(60, BigDecimal.valueOf(60))
    );

    step8IWriteOff(today.plusDays(68));
  }

  private BigDecimal findNextRepaymentAmount(
      final LocalDateTime forDateTime) {
    AccountingFixture.mockBalance(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT, BigDecimal.valueOf(2000_00L, 2));
    final Payment nextPayment = portfolioManager.getCostComponentsForAction(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT.name(),
        null,
        null,
        DateConverter.toIsoString(forDateTime));
    final BigDecimal nextRepaymentAmount = nextPayment.getBalanceAdjustments()
        .getOrDefault(AccountDesignators.ENTRY, BigDecimal.ZERO).negate();
    Assert.assertTrue(nextRepaymentAmount.signum() != -1);
    return nextRepaymentAmount;
  }

  //Create product and set charges to fixed fees.
  private void step1CreateProduct() throws InterruptedException {
    logger.info("step1CreateProduct");
    product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(DISBURSEMENT_RANGES);
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList(DISBURSEMENT_LOWER_RANGE, DISBURSEMENT_UPPER_RANGE));
    balanceSegmentSet.setSegments(Arrays.asList(BigDecimal.ZERO, BigDecimal.valueOf(1_000_0000, 4)));
    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    setFeeToFixedValue(product.getIdentifier(), ChargeIdentifiers.PROCESSING_FEE_ID, PROCESSING_FEE_AMOUNT);
    setFeeToFixedValue(product.getIdentifier(), ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID, LOAN_ORIGINATION_FEE_AMOUNT);

    final ChargeDefinition lowerRangeDisbursementFeeChargeDefinition
        = portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSEMENT_FEE_ID);
    lowerRangeDisbursementFeeChargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    lowerRangeDisbursementFeeChargeDefinition.setAmount(DISBURSEMENT_FEE_LOWER_RANGE_AMOUNT);
    lowerRangeDisbursementFeeChargeDefinition.setProportionalTo(ChargeProportionalDesignator.REQUESTED_DISBURSEMENT_DESIGNATOR.getValue());
    lowerRangeDisbursementFeeChargeDefinition.setForSegmentSet(DISBURSEMENT_RANGES);
    lowerRangeDisbursementFeeChargeDefinition.setFromSegment(DISBURSEMENT_LOWER_RANGE);
    lowerRangeDisbursementFeeChargeDefinition.setToSegment(DISBURSEMENT_LOWER_RANGE);

    portfolioManager.changeChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSEMENT_FEE_ID, lowerRangeDisbursementFeeChargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), ChargeIdentifiers.DISBURSEMENT_FEE_ID)));

    final ChargeDefinition upperRangeDisbursementFeeChargeDefinition = new ChargeDefinition();
    upperRangeDisbursementFeeChargeDefinition.setIdentifier(UPPER_RANGE_DISBURSEMENT_FEE_ID);
    upperRangeDisbursementFeeChargeDefinition.setName(UPPER_RANGE_DISBURSEMENT_FEE_ID);
    upperRangeDisbursementFeeChargeDefinition.setDescription(lowerRangeDisbursementFeeChargeDefinition.getDescription());
    upperRangeDisbursementFeeChargeDefinition.setFromAccountDesignator(lowerRangeDisbursementFeeChargeDefinition.getFromAccountDesignator());
    upperRangeDisbursementFeeChargeDefinition.setToAccountDesignator(lowerRangeDisbursementFeeChargeDefinition.getToAccountDesignator());
    upperRangeDisbursementFeeChargeDefinition.setAccrualAccountDesignator(lowerRangeDisbursementFeeChargeDefinition.getAccrualAccountDesignator());
    upperRangeDisbursementFeeChargeDefinition.setAccrueAction(lowerRangeDisbursementFeeChargeDefinition.getAccrueAction());
    upperRangeDisbursementFeeChargeDefinition.setChargeAction(lowerRangeDisbursementFeeChargeDefinition.getChargeAction());
    upperRangeDisbursementFeeChargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    upperRangeDisbursementFeeChargeDefinition.setAmount(DISBURSEMENT_FEE_UPPER_RANGE_AMOUNT);
    upperRangeDisbursementFeeChargeDefinition.setProportionalTo(ChargeProportionalDesignator.REQUESTED_DISBURSEMENT_DESIGNATOR.getValue());
    upperRangeDisbursementFeeChargeDefinition.setForSegmentSet(DISBURSEMENT_RANGES);
    upperRangeDisbursementFeeChargeDefinition.setFromSegment(DISBURSEMENT_UPPER_RANGE);
    upperRangeDisbursementFeeChargeDefinition.setToSegment(DISBURSEMENT_UPPER_RANGE);

    portfolioManager.createChargeDefinition(product.getIdentifier(), upperRangeDisbursementFeeChargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), UPPER_RANGE_DISBURSEMENT_FEE_ID)));

    taskDefinition = createTaskDefinition(product);

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    final List<LossProvisionStep> lossProvisionSteps = Arrays.asList(
        new LossProvisionStep(0, BigDecimal.ONE),
        new LossProvisionStep(1, BigDecimal.valueOf(9)),
        new LossProvisionStep(30, BigDecimal.valueOf(30)),
        new LossProvisionStep(60, BigDecimal.valueOf(60)));
    final LossProvisionConfiguration lossProvisionConfiguration = new LossProvisionConfiguration(lossProvisionSteps);
    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration);
    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.PUT_LOSS_PROVISION_STEPS, product.getIdentifier()));
  }

  private void step2CreateCase() throws InterruptedException {
    logger.info("step2CreateCase");
    final CaseParameters caseParameters = Fixture.createAdjustedCaseParameters(x ->
        x.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, null, null, null))
    );
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    customerCase = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, Action.IMPORT);
  }

  //Open the case and accept a processing fee.
  private void step3OpenCase(final LocalDateTime forDateTime) throws InterruptedException {
    logger.info("step3OpenCase");
    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        forDateTime,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE,
        Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);
  }

  //Import an active case.
  private void step3IImportCaseWhenAccountsDontExistYet(final LocalDateTime forDateTime) throws InterruptedException {
    logger.info("step3IImportCaseWhenAccountsDontExistYet");

    final BigDecimal currentPrincipal = BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS);

    final AccountAssignment customerLoanPrincipalAccountAssignment = new AccountAssignment();
    customerLoanPrincipalAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    customerLoanPrincipalAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-principal-account-identifier");
    customerLoanPrincipalAccountAssignment.setLedgerIdentifier(AccountDesignators.CUSTOMER_LOAN_GROUP);
    final AccountAssignment customerLoanInterestAccountAssignment = new AccountAssignment();
    customerLoanInterestAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    customerLoanInterestAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-interest-account-identifier");
    customerLoanInterestAccountAssignment.setLedgerIdentifier(AccountDesignators.CUSTOMER_LOAN_GROUP);
    final AccountAssignment customerLoanFeeAccountAssignment = new AccountAssignment();
    customerLoanFeeAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_FEES);
    customerLoanFeeAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-fees-account-identifier");
    customerLoanFeeAccountAssignment.setLedgerIdentifier(AccountDesignators.CUSTOMER_LOAN_GROUP);
    final ArrayList<AccountAssignment> importAccountAssignments = new ArrayList<>();
    importAccountAssignments.add(customerLoanPrincipalAccountAssignment);
    importAccountAssignments.add(customerLoanInterestAccountAssignment);
    importAccountAssignments.add(customerLoanFeeAccountAssignment);

    final ImportParameters importParameters = new ImportParameters();
    importParameters.setCaseAccountAssignments(importAccountAssignments);
    importParameters.setPaymentSize(IMPORTED_NEXT_REPAYMENT_AMOUNT);
    importParameters.setCreatedOn(DateConverter.toIsoString(forDateTime));
    importParameters.setCurrentBalances(Collections.singletonMap(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, currentPrincipal));
    importParameters.setStartOfTerm(DateConverter.toIsoString(forDateTime));
    portfolioManager.executeImportCommand(product.getIdentifier(), customerCase.getIdentifier(), importParameters);

    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.IMPORT_INDIVIDUALLOAN_CASE, new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), DateConverter.toIsoString(forDateTime))));

    final String customerLoanLedgerIdentifier = AccountingFixture.verifyLedgerCreation(
        ledgerManager,
        AccountingFixture.CUSTOMER_LOAN_LEDGER_IDENTIFIER,
        AccountType.ASSET);

    customerLoanPrincipalIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(
            ledgerManager, customerLoanLedgerIdentifier,
            AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
            customerLoanPrincipalAccountAssignment.getAlternativeAccountNumber(),
            AccountType.ASSET,
            currentPrincipal);
    customerLoanInterestIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(
            ledgerManager,
            customerLoanLedgerIdentifier,
            AccountDesignators.CUSTOMER_LOAN_INTEREST,
            customerLoanInterestAccountAssignment.getAlternativeAccountNumber(),
            AccountType.ASSET,
            BigDecimal.ZERO);
    customerLoanFeeIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(
            ledgerManager,
            customerLoanLedgerIdentifier,
            AccountDesignators.CUSTOMER_LOAN_FEES,
            customerLoanFeeAccountAssignment.getAlternativeAccountNumber(),
            AccountType.ASSET,
            BigDecimal.ZERO);

    final CaseStatus changedCaseStatus = portfolioManager.getCaseStatus(product.getIdentifier(), customerCase.getIdentifier());
    Assert.assertEquals(Case.State.ACTIVE.name(), changedCaseStatus.getCurrentState());
    Assert.assertEquals(DateConverter.toIsoString(forDateTime), changedCaseStatus.getStartOfTerm());
    Assert.assertEquals("2018-02-08T00:00:00Z", changedCaseStatus.getEndOfTerm());
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.MARK_IN_ARREARS, Action.WRITE_OFF, Action.CLOSE);

    expectedCurrentPrincipal = currentPrincipal;
    updateBalanceMock();

    final Case changedCase = portfolioManager.getCase(product.getIdentifier(), customerCase.getIdentifier());
    final Map<String, String> designatorsAssignedForCase = changedCase.getAccountAssignments().stream()
        .collect(Collectors.toMap(AccountAssignment::getDesignator, AccountAssignment::getAccountIdentifier));
    Assert.assertEquals(
        customerLoanPrincipalIdentifier,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL));
    Assert.assertEquals(
        customerLoanInterestIdentifier,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_INTEREST));
    Assert.assertEquals(
        customerLoanFeeIdentifier,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_FEES));
  }

  //Import an active case.
  private void step3IImportCaseWhenAccountsExist(final LocalDateTime forDateTime) throws InterruptedException {
    logger.info("step3IImportCaseWhenAccountsExist");

    final BigDecimal currentPrincipal = BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS);

    final AccountAssignment customerLoanPrincipalAccountAssignment = new AccountAssignment();
    customerLoanPrincipalAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    customerLoanPrincipalAccountAssignment.setAccountIdentifier(AccountingFixture.IMPORTED_CUSTOMER_LOAN_PRINCIPAL_ACCOUNT);
    customerLoanPrincipalAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-principal-account-identifier");
    final AccountAssignment customerLoanInterestAccountAssignment = new AccountAssignment();
    customerLoanInterestAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    customerLoanInterestAccountAssignment.setAccountIdentifier(AccountingFixture.IMPORTED_CUSTOMER_LOAN_INTEREST_ACCOUNT);
    customerLoanInterestAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-interest-account-identifier");
    final AccountAssignment customerLoanFeeAccountAssignment = new AccountAssignment();
    customerLoanFeeAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_FEES);
    customerLoanFeeAccountAssignment.setAccountIdentifier(AccountingFixture.IMPORTED_CUSTOMER_LOAN_FEES_ACCOUNT);
    customerLoanFeeAccountAssignment.setAlternativeAccountNumber("external-system-sourced-customer-loan-fees-account-identifier");
    final ArrayList<AccountAssignment> importAccountAssignments = new ArrayList<>();
    importAccountAssignments.add(customerLoanPrincipalAccountAssignment);
    importAccountAssignments.add(customerLoanInterestAccountAssignment);
    importAccountAssignments.add(customerLoanFeeAccountAssignment);

    final ImportParameters importParameters = new ImportParameters();
    importParameters.setCaseAccountAssignments(importAccountAssignments);
    importParameters.setPaymentSize(IMPORTED_NEXT_REPAYMENT_AMOUNT);
    importParameters.setCreatedOn(DateConverter.toIsoString(forDateTime));
    importParameters.setCurrentBalances(Collections.singletonMap(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, currentPrincipal));
    importParameters.setStartOfTerm(DateConverter.toIsoString(forDateTime));
    portfolioManager.executeImportCommand(product.getIdentifier(), customerCase.getIdentifier(), importParameters);

    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.IMPORT_INDIVIDUALLOAN_CASE, new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), DateConverter.toIsoString(forDateTime))));

    AccountingFixture.verifyLedgerCreation(
        ledgerManager,
        AccountingFixture.CUSTOMER_LOAN_LEDGER_IDENTIFIER,
        AccountType.ASSET);

    customerLoanPrincipalIdentifier = AccountingFixture.IMPORTED_CUSTOMER_LOAN_PRINCIPAL_ACCOUNT;
    customerLoanInterestIdentifier = AccountingFixture.IMPORTED_CUSTOMER_LOAN_INTEREST_ACCOUNT;
    customerLoanFeeIdentifier = AccountingFixture.IMPORTED_CUSTOMER_LOAN_FEES_ACCOUNT;

    final CaseStatus changedCaseStatus = portfolioManager.getCaseStatus(product.getIdentifier(), customerCase.getIdentifier());
    Assert.assertEquals(Case.State.ACTIVE.name(), changedCaseStatus.getCurrentState());
    Assert.assertEquals(DateConverter.toIsoString(forDateTime), changedCaseStatus.getStartOfTerm());
    Assert.assertEquals("2018-02-08T00:00:00Z", changedCaseStatus.getEndOfTerm());
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.MARK_IN_ARREARS, Action.WRITE_OFF, Action.CLOSE);

    expectedCurrentPrincipal = currentPrincipal;
    updateBalanceMock();

    final Case changedCase = portfolioManager.getCase(product.getIdentifier(), customerCase.getIdentifier());
    final Map<String, String> designatorsAssignedForCase = changedCase.getAccountAssignments().stream()
        .collect(Collectors.toMap(AccountAssignment::getDesignator, AccountAssignment::getAccountIdentifier));
    Assert.assertEquals(
        AccountingFixture.IMPORTED_CUSTOMER_LOAN_PRINCIPAL_ACCOUNT,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL));
    Assert.assertEquals(
        AccountingFixture.IMPORTED_CUSTOMER_LOAN_INTEREST_ACCOUNT,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_INTEREST));
    Assert.assertEquals(
        AccountingFixture.IMPORTED_CUSTOMER_LOAN_FEES_ACCOUNT,
        designatorsAssignedForCase.get(AccountDesignators.CUSTOMER_LOAN_FEES));
  }


  //Deny the case. Once this is done, no more actions are possible for the case.
  private void step4DenyCase(final LocalDateTime forDateTime) throws InterruptedException {
    logger.info("step4DenyCase");
    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DENY,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DENY,
        forDateTime,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE,
        Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }


  //Approve the case, accept a loan origination fee, and prepare to disburse the loan by earmarking the funds.
  private void step4ApproveCase(final LocalDateTime forDateTime) throws InterruptedException
  {
    logger.info("step4ApproveCase");

    markTaskExecuted(product, customerCase, taskDefinition);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPROVE,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPROVE,
        forDateTime,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE,
        Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);

    final String customerLoanLedgerIdentifier = AccountingFixture.verifyLedgerCreation(
        ledgerManager,
        AccountingFixture.CUSTOMER_LOAN_LEDGER_IDENTIFIER,
        AccountType.ASSET);

    customerLoanPrincipalIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(ledgerManager, customerLoanLedgerIdentifier, AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, AccountType.ASSET);
    customerLoanInterestIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(ledgerManager, customerLoanLedgerIdentifier, AccountDesignators.CUSTOMER_LOAN_INTEREST, AccountType.ASSET);
    customerLoanFeeIdentifier =
        AccountingFixture.verifyAccountCreationMatchingDesignator(ledgerManager, customerLoanLedgerIdentifier, AccountDesignators.CUSTOMER_LOAN_FEES, AccountType.ASSET);

    expectedCurrentPrincipal = BigDecimal.ZERO;
    interestAccrued = BigDecimal.ZERO;
    nonLateFees = BigDecimal.ZERO;
    lateFees = BigDecimal.ZERO;
    updateBalanceMock();
  }

  //Approve the case, accept a loan origination fee, and prepare to disburse the loan by earmarking the funds.
  private void step5Disburse(
      final BigDecimal amount,
      final LocalDateTime forDateTime,
      final String whichDisbursementFee,
      final BigDecimal disbursementFeeAmount,
      final BigDecimal balanceInEntryAccount,
      final String entryAccountIdentifier) throws InterruptedException {
    logger.info("step5Disburse  '{}'", amount);
    final BigDecimal provisionForLosses = amount.multiply(BigDecimal.valueOf(0.01)).setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);

    AccountingFixture.mockBalance(entryAccountIdentifier, balanceInEntryAccount);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE,
        Sets.newLinkedHashSet(AccountDesignators.ENTRY, AccountDesignators.CUSTOMER_LOAN_GROUP),
        amount,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS,
        new CostComponent(whichDisbursementFee, disbursementFeeAmount),
        new CostComponent(ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID, LOAN_ORIGINATION_FEE_AMOUNT),
        new CostComponent(ChargeIdentifiers.PROCESSING_FEE_ID, PROCESSING_FEE_AMOUNT),
        new CostComponent(ChargeIdentifiers.DISBURSE_PAYMENT_ID, amount));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE,
        forDateTime,
        assignEntry(entryAccountIdentifier),
        amount,
        IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE,
        Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.MARK_IN_ARREARS, Action.WRITE_OFF, Action.CLOSE);

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(customerLoanPrincipalIdentifier, amount.toPlainString()));
    debtors.add(new Debtor(customerLoanFeeIdentifier, PROCESSING_FEE_AMOUNT.add(disbursementFeeAmount).add(LOAN_ORIGINATION_FEE_AMOUNT).toPlainString()));
    debtors.add(new Debtor(AccountingFixture.GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, provisionForLosses.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(entryAccountIdentifier, amount.toString()));
    creditors.add(new Creditor(AccountingFixture.PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER, PROCESSING_FEE_AMOUNT.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER, disbursementFeeAmount.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER, LOAN_ORIGINATION_FEE_AMOUNT.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, provisionForLosses.toPlainString()));
    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE);

    expectedCurrentPrincipal = expectedCurrentPrincipal.add(amount);
    interestAccrued = BigDecimal.ZERO;
    nonLateFees = nonLateFees.add(disbursementFeeAmount).add(PROCESSING_FEE_AMOUNT).add(LOAN_ORIGINATION_FEE_AMOUNT);
    lateFees = BigDecimal.ZERO;
    productLossAllowance = provisionForLosses;

    updateBalanceMock();
  }

  private void step6CalculateInterestAndCheckForLatenessForWeek(
      final LocalDateTime referenceDate,
      final int weekNumber) throws InterruptedException {
    step6CalculateInterestAndCheckForLatenessForRangeOfDays(
        referenceDate,
        (weekNumber * 7) + 1,
        (weekNumber + 1) * 7,
        -1,
        BigDecimal.ZERO);
  }

  private void step6CalculateInterestAndCheckForLatenessForRangeOfDays(
      final LocalDateTime referenceDate,
      final int startInclusive,
      final int endInclusive,
      final int relativeDayOfLateFee,
      final BigDecimal calculatedLateFee) throws InterruptedException {
    try {
      final LocalDateTime absoluteDayOfLateFee = referenceDate.plusDays(startInclusive + relativeDayOfLateFee);
      IntStream.rangeClosed(startInclusive, endInclusive)
          .mapToObj(referenceDate::plusDays)
          .forEach(day -> {
            try {
              if (day.equals(absoluteDayOfLateFee)) {
                step6CalculateInterestAccrualAndCheckForLateness(day, calculatedLateFee);
              }
              else {
                step6CalculateInterestAccrualAndCheckForLateness(day, BigDecimal.ZERO);
              }
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause != null && cause.getClass().isAssignableFrom(InterruptedException.class))
        throw (InterruptedException)e.getCause();
      else
        throw e;
    }
  }

  //Perform daily interest calculation.
  private void step6CalculateInterestAccrualAndCheckForLateness(
      final LocalDateTime forDateTime,
      final BigDecimal calculatedLateFee) throws InterruptedException {
    logger.info("step6CalculateInterestAccrualAndCheckForLateness  '{}'", forDateTime);
    final String beatIdentifier = "alignment0";
    final String midnightTimeStamp = DateConverter.toIsoString(forDateTime);

    final BigDecimal dailyInterestRate = Fixture.INTEREST_RATE
        .divide(BigDecimal.valueOf(100), 8, BigDecimal.ROUND_HALF_EVEN)
        .divide(Fixture.ACCRUAL_PERIODS, 8, BigDecimal.ROUND_HALF_EVEN);

    final BigDecimal calculatedInterest = expectedCurrentPrincipal
        .multiply(dailyInterestRate)
        .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);

    final BigDecimal provisionForLosses = calculatedLateFee.equals(BigDecimal.ZERO) ?
        BigDecimal.ZERO :
        expectedCurrentPrincipal.multiply(BigDecimal.valueOf(0.09))
            .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);

    logger.info("calculatedInterest '{}'", calculatedInterest);
    logger.info("calculatedLateFee '{}'", calculatedLateFee);
    logger.info("provisionForLosses '{}'", provisionForLosses);


    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPLY_INTEREST,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);

    if (calculatedLateFee.compareTo(BigDecimal.ZERO) != 0) {
      checkCostComponentForActionCorrect(
          product.getIdentifier(),
          customerCase.getIdentifier(),
          Action.MARK_LATE,
          null,
          null,
          forDateTime,
          MINOR_CURRENCY_UNIT_DIGITS,
          new CostComponent(ChargeIdentifiers.PROVISION_FOR_LOSSES_ID, provisionForLosses.negate()));
    }
    final BeatPublish interestBeat = new BeatPublish(beatIdentifier, midnightTimeStamp);
    portfolioBeatListener.publishBeat(interestBeat);
    Assert.assertTrue(this.eventRecorder.wait(org.apache.fineract.cn.rhythm.spi.v1.events.EventConstants.POST_PUBLISHEDBEAT,
        new BeatPublishEvent(EventConstants.DESTINATION, beatIdentifier, midnightTimeStamp)));

    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.CHECK_LATE_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));

    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));

    if (calculatedLateFee.compareTo(BigDecimal.ZERO) != 0)
      Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.MARK_LATE_INDIVIDUALLOAN_CASE,
          new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));


    final Case customerCaseAfterStateChange = portfolioManager.getCase(product.getIdentifier(), customerCase.getIdentifier());
    Assert.assertEquals(customerCaseAfterStateChange.getCurrentState(), Case.State.ACTIVE.name());

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(
        customerLoanInterestIdentifier,
        calculatedInterest.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(
        AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER,
        calculatedInterest.toPlainString()));
    AccountingFixture.verifyTransfer(
        ledgerManager,
        debtors,
        creditors,
        product.getIdentifier(),
        customerCase.getIdentifier(), Action.APPLY_INTEREST);


    if (calculatedLateFee.compareTo(BigDecimal.ZERO) != 0) {
      final Set<Debtor> lateFeeDebtors = new HashSet<>();
      lateFeeDebtors.add(new Debtor(
          customerLoanFeeIdentifier,
          calculatedLateFee.toPlainString()));
      lateFeeDebtors.add(new Debtor(
          AccountingFixture.GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER,
          provisionForLosses.toPlainString()));

      final Set<Creditor> lateFeeCreditors = new HashSet<>();
      lateFeeCreditors.add(new Creditor(
          AccountingFixture.LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER,
          calculatedLateFee.toPlainString()));
      lateFeeCreditors.add(new Creditor(
          AccountingFixture.PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER,
          provisionForLosses.toPlainString()));
      AccountingFixture.verifyTransfer(
          ledgerManager,
          lateFeeDebtors,
          lateFeeCreditors,
          product.getIdentifier(),
          customerCase.getIdentifier(),
          Action.MARK_LATE);
      lateFees = lateFees.add(calculatedLateFee);
      productLossAllowance = productLossAllowance.add(provisionForLosses);
    }
    interestAccrued = interestAccrued.add(calculatedInterest);

    updateBalanceMock();
    logger.info("Completed step6CalculateInterestAccrualAndCheckForLateness");
  }

  private void step6ICalculateInterestAndLossAllowancesForLateLoanForRangeOfDays(
      final LocalDateTime referenceDate,
      final LossProvisionStep... lossProvisionSteps) throws InterruptedException
  {
    try {
      final Map<Integer, BigDecimal> lossProvisionConfiguration = Stream.of(lossProvisionSteps)
          .collect(Collectors.toMap(LossProvisionStep::getDaysLate, LossProvisionStep::getPercentProvision));

      IntStream.rangeClosed(9, 67)
          .forEach(day -> {
            try {
              final int daysLate = day - 7;
              step6ICalculateInterestAndLossAllowancesForLateLoan(
                  referenceDate.plusDays(day),
                  daysLate,
                  lossProvisionConfiguration.get(daysLate));
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause != null && cause.getClass().isAssignableFrom(InterruptedException.class))
        throw (InterruptedException)e.getCause();
      else
        throw e;
    }
  }

  private void step6ICalculateInterestAndLossAllowancesForLateLoan(
      final LocalDateTime forDateTime,
      final int daysLate,
      final @Nullable BigDecimal percentProvision) throws InterruptedException
  {
    logger.info("step6ICalculateInterestAndLossAllowancesForLateLoan  '{}'", forDateTime);
    final String beatIdentifier = "alignment0";
    final String midnightTimeStamp = DateConverter.toIsoString(forDateTime);

    final BigDecimal dailyInterestRate = Fixture.INTEREST_RATE
        .divide(BigDecimal.valueOf(100), 8, BigDecimal.ROUND_HALF_EVEN)
        .divide(Fixture.ACCRUAL_PERIODS, 8, BigDecimal.ROUND_HALF_EVEN);

    final BigDecimal calculatedInterest = expectedCurrentPrincipal
        .multiply(dailyInterestRate)
        .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);

    final BigDecimal provisionForLosses = percentProvision == null ?
        BigDecimal.ZERO :
        expectedCurrentPrincipal.multiply(percentProvision.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_EVEN))
            .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);

    logger.info("calculatedInterest '{}'", calculatedInterest);
    logger.info("percentProvision '{}'", percentProvision);
    logger.info("provisionForLosses '{}'", provisionForLosses);


    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPLY_INTEREST,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.MARK_IN_ARREARS,
        null,
        BigDecimal.valueOf(daysLate),
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS,
        new CostComponent(ChargeIdentifiers.PROVISION_FOR_LOSSES_ID, provisionForLosses.negate()));

    final BeatPublish interestBeat = new BeatPublish(beatIdentifier, midnightTimeStamp);
    portfolioBeatListener.publishBeat(interestBeat);
    Assert.assertTrue(this.eventRecorder.wait(org.apache.fineract.cn.rhythm.spi.v1.events.EventConstants.POST_PUBLISHEDBEAT,
        new BeatPublishEvent(EventConstants.DESTINATION, beatIdentifier, midnightTimeStamp)));

    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.CHECK_LATE_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));

    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));

    if (percentProvision != null) {
      Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.MARK_IN_ARREARS_INDIVIDUALLOAN_CASE,
          new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));
    }


    final Case customerCaseAfterStateChange = portfolioManager.getCase(product.getIdentifier(), customerCase.getIdentifier());
    Assert.assertEquals(customerCaseAfterStateChange.getCurrentState(), Case.State.ACTIVE.name());

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(
        customerLoanInterestIdentifier,
        calculatedInterest.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(
        AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER,
        calculatedInterest.toPlainString()));
    AccountingFixture.verifyTransfer(
        ledgerManager,
        debtors,
        creditors,
        product.getIdentifier(),
        customerCase.getIdentifier(), Action.APPLY_INTEREST);

    if (percentProvision != null) {
      final Set<Debtor> lateFeeDebtors = new HashSet<>();
      lateFeeDebtors.add(new Debtor(
          AccountingFixture.GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER,
          provisionForLosses.toPlainString()));

      final Set<Creditor> lateFeeCreditors = new HashSet<>();
      lateFeeCreditors.add(new Creditor(
          AccountingFixture.PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER,
          provisionForLosses.toPlainString()));
      AccountingFixture.verifyTransfer(
          ledgerManager,
          lateFeeDebtors,
          lateFeeCreditors,
          product.getIdentifier(),
          customerCase.getIdentifier(),
          Action.MARK_IN_ARREARS);
      productLossAllowance = productLossAllowance.add(provisionForLosses);
    }
    interestAccrued = interestAccrued.add(calculatedInterest);

    updateBalanceMock();
    logger.info("Completed step6ICalculateInterestAndLossAllowancesForLateLoan");

  }

  private Payment step7PaybackPartialAmount(
      final BigDecimal amount,
      final LocalDateTime forDateTime,
      final BigDecimal lateFee,
      final BigDecimal balanceInEntryAccount,
      final String entryAccountIdentifier) throws InterruptedException {
    logger.info("step7PaybackPartialAmount '{}' '{}'", amount, forDateTime);
    final BigDecimal principal = amount.subtract(interestAccrued).subtract(lateFee.add(nonLateFees));

    AccountingFixture.mockBalance(entryAccountIdentifier, balanceInEntryAccount);

    final Payment payment = checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT,
        new HashSet<>(Arrays.asList(AccountDesignators.ENTRY, AccountDesignators.CUSTOMER_LOAN_GROUP, AccountDesignators.LOAN_FUNDS_SOURCE)),
        amount,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS,
        new CostComponent(ChargeIdentifiers.REPAY_PRINCIPAL_ID, principal),
        new CostComponent(ChargeIdentifiers.REPAY_INTEREST_ID, interestAccrued),
        new CostComponent(ChargeIdentifiers.REPAY_FEES_ID, lateFee.add(nonLateFees)),
        new CostComponent(ChargeIdentifiers.INTEREST_ID, interestAccrued),
        new CostComponent(ChargeIdentifiers.LATE_FEE_ID, lateFee));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT,
        forDateTime,
        assignEntry(entryAccountIdentifier),
        amount,
        IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE,
        Case.State.ACTIVE); //Close has to be done explicitly.
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.MARK_IN_ARREARS, Action.WRITE_OFF, Action.CLOSE);

    final Set<Debtor> debtors = new HashSet<>();
    BigDecimal customerDepositAccountDebit = principal;
    if (interestAccrued.compareTo(BigDecimal.ZERO) != 0) {
      customerDepositAccountDebit = customerDepositAccountDebit.add(interestAccrued);
      debtors.add(new Debtor(AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER, interestAccrued.toPlainString()));
    }
    if (lateFee.add(nonLateFees).compareTo(BigDecimal.ZERO) != 0) {
      customerDepositAccountDebit = customerDepositAccountDebit.add(lateFee.add(nonLateFees));
    }
    if (lateFee.compareTo(BigDecimal.ZERO) != 0) {
      debtors.add(new Debtor(AccountingFixture.LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER, lateFee.toPlainString()));
    }
    debtors.add(new Debtor(entryAccountIdentifier, customerDepositAccountDebit.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(customerLoanPrincipalIdentifier, principal.toPlainString()));
    if (interestAccrued.compareTo(BigDecimal.ZERO) != 0) {
      creditors.add(new Creditor(customerLoanInterestIdentifier, interestAccrued.toPlainString()));
      creditors.add(new Creditor(AccountingFixture.CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER, interestAccrued.toPlainString()));
    }
    if (lateFee.add(nonLateFees).compareTo(BigDecimal.ZERO) != 0) {
      creditors.add(new Creditor(customerLoanFeeIdentifier, lateFee.add(nonLateFees).toPlainString()));
    }
    if (lateFee.compareTo(BigDecimal.ZERO) != 0) {
      creditors.add(new Creditor(AccountingFixture.LATE_FEE_INCOME_ACCOUNT_IDENTIFIER, lateFee.toPlainString()));
    }

    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT);

    expectedCurrentPrincipal = expectedCurrentPrincipal.subtract(principal);
    interestAccrued = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
    nonLateFees = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
    lateFees = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);

    updateBalanceMock();
    logger.info("Completed step7PaybackPartialAmount");
    return payment;
  }

  private void step8Close(
      final LocalDateTime forDateTime) throws InterruptedException
  {
    logger.info("step8Close for '{}'", forDateTime);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.CLOSE,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.CLOSE,
        forDateTime,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE,
        Case.State.CLOSED); //Close has to be done explicitly.

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }

  private void step8IWriteOff(
      final LocalDateTime forDateTime) throws InterruptedException {
    logger.info("step8IWriteOff for '{}'", forDateTime);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.WRITE_OFF,
        null,
        null,
        forDateTime,
        MINOR_CURRENCY_UNIT_DIGITS,
        new CostComponent(ChargeIdentifiers.WRITE_OFF_ID, expectedCurrentPrincipal),
        new CostComponent(ChargeIdentifiers.INTEREST_ID, interestAccrued),
        new CostComponent(ChargeIdentifiers.LATE_FEE_ID, lateFees));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.WRITE_OFF,
        forDateTime,
        Collections.singletonList(assignExpenseToGeneralExpense()),
        IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE,
        Case.State.CLOSED); //Close has to be done explicitly.

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(AccountingFixture.GENERAL_EXPENSE_ACCOUNT_IDENTIFIER, expectedCurrentPrincipal.toPlainString()));
    debtors.add(new Debtor(AccountingFixture.LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER, lateFees.toPlainString()));
    debtors.add(new Debtor(AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER, interestAccrued.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(AccountingFixture.GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, expectedCurrentPrincipal.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, lateFees.add(interestAccrued).toPlainString()));

    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.WRITE_OFF);

    productLossAllowance = BigDecimal.ZERO;
    updateBalanceMock();
  }

  private void updateBalanceMock() {
    logger.info("Updating balance mocks");
    final BigDecimal allFees = lateFees.add(nonLateFees);
    AccountingFixture.mockBalance(customerLoanPrincipalIdentifier, expectedCurrentPrincipal);
    AccountingFixture.mockBalance(customerLoanFeeIdentifier, allFees);
    AccountingFixture.mockBalance(customerLoanInterestIdentifier, interestAccrued);
    AccountingFixture.mockBalance(AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER, interestAccrued);
    AccountingFixture.mockBalance(AccountingFixture.LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER, lateFees);
    AccountingFixture.mockBalance(AccountingFixture.PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, productLossAllowance.negate());
    AccountingFixture.mockBalance(AccountingFixture.GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER, productLossAllowance);
    logger.info("updated currentPrincipal '{}'", expectedCurrentPrincipal);
    logger.info("updated interestAccrued '{}'", interestAccrued);
    logger.info("updated nonLateFees '{}'", nonLateFees);
    logger.info("updated lateFees '{}'", lateFees);
    logger.info("updated productLossAllowance '{}'", productLossAllowance);
  }
}