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
package io.mifos.individuallending;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import io.mifos.core.lang.ServiceException;
import io.mifos.customer.api.v1.client.CustomerManager;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseCreditWorthinessFactorEntity;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.individuallending.internal.repository.CreditWorthinessFactorType;
import io.mifos.individuallending.internal.service.CostComponentService;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.DataContextService;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Pattern;
import io.mifos.portfolio.service.ServiceConstants;
import io.mifos.products.spi.PatternFactory;
import io.mifos.products.spi.ProductCommandDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.*;
import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
public class IndividualLendingPatternFactory implements PatternFactory {
  final static private String INDIVIDUAL_LENDING_PACKAGE = "io.mifos.individuallending.api.v1";
  private final CaseParametersRepository caseParametersRepository;
  private final DataContextService dataContextService;
  private final CostComponentService costComponentService;
  private final CustomerManager customerManager;
  private final IndividualLendingCommandDispatcher individualLendingCommandDispatcher;
  private final Gson gson;

  @Autowired
  IndividualLendingPatternFactory(
      final CaseParametersRepository caseParametersRepository,
      final DataContextService dataContextService,
      final CostComponentService costComponentService,
      final CustomerManager customerManager,
      final IndividualLendingCommandDispatcher individualLendingCommandDispatcher,
      @Qualifier(ServiceConstants.GSON_NAME) final Gson gson)
  {
    this.caseParametersRepository = caseParametersRepository;
    this.dataContextService = dataContextService;
    this.costComponentService = costComponentService;
    this.customerManager = customerManager;
    this.individualLendingCommandDispatcher = individualLendingCommandDispatcher;
    this.gson = gson;
  }

  @Override
  public Pattern pattern() {

    final Set<String> individualLendingRequiredAccounts = new HashSet<>();
    individualLendingRequiredAccounts.add(CUSTOMER_LOAN);
    individualLendingRequiredAccounts.add(PENDING_DISBURSAL);
    individualLendingRequiredAccounts.add(LOAN_FUNDS_SOURCE);
    individualLendingRequiredAccounts.add(LOAN_FUNDS_SOURCE);
    individualLendingRequiredAccounts.add(PROCESSING_FEE_INCOME);
    individualLendingRequiredAccounts.add(ORIGINATION_FEE_INCOME);
    individualLendingRequiredAccounts.add(DISBURSEMENT_FEE_INCOME);
    individualLendingRequiredAccounts.add(INTEREST_INCOME);
    individualLendingRequiredAccounts.add(INTEREST_ACCRUAL);
    individualLendingRequiredAccounts.add(LATE_FEE_INCOME);
    individualLendingRequiredAccounts.add(LATE_FEE_ACCRUAL);
    individualLendingRequiredAccounts.add(ARREARS_ALLOWANCE);
    individualLendingRequiredAccounts.add(ENTRY);
    return new Pattern(INDIVIDUAL_LENDING_PACKAGE, individualLendingRequiredAccounts);
  }

  @Override
  public List<ChargeDefinition> charges() {
    return defaultIndividualLoanCharges();
  }

  public static List<ChargeDefinition> defaultIndividualLoanCharges() {
    final List<ChargeDefinition> ret = new ArrayList<>();
    final ChargeDefinition processingFee = charge(
            PROCESSING_FEE_NAME,
            Action.OPEN,
            BigDecimal.valueOf(0.01),
            ENTRY,
            PROCESSING_FEE_INCOME);
    processingFee.setReadOnly(false);

    final ChargeDefinition loanOriginationFee = charge(
            LOAN_ORIGINATION_FEE_NAME,
            Action.APPROVE,
            BigDecimal.valueOf(0.01),
            ENTRY,
            ORIGINATION_FEE_INCOME);
    loanOriginationFee.setReadOnly(false);

    final ChargeDefinition loanFundsAllocation = charge(
            LOAN_FUNDS_ALLOCATION_ID,
            Action.APPROVE,
            BigDecimal.valueOf(1.00),
            LOAN_FUNDS_SOURCE,
            PENDING_DISBURSAL);
    loanFundsAllocation.setReadOnly(true);

    final ChargeDefinition disbursementFee = charge(
            DISBURSEMENT_FEE_NAME,
            Action.DISBURSE,
            BigDecimal.valueOf(0.001),
            ENTRY,
            DISBURSEMENT_FEE_INCOME);
    disbursementFee.setReadOnly(false);

    final ChargeDefinition disbursePayment = new ChargeDefinition();
    disbursePayment.setChargeAction(Action.DISBURSE.name());
    disbursePayment.setIdentifier(DISBURSE_PAYMENT_ID);
    disbursePayment.setName(DISBURSE_PAYMENT_NAME);
    disbursePayment.setDescription(DISBURSE_PAYMENT_NAME);
    disbursePayment.setFromAccountDesignator(LOANS_PAYABLE);
    disbursePayment.setToAccountDesignator(ENTRY);
    disbursePayment.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR.getValue());
    disbursePayment.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    disbursePayment.setAmount(BigDecimal.ONE);
    disbursePayment.setReadOnly(true);

    final ChargeDefinition trackPrincipalDisbursePayment = new ChargeDefinition();
    trackPrincipalDisbursePayment.setChargeAction(Action.DISBURSE.name());
    trackPrincipalDisbursePayment.setIdentifier(TRACK_DISBURSAL_PAYMENT_ID);
    trackPrincipalDisbursePayment.setName(TRACK_DISBURSAL_PAYMENT_NAME);
    trackPrincipalDisbursePayment.setDescription(TRACK_DISBURSAL_PAYMENT_NAME);
    trackPrincipalDisbursePayment.setFromAccountDesignator(PENDING_DISBURSAL);
    trackPrincipalDisbursePayment.setToAccountDesignator(CUSTOMER_LOAN);
    trackPrincipalDisbursePayment.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR.getValue());
    trackPrincipalDisbursePayment.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    trackPrincipalDisbursePayment.setAmount(BigDecimal.ONE);
    trackPrincipalDisbursePayment.setReadOnly(true);

    //TODO: Make payable at time of ACCEPT_PAYMENT but accrued at MARK_LATE
    final ChargeDefinition lateFee = charge(
            LATE_FEE_NAME,
            Action.MARK_LATE,
            BigDecimal.valueOf(0.01),
            CUSTOMER_LOAN,
            LATE_FEE_INCOME);
    lateFee.setAccrueAction(Action.MARK_LATE.name());
    lateFee.setAccrualAccountDesignator(LATE_FEE_ACCRUAL);
    lateFee.setProportionalTo(ChargeIdentifiers.REPAYMENT_ID);
    lateFee.setReadOnly(false);

    //TODO: Make multiple write off allowance charges.
    final ChargeDefinition writeOffAllowanceCharge = charge(
            ALLOW_FOR_WRITE_OFF_NAME,
            Action.MARK_LATE,
            BigDecimal.valueOf(0.30),
            PENDING_DISBURSAL,
            ARREARS_ALLOWANCE);
    writeOffAllowanceCharge.setProportionalTo(ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR.getValue());
    writeOffAllowanceCharge.setReadOnly(true);

    final ChargeDefinition interestCharge = charge(
        INTEREST_NAME,
        Action.ACCEPT_PAYMENT,
        BigDecimal.ONE,
        CUSTOMER_LOAN,
        INTEREST_INCOME);
    interestCharge.setForCycleSizeUnit(ChronoUnit.YEARS);
    interestCharge.setAccrueAction(Action.APPLY_INTEREST.name());
    interestCharge.setAccrualAccountDesignator(INTEREST_ACCRUAL);
    interestCharge.setProportionalTo(ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR.getValue());
    interestCharge.setChargeMethod(ChargeDefinition.ChargeMethod.INTEREST);
    interestCharge.setReadOnly(true);

    final ChargeDefinition customerRepaymentCharge = new ChargeDefinition();
    customerRepaymentCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    customerRepaymentCharge.setIdentifier(REPAYMENT_ID);
    customerRepaymentCharge.setName(REPAYMENT_NAME);
    customerRepaymentCharge.setDescription(REPAYMENT_NAME);
    customerRepaymentCharge.setFromAccountDesignator(CUSTOMER_LOAN);
    customerRepaymentCharge.setToAccountDesignator(ENTRY);
    customerRepaymentCharge.setProportionalTo(ChargeProportionalDesignator.REPAYMENT_DESIGNATOR.getValue());
    customerRepaymentCharge.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    customerRepaymentCharge.setAmount(BigDecimal.ONE);
    customerRepaymentCharge.setReadOnly(true);

    final ChargeDefinition trackReturnPrincipalCharge = new ChargeDefinition();
    trackReturnPrincipalCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    trackReturnPrincipalCharge.setIdentifier(TRACK_RETURN_PRINCIPAL_ID);
    trackReturnPrincipalCharge.setName(TRACK_RETURN_PRINCIPAL_NAME);
    trackReturnPrincipalCharge.setDescription(TRACK_RETURN_PRINCIPAL_NAME);
    trackReturnPrincipalCharge.setFromAccountDesignator(LOAN_FUNDS_SOURCE);
    trackReturnPrincipalCharge.setToAccountDesignator(LOANS_PAYABLE);
    trackReturnPrincipalCharge.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_ADJUSTMENT_DESIGNATOR.getValue());
    trackReturnPrincipalCharge.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    trackReturnPrincipalCharge.setAmount(BigDecimal.ONE);
    trackReturnPrincipalCharge.setReadOnly(true);

    final ChargeDefinition disbursementReturnCharge = charge(
            RETURN_DISBURSEMENT_NAME,
            Action.CLOSE,
            BigDecimal.valueOf(1.0),
            PENDING_DISBURSAL,
            LOAN_FUNDS_SOURCE);
    disbursementReturnCharge.setProportionalTo(ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR.getValue());
    disbursementReturnCharge.setReadOnly(true);

    ret.add(processingFee);
    ret.add(loanOriginationFee);
    ret.add(loanFundsAllocation);
    ret.add(disbursementFee);
    ret.add(disbursePayment);
    ret.add(trackPrincipalDisbursePayment);
    ret.add(lateFee);
    ret.add(writeOffAllowanceCharge);
    ret.add(interestCharge);
    ret.add(customerRepaymentCharge);
    ret.add(trackReturnPrincipalCharge);
    ret.add(disbursementReturnCharge);

    return ret;
  }

  @Override
  public void checkParameters(final String parameters) {
    final CaseParameters caseParameters = gson.fromJson(parameters, CaseParameters.class);
    final String customerIdentifier = caseParameters.getCustomerIdentifier();
    if (!customerManager.isCustomerInGoodStanding(customerIdentifier))
      throw ServiceException.badRequest("Customer ''{0}'' is either not a customer or is not in good standing.");
  }

  @Transactional
  @Override
  public void persistParameters(final Long caseId, final String parameters) {
    checkParameters(parameters);
    final CaseParameters caseParameters = gson.fromJson(parameters, CaseParameters.class);
    final CaseParametersEntity caseParametersEntity = CaseParametersMapper.map(caseId, caseParameters);
    caseParametersRepository.save(caseParametersEntity);
  }

  private static class CaseCreditWorthinessFactorUniquenessCriteria {
    String customerId;
    CreditWorthinessFactorType factorType;
    int position;

    CaseCreditWorthinessFactorUniquenessCriteria(final CaseCreditWorthinessFactorEntity entity) {
      this.customerId = entity.getCustomerIdentifier();
      this.factorType = entity.getFactorType();
      this.position = entity.getPositionInFactor();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CaseCreditWorthinessFactorUniquenessCriteria that = (CaseCreditWorthinessFactorUniquenessCriteria) o;
      return position == that.position &&
              Objects.equals(customerId, that.customerId) &&
              factorType == that.factorType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(customerId, factorType, position);
    }
  }

  @Transactional
  @Override
  public void changeParameters(final Long caseId, final String parameters) {
    checkParameters(parameters);
    final CaseParameters caseParameters = gson.fromJson(parameters, CaseParameters.class);
    final CaseParametersEntity oldCaseParameters = caseParametersRepository.findByCaseId(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case id does not represent an individual loan: " + caseId));

    oldCaseParameters.setCustomerIdentifier(caseParameters.getCustomerIdentifier());
    oldCaseParameters.setBalanceRangeMaximum(caseParameters.getMaximumBalance());
    oldCaseParameters.setTermRangeTemporalUnit(caseParameters.getTermRange().getTemporalUnit());
    oldCaseParameters.setTermRangeMinimum(0);
    oldCaseParameters.setTermRangeMaximum(caseParameters.getTermRange().getMaximum());
    oldCaseParameters.setPaymentCycleTemporalUnit(caseParameters.getPaymentCycle().getTemporalUnit());
    oldCaseParameters.setPaymentCyclePeriod(caseParameters.getPaymentCycle().getPeriod());
    oldCaseParameters.setPaymentCycleAlignmentDay(caseParameters.getPaymentCycle().getAlignmentDay());
    oldCaseParameters.setPaymentCycleAlignmentWeek(caseParameters.getPaymentCycle().getAlignmentWeek());
    oldCaseParameters.setPaymentCycleAlignmentMonth(caseParameters.getPaymentCycle().getAlignmentMonth());



    final Set<CaseCreditWorthinessFactorEntity> oldCreditWorthinessFactorEntities = oldCaseParameters.getCreditWorthinessFactors();
    final Map<CaseCreditWorthinessFactorUniquenessCriteria, CaseCreditWorthinessFactorEntity> forFindingThings = oldCreditWorthinessFactorEntities.stream()
            .collect(Collectors.toMap(CaseCreditWorthinessFactorUniquenessCriteria::new, x -> x));

    final Set<CaseCreditWorthinessFactorEntity> newCreditWorthinessFactorEntities = CaseParametersMapper.mapSnapshotsToFactors(caseParameters.getCreditWorthinessSnapshots(),oldCaseParameters);
    newCreditWorthinessFactorEntities.forEach(x -> {
      final CaseCreditWorthinessFactorEntity existingThing = forFindingThings.get(new CaseCreditWorthinessFactorUniquenessCriteria(x));
      if (existingThing != null) x.setId(existingThing.getId());
    });
    oldCaseParameters.getCreditWorthinessFactors().clear();
    oldCaseParameters.getCreditWorthinessFactors().addAll(newCreditWorthinessFactorEntities);

    caseParametersRepository.save(oldCaseParameters);
  }

  @Override
  public Optional<String> getParameters(final Long caseId, final int minorCurrencyUnitDigits) {
    return caseParametersRepository
            .findByCaseId(caseId)
            .map(x -> CaseParametersMapper.mapEntity(x, minorCurrencyUnitDigits))
            .map(gson::toJson);
  }

  @Override
  public Set<String> getNextActionsForState(final Case.State state) {
    return getAllowedNextActionsForState(state).stream().map(Enum::name).collect(Collectors.toSet());
  }

  @Override
  public List<CostComponent> getCostComponentsForAction(
      final String productIdentifier,
      final String caseIdentifier,
      final String actionIdentifier,
      final Set<String> forAccountDesignators,
      final BigDecimal forPaymentSize) {
    final Action action = Action.valueOf(actionIdentifier);
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(productIdentifier, caseIdentifier, Collections.emptyList());
    final Case.State caseState = Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState());
    checkActionCanBeExecuted(caseState, action);

    return costComponentService.getCostComponentsForAction(action, dataContextOfAction, forPaymentSize)
        .stream()
        .filter(costComponentEntry -> chargeReferencesAccountDesignators(costComponentEntry.getKey(), action, forAccountDesignators))
        .map(costComponentEntry -> new CostComponent(costComponentEntry.getKey().getIdentifier(), costComponentEntry.getValue().getAmount()))
        .collect(Collectors.toList())
            .stream()
            .map(x -> new CostComponent(x.getChargeIdentifier(), x.getAmount()))
            .collect(Collectors.toList());
  }

  private boolean chargeReferencesAccountDesignators(
      final ChargeDefinition chargeDefinition,
      final Action action,
      final Set<String> forAccountDesignators) {
    final Set<String> accountsToCompare = Sets.newHashSet(
        chargeDefinition.getFromAccountDesignator(),
        chargeDefinition.getToAccountDesignator()
    );
    if (chargeDefinition.getAccrualAccountDesignator() != null)
      accountsToCompare.add(chargeDefinition.getAccrualAccountDesignator());

    return !Sets.intersection(accountsToCompare, forAccountDesignators).isEmpty();
  }

  public static void checkActionCanBeExecuted(final Case.State state, final Action action) {
    if (!getAllowedNextActionsForState(state).contains(action))
      throw ServiceException.badRequest("Cannot call action {0} from state {1}", action.name(), state.name());
  }

  private static Set<Action> getAllowedNextActionsForState(final Case.State state) {
    switch (state)
    {
      case CREATED:
        //noinspection ArraysAsListWithZeroOrOneArgument
        return new HashSet<>(Arrays.asList(Action.OPEN));
      case PENDING:
        return new HashSet<>(Arrays.asList(Action.DENY, Action.APPROVE));
      case APPROVED:
        return new HashSet<>(Arrays.asList(Action.DISBURSE, Action.CLOSE));
      case ACTIVE:
        return new HashSet<>(Arrays.asList(Action.CLOSE, Action.ACCEPT_PAYMENT, Action.MARK_LATE, Action.APPLY_INTEREST, Action.DISBURSE, Action.WRITE_OFF));
      case CLOSED:
        return Collections.emptySet();
      default:
        return Collections.emptySet();
    }
  }

  public ProductCommandDispatcher getIndividualLendingCommandDispatcher() {
    return this.individualLendingCommandDispatcher;
  }

  private static ChargeDefinition charge(
          final String name,
          final Action action,
          final BigDecimal defaultAmount,
          final String fromAccount,
          final String toAccount)
  {
    final ChargeDefinition ret = new ChargeDefinition();

    ret.setIdentifier(name.toLowerCase(Locale.US).replace(" ", "-"));
    ret.setName(name);
    ret.setDescription(name);
    ret.setChargeAction(action.name());
    ret.setAmount(defaultAmount);
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    ret.setProportionalTo(ChargeProportionalDesignator.MAXIMUM_BALANCE_DESIGNATOR.getValue());
    ret.setFromAccountDesignator(fromAccount);
    ret.setToAccountDesignator(toAccount);

    return ret;
  }
}
