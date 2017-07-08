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

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiFunction;

/**
 * @author Myrle Krantz
 */
@Service
public class CostComponentService {
  private static final int RUNNING_CALCULATION_PRECISION = 8;

  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final IndividualLoanService individualLoanService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public CostComponentService(
          final ProductRepository productRepository,
          final CaseRepository caseRepository,
          final CaseParametersRepository caseParametersRepository,
          final IndividualLoanService individualLoanService,
          final AccountingAdapter accountingAdapter) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.individualLoanService = individualLoanService;
    this.accountingAdapter = accountingAdapter;
  }

  public DataContextOfAction checkedGetDataContext(
          final String productIdentifier,
          final String caseIdentifier,
          final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
            productRepository.findByIdentifier(productIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
    final CaseEntity customerCase =
            caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Case not found ''{0}.{1}''.", productIdentifier, caseIdentifier));

    final CaseParameters caseParameters =
            caseParametersRepository.findByCaseId(customerCase.getId())
                    .map(x -> CaseParametersMapper.mapEntity(x, product.getMinorCurrencyUnitDigits()))
                    .orElseThrow(() -> ServiceException.notFound(
                            "Individual loan not found ''{0}.{1}''.",
                            productIdentifier, caseIdentifier));

    return new DataContextOfAction(product, customerCase, caseParameters, oneTimeAccountAssignments);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForAction(
      final Action action,
      final DataContextOfAction dataContextOfAction) {
    switch (action) {
      case OPEN:
        return getCostComponentsForOpen(dataContextOfAction);
      case APPROVE:
        return getCostComponentsForApprove(dataContextOfAction);
      case DENY:
        return getCostComponentsForDeny(dataContextOfAction);
      case DISBURSE:
        return getCostComponentsForDisburse(dataContextOfAction);
      case APPLY_INTEREST:
        return getCostComponentsForApplyInterest(dataContextOfAction);
      case ACCEPT_PAYMENT:
        return getCostComponentsForAcceptPayment(dataContextOfAction);
      case CLOSE:
        return getCostComponentsForClose(dataContextOfAction);
      case MARK_LATE:
        return getCostComponentsForMarkLate(dataContextOfAction);
      case WRITE_OFF:
        return getCostComponentsForMarkLate(dataContextOfAction);
      case RECOVER:
        return getCostComponentsForMarkLate(dataContextOfAction);
      default:
        throw ServiceException.internalError("Invalid action: ''{0}''.", action.name());
    }
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForOpen(final DataContextOfAction dataContextOfAction) {
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.OPEN, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, minorCurrencyUnitDigits, BigDecimal.ZERO, scheduledActions);

    return getCostComponentsForScheduledCharges(
            scheduledCharges,
            caseParameters.getMaximumBalance(),
            BigDecimal.ZERO,
            minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDeny(final DataContextOfAction dataContextOfAction) {
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DENY, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, minorCurrencyUnitDigits, BigDecimal.ZERO, scheduledActions);

    return getCostComponentsForScheduledCharges(
        scheduledCharges,
        caseParameters.getMaximumBalance(),
        BigDecimal.ZERO,
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApprove(final DataContextOfAction dataContextOfAction) {
    //Charge the approval fee if applicable.
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.APPROVE, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, minorCurrencyUnitDigits, BigDecimal.ZERO, scheduledActions);

    return getCostComponentsForScheduledCharges(
            scheduledCharges,
            caseParameters.getMaximumBalance(),
            BigDecimal.ZERO,
            minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDisburse(final DataContextOfAction dataContextOfAction) {
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DISBURSE, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, minorCurrencyUnitDigits, BigDecimal.ZERO, scheduledActions);

    return getCostComponentsForScheduledCharges(
            scheduledCharges,
            caseParameters.getMaximumBalance(),
            BigDecimal.ZERO,
            minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApplyInterest(final DataContextOfAction dataContextOfAction) {

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier);

    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getScheduledActionsForDisbursedLoan(LocalDate.now(), dataContextOfAction.getCustomerCase().getEndOfTerm().toLocalDate(), caseParameters, Action.APPLY_INTEREST);
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(productIdentifier, minorCurrencyUnitDigits, currentBalance, scheduledActions);

    return getCostComponentsForScheduledCharges(
            scheduledCharges,
            caseParameters.getMaximumBalance(),
            currentBalance,
            minorCurrencyUnitDigits);
  }

  private CostComponentsForRepaymentPeriod getCostComponentsForAcceptPayment(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  private CostComponentsForRepaymentPeriod getCostComponentsForMarkLate(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  public CostComponentsForRepaymentPeriod getCostComponentsForWriteOff(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  private CostComponentsForRepaymentPeriod getCostComponentsForClose(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  public CostComponentsForRepaymentPeriod getCostComponentsForRecover(final DataContextOfAction dataContextOfAction) {
    return null;
  }

  static CostComponentsForRepaymentPeriod getCostComponentsForScheduledCharges(
      final Collection<ScheduledCharge> scheduledCharges,
      final BigDecimal maximumBalance,
      final BigDecimal runningBalance,
      final int minorCurrencyUnitDigits) {
    BigDecimal balanceAdjustment = BigDecimal.ZERO;

    final Map<ChargeDefinition, CostComponent> costComponentMap = new HashMap<>();
    for (final ScheduledCharge scheduledCharge : scheduledCharges)
    {
      final CostComponent costComponent = costComponentMap
          .computeIfAbsent(scheduledCharge.getChargeDefinition(),
              chargeIdentifier -> {
                final CostComponent ret = new CostComponent();
                ret.setChargeIdentifier(scheduledCharge.getChargeDefinition().getIdentifier());
                ret.setAmount(BigDecimal.ZERO);
                return ret;
              });

      final BigDecimal chargeAmount = howToApplyScheduledChargeToBalance(scheduledCharge)
          .apply(maximumBalance, runningBalance)
          .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
      if (chargeDefinitionTouchesCustomerLoanAccount(scheduledCharge.getChargeDefinition()))
        balanceAdjustment = balanceAdjustment.add(chargeAmount);
      costComponent.setAmount(costComponent.getAmount().add(chargeAmount));
    }

    return new CostComponentsForRepaymentPeriod(
        costComponentMap,
        balanceAdjustment);
  }

  private static BiFunction<BigDecimal, BigDecimal, BigDecimal> howToApplyScheduledChargeToBalance(
      final ScheduledCharge scheduledCharge)
  {

    switch (scheduledCharge.getChargeDefinition().getChargeMethod())
    {
      case FIXED:
        return (maximumBalance, runningBalance) -> scheduledCharge.getChargeDefinition().getAmount();
      case PROPORTIONAL: {
        switch (scheduledCharge.getChargeDefinition().getProportionalTo()) {
          case ChargeIdentifiers.RUNNING_BALANCE_DESIGNATOR:
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(runningBalance);
          case ChargeIdentifiers.MAXIMUM_BALANCE_DESIGNATOR:
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(maximumBalance);
          default:
//TODO: correctly implement charges which are proportionate to other charges.
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(maximumBalance);
        }
      }
      default:
        return (maximumBalance, runningBalance) -> BigDecimal.ZERO;
    }
  }

  private static boolean chargeDefinitionTouchesCustomerLoanAccount(final ChargeDefinition chargeDefinition)
  {
    return chargeDefinition.getToAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN) ||
        chargeDefinition.getFromAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN) ||
        (chargeDefinition.getAccrualAccountDesignator() != null && chargeDefinition.getAccrualAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN));
  }
  private static LocalDate today() {
    return LocalDate.now(ZoneId.of("UTC"));
  }
}
