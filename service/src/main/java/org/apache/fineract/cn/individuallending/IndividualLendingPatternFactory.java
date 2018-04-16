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
package org.apache.fineract.cn.individuallending;

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.mapper.CaseParametersMapper;
import org.apache.fineract.cn.individuallending.internal.repository.CaseCreditWorthinessFactorEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersRepository;
import org.apache.fineract.cn.individuallending.internal.repository.CreditWorthinessFactorType;
import org.apache.fineract.cn.individuallending.internal.service.ChargeDefinitionService;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DataContextService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.AcceptPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ApplyInterestPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ApprovePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ClosePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.DenyPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.DisbursePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.MarkInArrearsPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.MarkLatePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.OpenPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.PaymentBuilder;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.PaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.RealRunningBalances;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.RecoverPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.WriteOffPaymentBuilderService;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Pattern;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.api.v1.domain.RequiredAccountAssignment;
import org.apache.fineract.cn.portfolio.service.ServiceConstants;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import org.apache.fineract.cn.products.spi.PatternFactory;
import org.apache.fineract.cn.products.spi.ProductCommandDispatcher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.customer.api.v1.client.CustomerManager;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
public class IndividualLendingPatternFactory implements PatternFactory {
  final static private String INDIVIDUAL_LENDING_PACKAGE = "org.apache.fineract.cn.individuallending.api.v1";
  final static private Pattern INDIVIDUAL_LENDING_PATTERN;

  static {
    INDIVIDUAL_LENDING_PATTERN = new Pattern();
    INDIVIDUAL_LENDING_PATTERN.setParameterPackage(INDIVIDUAL_LENDING_PACKAGE);
    INDIVIDUAL_LENDING_PATTERN.setAccountAssignmentGroups(Collections.singleton(AccountDesignators.CUSTOMER_LOAN_GROUP));
    final Set<RequiredAccountAssignment> individualLendingRequiredAccounts = new HashSet<>();
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
        AccountType.ASSET.name(),
        AccountDesignators.CUSTOMER_LOAN_GROUP));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.CUSTOMER_LOAN_INTEREST,
        AccountType.ASSET.name(),
        AccountDesignators.CUSTOMER_LOAN_GROUP));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountType.ASSET.name(),
        AccountDesignators.CUSTOMER_LOAN_GROUP));

    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.LOAN_FUNDS_SOURCE,
        AccountType.ASSET.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.PROCESSING_FEE_INCOME,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.ORIGINATION_FEE_INCOME,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.DISBURSEMENT_FEE_INCOME,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.INTEREST_INCOME,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.INTEREST_ACCRUAL,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.LATE_FEE_INCOME,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.LATE_FEE_ACCRUAL,
        AccountType.REVENUE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.PRODUCT_LOSS_ALLOWANCE,
        AccountType.ASSET.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.GENERAL_LOSS_ALLOWANCE,
        AccountType.EXPENSE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.EXPENSE,
        AccountType.EXPENSE.name()));
    individualLendingRequiredAccounts.add(new RequiredAccountAssignment(
        AccountDesignators.ENTRY,
        AccountType.LIABILITY.name()));
    INDIVIDUAL_LENDING_PATTERN.setAccountAssignmentsRequired(individualLendingRequiredAccounts);
  }


  public static Pattern individualLendingPattern() {
    return INDIVIDUAL_LENDING_PATTERN;
  }

  private final CaseParametersRepository caseParametersRepository;
  private final DataContextService dataContextService;
  private final OpenPaymentBuilderService openPaymentBuilderService;
  private final ApprovePaymentBuilderService approvePaymentBuilderService;
  private final DenyPaymentBuilderService denyPaymentBuilderService;
  private final DisbursePaymentBuilderService disbursePaymentBuilderService;
  private final ApplyInterestPaymentBuilderService applyInterestPaymentBuilderService;
  private final AcceptPaymentBuilderService acceptPaymentBuilderService;
  private final ClosePaymentBuilderService closePaymentBuilderService;
  private final MarkLatePaymentBuilderService markLatePaymentBuilderService;
  private final MarkInArrearsPaymentBuilderService  markInArrearsBuilderService;
  private final WriteOffPaymentBuilderService writeOffPaymentBuilderService;
  private final RecoverPaymentBuilderService recoverPaymentBuilderService;
  private final AccountingAdapter accountingAdapter;
  private final CustomerManager customerManager;
  private final IndividualLendingCommandDispatcher individualLendingCommandDispatcher;
  private final Gson gson;

  @Autowired
  IndividualLendingPatternFactory(
      final CaseParametersRepository caseParametersRepository,
      final DataContextService dataContextService,
      final OpenPaymentBuilderService openPaymentBuilderService,
      final ApprovePaymentBuilderService approvePaymentBuilderService,
      final DenyPaymentBuilderService denyPaymentBuilderService,
      final DisbursePaymentBuilderService disbursePaymentBuilderService,
      final ApplyInterestPaymentBuilderService applyInterestPaymentBuilderService,
      final AcceptPaymentBuilderService acceptPaymentBuilderService,
      final ClosePaymentBuilderService closePaymentBuilderService,
      final MarkLatePaymentBuilderService markLatePaymentBuilderService,
      MarkInArrearsPaymentBuilderService markInArrearsBuilderService, final WriteOffPaymentBuilderService writeOffPaymentBuilderService,
      final RecoverPaymentBuilderService recoverPaymentBuilderService,
      AccountingAdapter accountingAdapter, final CustomerManager customerManager,
      final IndividualLendingCommandDispatcher individualLendingCommandDispatcher,
      @Qualifier(ServiceConstants.GSON_NAME) final Gson gson)
  {
    this.caseParametersRepository = caseParametersRepository;
    this.dataContextService = dataContextService;
    this.openPaymentBuilderService = openPaymentBuilderService;
    this.approvePaymentBuilderService = approvePaymentBuilderService;
    this.denyPaymentBuilderService = denyPaymentBuilderService;
    this.disbursePaymentBuilderService = disbursePaymentBuilderService;
    this.applyInterestPaymentBuilderService = applyInterestPaymentBuilderService;
    this.acceptPaymentBuilderService = acceptPaymentBuilderService;
    this.closePaymentBuilderService = closePaymentBuilderService;
    this.markLatePaymentBuilderService = markLatePaymentBuilderService;
    this.markInArrearsBuilderService = markInArrearsBuilderService;
    this.writeOffPaymentBuilderService = writeOffPaymentBuilderService;
    this.recoverPaymentBuilderService = recoverPaymentBuilderService;
    this.accountingAdapter = accountingAdapter;

    this.customerManager = customerManager;
    this.individualLendingCommandDispatcher = individualLendingCommandDispatcher;
    this.gson = gson;
  }

  @Override
  public Pattern pattern() {
    return INDIVIDUAL_LENDING_PATTERN;
  }

  @Override
  public Stream<ChargeDefinition> defaultConfigurableCharges() {
    return ChargeDefinitionService.defaultConfigurableIndividualLoanCharges();
  }

  @Override
  public void checkParameters(final String parameters) {
    final CaseParameters caseParameters = gson.fromJson(parameters, CaseParameters.class);

    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();
    final Set<ConstraintViolation<CaseParameters>> errors = validator.validate(caseParameters);
    if (errors.size() != 0) {
      throw ServiceException.badRequest("CaseParameters are invalid.");
    }
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
  public Payment getCostComponentsForAction(
      final String productIdentifier,
      final String caseIdentifier,
      final String actionIdentifier,
      final LocalDateTime forDateTime,
      final Set<String> forAccountDesignators,
      final BigDecimal forPaymentSize) {
    final Action action = Action.valueOf(actionIdentifier);
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(productIdentifier, caseIdentifier, Collections.emptyList());
    final Case.State caseState = Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState());
    checkActionCanBeExecuted(caseState, action);

    return getPaymentForAction(
        action,
        dataContextOfAction,
        forAccountDesignators,
        forPaymentSize,
        forDateTime.toLocalDate());
  }

  private Payment getPaymentForAction(
      final Action action,
      final DataContextOfAction dataContextOfAction,
      final Set<String> forAccountDesignators,
      final BigDecimal forPaymentSize,
      final LocalDate forDate) {
    final PaymentBuilderService paymentBuilderService;
    switch (action) {
      case OPEN:
        paymentBuilderService = openPaymentBuilderService;
        break;
      case APPROVE:
        paymentBuilderService = approvePaymentBuilderService;
        break;
      case DENY:
        paymentBuilderService = denyPaymentBuilderService;
        break;
      case DISBURSE:
        paymentBuilderService = disbursePaymentBuilderService;
        break;
      case APPLY_INTEREST:
        paymentBuilderService = applyInterestPaymentBuilderService;
        break;
      case ACCEPT_PAYMENT:
        paymentBuilderService = acceptPaymentBuilderService;
        break;
      case CLOSE:
        paymentBuilderService = closePaymentBuilderService;
        break;
      case MARK_LATE:
        paymentBuilderService = markLatePaymentBuilderService;
        break;
      case MARK_IN_ARREARS:
        paymentBuilderService = markInArrearsBuilderService;
        break;
      case WRITE_OFF:
        paymentBuilderService = writeOffPaymentBuilderService;
        break;
      case RECOVER:
        paymentBuilderService = recoverPaymentBuilderService;
        break;
      default:
        throw ServiceException.internalError("Invalid action: ''{0}''.", action.name());
    }

    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        dataContextOfAction);

    final PaymentBuilder paymentBuilder = paymentBuilderService.getPaymentBuilder(
        dataContextOfAction,
        forPaymentSize,
        forDate,
        runningBalances);

    return paymentBuilder.buildPayment(action, forAccountDesignators, forDate);
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
        return new HashSet<>(Arrays.asList(Action.OPEN, Action.IMPORT));
      case PENDING:
        return new HashSet<>(Arrays.asList(Action.DENY, Action.APPROVE));
      case APPROVED:
        return new HashSet<>(Arrays.asList(Action.DISBURSE, Action.CLOSE));
      case ACTIVE:
        return new HashSet<>(Arrays.asList(Action.CLOSE, Action.ACCEPT_PAYMENT, Action.MARK_LATE, Action.APPLY_INTEREST, Action.DISBURSE, Action.MARK_IN_ARREARS, Action.WRITE_OFF));
      case CLOSED:
        return Collections.emptySet();
      default:
        return Collections.emptySet();
    }
  }

  public ProductCommandDispatcher getIndividualLendingCommandDispatcher() {
    return this.individualLendingCommandDispatcher;
  }
}
