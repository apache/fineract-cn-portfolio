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
package io.mifos.portfolio;

import com.google.gson.Gson;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.Creditor;
import io.mifos.accounting.api.v1.domain.Debtor;
import io.mifos.core.api.util.ApiFactory;
import io.mifos.core.lang.DateConverter;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.api.v1.events.BalanceSegmentSetEvent;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.rhythm.spi.v1.client.BeatListener;
import io.mifos.rhythm.spi.v1.domain.BeatPublish;
import io.mifos.rhythm.spi.v1.events.BeatPublishEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static io.mifos.portfolio.Fixture.MINOR_CURRENCY_UNIT_DIGITS;

/**
 * @author Myrle Krantz
 */
public class TestAccountingInteractionInLoanWorkflow extends AbstractPortfolioTest {
  private static final BigDecimal PROCESSING_FEE_AMOUNT = BigDecimal.valueOf(100_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal LOAN_ORIGINATION_FEE_AMOUNT = BigDecimal.valueOf(100_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal DISBURSEMENT_FEE_LOWER_RANGE_AMOUNT = BigDecimal.valueOf(10_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final BigDecimal DISBURSEMENT_FEE_UPPER_RANGE_AMOUNT = BigDecimal.valueOf(1_00, MINOR_CURRENCY_UNIT_DIGITS);
  private static final String DISBURSEMENT_RANGES = "disbursement_ranges";
  private static final String DISBURSEMENT_LOWER_RANGE = "smaller";
  private static final String DISBURSEMENT_UPPER_RANGE = "larger";
  private static final String UPPER_RANGE_DISBURSEMENT_FEE_ID = ChargeIdentifiers.DISBURSEMENT_FEE_ID + "2";

  private BeatListener portfolioBeatListener;

  private Product product = null;
  private Case customerCase = null;
  private TaskDefinition taskDefinition = null;
  private CaseParameters caseParameters = null;
  private String pendingDisbursalAccountIdentifier = null;
  private String customerLoanAccountIdentifier = null;

  private BigDecimal expectedCurrentBalance = null;
  private BigDecimal interestAccrued = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);


  @Before
  public void prepBeatListener() {
    portfolioBeatListener = new ApiFactory(logger).create(BeatListener.class, testEnvironment.serverURI());
  }

  @Test
  public void workflowTerminatingInApplicationDenial() throws InterruptedException {
    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4DenyCase();
  }

  @Test
  public void workflowTerminatingInEarlyLoanPayoff() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS));
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), null);
    step7PaybackPartialAmount(expectedCurrentBalance, today, 0, BigDecimal.ZERO);
    step8Close();
  }

  @Test
  public void workflowWithTwoUnequalDisbursals() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    step5Disburse(
        BigDecimal.valueOf(500_00, MINOR_CURRENCY_UNIT_DIGITS),
        ChargeIdentifiers.DISBURSEMENT_FEE_ID, BigDecimal.valueOf(10_00, MINOR_CURRENCY_UNIT_DIGITS));
    step5Disburse(
        BigDecimal.valueOf(1_500_00, MINOR_CURRENCY_UNIT_DIGITS),
        UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.valueOf(15_00, MINOR_CURRENCY_UNIT_DIGITS));
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), null);
    step7PaybackPartialAmount(expectedCurrentBalance, today, 0, BigDecimal.ZERO);
    step8Close();
  }

  @Test
  public void workflowWithTwoNearlyEqualRepayments() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS));
    step6CalculateInterestAccrualAndCheckForLateness(midnightToday(), null);
    final BigDecimal repayment1 = expectedCurrentBalance.divide(BigDecimal.valueOf(2), BigDecimal.ROUND_HALF_EVEN);
    step7PaybackPartialAmount(
        repayment1.setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN),
        today,
        0, BigDecimal.ZERO);
    step7PaybackPartialAmount(expectedCurrentBalance, today, 0, BigDecimal.ZERO);
    step8Close();
  }

  @Test
  public void workflowWithNegativePaymentSize() throws InterruptedException {
    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    try {
      step5Disburse(BigDecimal.valueOf(-2).setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN),
          UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN));
      Assert.fail("Expected an IllegalArgumentException.");
    }
    catch (IllegalArgumentException ignored) { }
  }

  @Test
  public void workflowWithNormalRepayment() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS));

    int week = 0;
    final List<BigDecimal> repayments = new ArrayList<>();
    while (expectedCurrentBalance.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Simulating week {}. Expected current balance {}.", week, expectedCurrentBalance);
      step6CalculateInterestAndCheckForLatenessForWeek(today, week);
      final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today, (week+1)*7);
      repayments.add(nextRepaymentAmount);
      step7PaybackPartialAmount(nextRepaymentAmount, today, (week+1)*7, BigDecimal.ZERO);
      week++;
    }

    final BigDecimal minPayment = repayments.stream().min(BigDecimal::compareTo).orElseThrow(IllegalStateException::new);
    final BigDecimal maxPayment = repayments.stream().max(BigDecimal::compareTo).orElseThrow(IllegalStateException::new);
    final BigDecimal delta = maxPayment.subtract(minPayment).abs();
    Assert.assertTrue("Payments are " + repayments,
        delta.divide(maxPayment, BigDecimal.ROUND_HALF_EVEN).compareTo(BigDecimal.valueOf(0.01)) <= 0);


    step8Close();
  }

  @Test
  public void workflowWithOneLateRepayment() throws InterruptedException {
    final LocalDateTime today = midnightToday();

    step1CreateProduct();
    step2CreateCase();
    step3OpenCase();
    step4ApproveCase();
    step5Disburse(
        BigDecimal.valueOf(2_000_00, MINOR_CURRENCY_UNIT_DIGITS),
        UPPER_RANGE_DISBURSEMENT_FEE_ID, BigDecimal.valueOf(20_00, MINOR_CURRENCY_UNIT_DIGITS));

    int week = 0;
    final int weekOfLateRepayment = 3;
    final List<BigDecimal> repayments = new ArrayList<>();
    while (expectedCurrentBalance.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Simulating week {}. Expected current balance {}.", week, expectedCurrentBalance);
      if (week == weekOfLateRepayment) {
        final BigDecimal lateFee = BigDecimal.valueOf(14_49, MINOR_CURRENCY_UNIT_DIGITS);
        step6CalculateInterestAndCheckForLatenessForRangeOfDays(
            today,
            (week * 7) + 1,
            (week + 1) * 7 + 2,
            8,
            lateFee);
        final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today, (week + 1) * 7 + 2);
        repayments.add(nextRepaymentAmount);
        step7PaybackPartialAmount(nextRepaymentAmount, today, (week + 1) * 7 + 2, lateFee);
      }
      else {
        step6CalculateInterestAndCheckForLatenessForWeek(today, week);
        final BigDecimal nextRepaymentAmount = findNextRepaymentAmount(today, (week + 1) * 7);
        repayments.add(nextRepaymentAmount);
        step7PaybackPartialAmount(nextRepaymentAmount, today, (week + 1) * 7, BigDecimal.ZERO);
      }
      week++;
    }

    repayments.remove(3);

    final BigDecimal minPayment = repayments.stream().min(BigDecimal::compareTo).orElseThrow(IllegalStateException::new);
    final BigDecimal maxPayment = repayments.stream().max(BigDecimal::compareTo).orElseThrow(IllegalStateException::new);
    final BigDecimal delta = maxPayment.subtract(minPayment).abs();
    Assert.assertTrue("Payments are " + repayments,
        delta.divide(maxPayment, BigDecimal.ROUND_HALF_EVEN).compareTo(BigDecimal.valueOf(0.01)) <= 0);


    step8Close();
  }

  private BigDecimal findNextRepaymentAmount(
      final LocalDateTime referenceDate,
      final int dayNumber) {
    AccountingFixture.mockBalance(customerLoanAccountIdentifier, expectedCurrentBalance.negate());

    final List<CostComponent> costComponentsForNextPayment = portfolioManager.getCostComponentsForAction(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT.name(),
        null,
        null,
        DateConverter.toIsoString(referenceDate.plusDays(dayNumber)));
    return costComponentsForNextPayment.stream().filter(x -> x.getChargeIdentifier().equals(ChargeIdentifiers.REPAYMENT_ID)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("return missing repayment charge."))
        .getAmount();
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
    lowerRangeDisbursementFeeChargeDefinition.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR.getValue());
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
    upperRangeDisbursementFeeChargeDefinition.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR.getValue());
    upperRangeDisbursementFeeChargeDefinition.setForSegmentSet(DISBURSEMENT_RANGES);
    upperRangeDisbursementFeeChargeDefinition.setFromSegment(DISBURSEMENT_UPPER_RANGE);
    upperRangeDisbursementFeeChargeDefinition.setToSegment(DISBURSEMENT_UPPER_RANGE);

    portfolioManager.createChargeDefinition(product.getIdentifier(), upperRangeDisbursementFeeChargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), UPPER_RANGE_DISBURSEMENT_FEE_ID)));

    taskDefinition = createTaskDefinition(product);

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));
  }

  private void step2CreateCase() throws InterruptedException {
    logger.info("step2CreateCase");
    caseParameters = Fixture.createAdjustedCaseParameters(x ->
      x.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 1, null, null, null))
    );
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    customerCase = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);
  }

  //Open the case and accept a processing fee.
  private void step3OpenCase() throws InterruptedException {
    logger.info("step3OpenCase");
    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        Collections.singleton(AccountDesignators.ENTRY),
        null,
        new CostComponent(ChargeIdentifiers.PROCESSING_FEE_ID, PROCESSING_FEE_AMOUNT));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        Collections.singletonList(assignEntryToTeller()),
        IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE,
        Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);

    AccountingFixture.verifyTransfer(ledgerManager,
        AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER, AccountingFixture.PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER,
        PROCESSING_FEE_AMOUNT, product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN
    );
  }


  //Deny the case. Once this is done, no more actions are possible for the case.
  private void step4DenyCase() throws InterruptedException {
    logger.info("step4DenyCase");
    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DENY,
        Collections.singleton(AccountDesignators.ENTRY),
        null);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DENY,
        Collections.singletonList(assignEntryToTeller()),
        IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE,
        Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }


  //Approve the case, accept a loan origination fee, and prepare to disburse the loan by earmarking the funds.
  private void step4ApproveCase() throws InterruptedException {
    logger.info("step4ApproveCase");

    markTaskExecuted(product, customerCase, taskDefinition);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPROVE,
        Collections.singleton(AccountDesignators.ENTRY),
        null,
        new CostComponent(ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID, LOAN_ORIGINATION_FEE_AMOUNT));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPROVE,
        Collections.singletonList(assignEntryToTeller()),
        IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE,
        Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);

    pendingDisbursalAccountIdentifier =
        AccountingFixture.verifyAccountCreation(ledgerManager, AccountingFixture.PENDING_DISBURSAL_LEDGER_IDENTIFIER, AccountType.ASSET);
    customerLoanAccountIdentifier =
        AccountingFixture.verifyAccountCreation(ledgerManager, AccountingFixture.CUSTOMER_LOAN_LEDGER_IDENTIFIER, AccountType.ASSET);

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(AccountingFixture.LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER, caseParameters.getMaximumBalance().toPlainString()));
    debtors.add(new Debtor(AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER, LOAN_ORIGINATION_FEE_AMOUNT.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(pendingDisbursalAccountIdentifier, caseParameters.getMaximumBalance().toPlainString()));
    creditors.add(new Creditor(AccountingFixture.LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER, LOAN_ORIGINATION_FEE_AMOUNT.toPlainString()));
    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE);

    expectedCurrentBalance = BigDecimal.ZERO;
  }

  //Approve the case, accept a loan origination fee, and prepare to disburse the loan by earmarking the funds.
  private void step5Disburse(
      final BigDecimal amount,
      final String whichDisbursementFee,
      final BigDecimal disbursementFeeAmount) throws InterruptedException {
    logger.info("step5Disburse");
    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE,
        Collections.singleton(AccountDesignators.ENTRY),
        amount, new CostComponent(whichDisbursementFee, disbursementFeeAmount),
        new CostComponent(ChargeIdentifiers.DISBURSE_PAYMENT_ID, amount));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE,
        LocalDateTime.now(Clock.systemUTC()),
        Collections.singletonList(assignEntryToTeller()),
        amount,
        IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE,
        midnightToday(),
        Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPLY_INTEREST,
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);


    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(pendingDisbursalAccountIdentifier, amount.toPlainString()));
    debtors.add(new Debtor(AccountingFixture.LOANS_PAYABLE_ACCOUNT_IDENTIFIER, amount.toPlainString()));
    debtors.add(new Debtor(AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER, disbursementFeeAmount.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(customerLoanAccountIdentifier, amount.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER, amount.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER, disbursementFeeAmount.toPlainString()));
    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE);

    expectedCurrentBalance = expectedCurrentBalance.add(amount);
  }

  private void step6CalculateInterestAndCheckForLatenessForWeek(
      final LocalDateTime referenceDate,
      final int weekNumber) throws InterruptedException {
    step6CalculateInterestAndCheckForLatenessForRangeOfDays(
        referenceDate,
        (weekNumber * 7) + 1,
        (weekNumber + 1) * 7,
        -1,
        null);
  }

  private void step6CalculateInterestAndCheckForLatenessForRangeOfDays(
      final LocalDateTime referenceDate,
      final int startInclusive,
      final int endInclusive,
      final int dayOfLateFee,
      final BigDecimal calculatedLateFee) throws InterruptedException {
    try {
      IntStream.rangeClosed(startInclusive, endInclusive)
          .mapToObj(referenceDate::plusDays)
          .forEach(day -> {
            try {
              if (day.equals(referenceDate.plusDays(dayOfLateFee))) {
                step6CalculateInterestAccrualAndCheckForLateness(day, calculatedLateFee);
              }
              else {
                step6CalculateInterestAccrualAndCheckForLateness(day, null);
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
      final LocalDateTime forTime,
      final BigDecimal calculatedLateFee) throws InterruptedException {
    logger.info("step6CalculateInterestAccrualAndCheckForLateness");
    final String beatIdentifier = "alignment0";
    final String midnightTimeStamp = DateConverter.toIsoString(forTime);

    AccountingFixture.mockBalance(customerLoanAccountIdentifier, expectedCurrentBalance.negate());

    final BigDecimal calculatedInterest = expectedCurrentBalance.multiply(Fixture.INTEREST_RATE.divide(Fixture.ACCRUAL_PERIODS, 8, BigDecimal.ROUND_HALF_EVEN))
        .setScale(MINOR_CURRENCY_UNIT_DIGITS, BigDecimal.ROUND_HALF_EVEN);


    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPLY_INTEREST,
        Collections.singleton(AccountDesignators.CUSTOMER_LOAN),
        null,
        new CostComponent(ChargeIdentifiers.INTEREST_ID, calculatedInterest));

    if (calculatedLateFee != null) {
      checkCostComponentForActionCorrect(
          product.getIdentifier(),
          customerCase.getIdentifier(),
          Action.MARK_LATE,
          Collections.singleton(AccountDesignators.CUSTOMER_LOAN),
          null,
          new CostComponent(ChargeIdentifiers.LATE_FEE_ID, calculatedLateFee));
    }
    final BeatPublish interestBeat = new BeatPublish(beatIdentifier, midnightTimeStamp);
    portfolioBeatListener.publishBeat(interestBeat);
    Assert.assertTrue(this.eventRecorder.wait(io.mifos.rhythm.spi.v1.events.EventConstants.POST_PUBLISHEDBEAT,
        new BeatPublishEvent(EventConstants.DESTINATION, beatIdentifier, midnightTimeStamp)));

    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.CHECK_LATE_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));

    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE,
        new IndividualLoanCommandEvent(product.getIdentifier(), customerCase.getIdentifier(), midnightTimeStamp)));


    final Case customerCaseAfterStateChange = portfolioManager.getCase(product.getIdentifier(), customerCase.getIdentifier());
    Assert.assertEquals(customerCaseAfterStateChange.getCurrentState(), Case.State.ACTIVE.name());


    interestAccrued = interestAccrued.add(calculatedInterest);

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(
        customerLoanAccountIdentifier,
        calculatedInterest.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(
        AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER,
        calculatedInterest.toPlainString()));
    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.APPLY_INTEREST);

    expectedCurrentBalance = expectedCurrentBalance.add(calculatedInterest);
  }

  private void step7PaybackPartialAmount(
      final BigDecimal amount,
      final LocalDateTime referenceDate,
      final int dayNumber,
      final BigDecimal lateFee) throws InterruptedException {
    logger.info("step7PaybackPartialAmount '{}'", amount);

    AccountingFixture.mockBalance(customerLoanAccountIdentifier, expectedCurrentBalance.negate());

    final BigDecimal principal = amount.subtract(interestAccrued).subtract(lateFee);

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT,
        new HashSet<>(Arrays.asList(AccountDesignators.ENTRY, AccountDesignators.CUSTOMER_LOAN, AccountDesignators.LOAN_FUNDS_SOURCE)),
        amount,
        new CostComponent(ChargeIdentifiers.REPAYMENT_ID, amount),
        new CostComponent(ChargeIdentifiers.TRACK_RETURN_PRINCIPAL_ID, principal),
        new CostComponent(ChargeIdentifiers.INTEREST_ID, interestAccrued),
        new CostComponent(ChargeIdentifiers.LATE_FEE_ID, lateFee));
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.ACCEPT_PAYMENT,
        referenceDate.plusDays(dayNumber),
        Collections.singletonList(assignEntryToTeller()),
        amount,
        IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE,
        midnightToday(),
        Case.State.ACTIVE); //Close has to be done explicitly.
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPLY_INTEREST,
        Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);

    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor(customerLoanAccountIdentifier, amount.toPlainString()));
    debtors.add(new Debtor(AccountingFixture.LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER, principal.toPlainString()));
    if (interestAccrued.compareTo(BigDecimal.ZERO) != 0)
      debtors.add(new Debtor(AccountingFixture.LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER, interestAccrued.toPlainString()));
    if (lateFee.compareTo(BigDecimal.ZERO) != 0)
      debtors.add(new Debtor(AccountingFixture.LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER, lateFee.toPlainString()));

    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor(AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER, amount.toPlainString()));
    creditors.add(new Creditor(AccountingFixture.LOANS_PAYABLE_ACCOUNT_IDENTIFIER, principal.toPlainString()));
    if (interestAccrued.compareTo(BigDecimal.ZERO) != 0)
      creditors.add(new Creditor(AccountingFixture.CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER, interestAccrued.toPlainString()));
    if (lateFee.compareTo(BigDecimal.ZERO) != 0)
      creditors.add(new Creditor(AccountingFixture.LATE_FEE_INCOME_ACCOUNT_IDENTIFIER, lateFee.toPlainString()));

    AccountingFixture.verifyTransfer(ledgerManager, debtors, creditors, product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT);

    expectedCurrentBalance = expectedCurrentBalance.subtract(amount).add(lateFee);
    interestAccrued = BigDecimal.ZERO.setScale(MINOR_CURRENCY_UNIT_DIGITS, RoundingMode.HALF_EVEN);
  }

  private void step8Close() throws InterruptedException {
    logger.info("step8Close");

    AccountingFixture.mockBalance(customerLoanAccountIdentifier, expectedCurrentBalance.negate());

    checkCostComponentForActionCorrect(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.CLOSE,
        Collections.singleton(AccountDesignators.ENTRY),
        null);
    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.CLOSE,
        Collections.singletonList(assignEntryToTeller()),
        IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE,
        Case.State.CLOSED); //Close has to be done explicitly.

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }
}