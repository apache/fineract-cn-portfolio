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
package io.mifos.individuallending.internal.service.costcomponent;

import com.google.common.collect.Sets;
import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Payment;
import io.mifos.portfolio.api.v1.domain.RequiredAccountAssignment;
import io.mifos.portfolio.service.internal.util.ChargeInstance;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class PaymentBuilder {
  private final RunningBalances prePaymentBalances;
  private final Map<ChargeDefinition, CostComponent> costComponents;
  private final Map<String, BigDecimal> balanceAdjustments;
  private final boolean accrualAccounting;

  PaymentBuilder(final RunningBalances prePaymentBalances,
                 final boolean accrualAccounting) {
    this.prePaymentBalances = prePaymentBalances;
    this.costComponents = new HashMap<>();
    this.balanceAdjustments = new HashMap<>();
    this.accrualAccounting = accrualAccounting;
  }

  public Payment buildPayment(final Action action, final Set<String> forAccountDesignators) {

    if (!forAccountDesignators.isEmpty()) {
      final Stream<Map.Entry<ChargeDefinition, CostComponent>> costComponentStream = stream()
          .filter(costComponentEntry -> chargeReferencesAccountDesignators(
              costComponentEntry.getKey(),
              action,
              forAccountDesignators));

      final List<CostComponent> costComponentList = costComponentStream
          .map(costComponentEntry -> new CostComponent(
              costComponentEntry.getKey().getIdentifier(),
              costComponentEntry.getValue().getAmount()))
          .collect(Collectors.toList());

      return new Payment(costComponentList, balanceAdjustments);
    }
    else {
      return buildPayment();
    }

  }

  private Payment buildPayment() {
    final Stream<Map.Entry<ChargeDefinition, CostComponent>> costComponentStream  = stream();

    final List<CostComponent> costComponentList = costComponentStream
        .map(costComponentEntry -> new CostComponent(
            costComponentEntry.getKey().getIdentifier(),
            costComponentEntry.getValue().getAmount()))
        .collect(Collectors.toList());

    return new Payment(costComponentList, balanceAdjustments);
  }

  public PlannedPayment accumulatePlannedPayment(final SimulatedRunningBalances balances) {
    final Payment payment = buildPayment();
    balanceAdjustments.forEach(balances::adjustBalance);
    final Map<String, BigDecimal> balancesCopy = balances.snapshot();

    return new PlannedPayment(payment, balancesCopy);
  }

  public List<ChargeInstance> buildCharges(
      final Action action,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {
    return stream()
        .map(entry -> mapCostComponentEntryToChargeInstance(action, entry, designatorToAccountIdentifierMapper))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  BigDecimal getBalanceAdjustment(final String... accountDesignators) {
    return Arrays.stream(accountDesignators)
        .map(accountDesignator -> balanceAdjustments.getOrDefault(accountDesignator, BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  void adjustBalances(
      final Action action,
      final ChargeDefinition chargeDefinition,
      final BigDecimal chargeAmount) {
    BigDecimal adjustedChargeAmount = BigDecimal.ZERO;
    if (this.accrualAccounting && chargeIsAccrued(chargeDefinition)) {
      if (Action.valueOf(chargeDefinition.getAccrueAction()) == action) {
        adjustedChargeAmount = getMaxCharge(chargeDefinition.getFromAccountDesignator(), chargeDefinition.getAccrualAccountDesignator(), chargeAmount);

        this.addToBalance(chargeDefinition.getFromAccountDesignator(), adjustedChargeAmount.negate());
        this.addToBalance(chargeDefinition.getAccrualAccountDesignator(), adjustedChargeAmount);
      } else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
        adjustedChargeAmount = getMaxCharge(chargeDefinition.getAccrualAccountDesignator(), chargeDefinition.getToAccountDesignator(), chargeAmount);

        this.addToBalance(chargeDefinition.getAccrualAccountDesignator(), adjustedChargeAmount.negate());
        this.addToBalance(chargeDefinition.getToAccountDesignator(), adjustedChargeAmount);
      }
    }
    else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
      adjustedChargeAmount = getMaxCharge(chargeDefinition.getFromAccountDesignator(), chargeDefinition.getToAccountDesignator(), chargeAmount);

      this.addToBalance(chargeDefinition.getFromAccountDesignator(), adjustedChargeAmount.negate());
      this.addToBalance(chargeDefinition.getToAccountDesignator(), adjustedChargeAmount);
    }


    addToCostComponent(chargeDefinition, adjustedChargeAmount);
  }

  private BigDecimal getMaxCharge(
      final String fromAccountDesignator,
      final String toAccountDesignator,
      final BigDecimal plannedCharge) {
    final BigDecimal expectedImpactOnDebitAccount = plannedCharge.add(this.getBalanceAdjustment(fromAccountDesignator));
    final BigDecimal maxImpactOnDebitAccount = prePaymentBalances.getMaxDebit(fromAccountDesignator, expectedImpactOnDebitAccount);
    final BigDecimal maxDebit = maxImpactOnDebitAccount.subtract(this.getBalanceAdjustment(fromAccountDesignator));

    final BigDecimal expectedImpactOnCreditAccount = plannedCharge.add(this.getBalanceAdjustment(toAccountDesignator));
    final BigDecimal maxImpactOnCreditAccount = prePaymentBalances.getMaxCredit(toAccountDesignator, expectedImpactOnCreditAccount);
    final BigDecimal maxCredit = maxImpactOnCreditAccount.subtract(this.getBalanceAdjustment(toAccountDesignator));
    return maxCredit.min(maxDebit);
  }

  private static boolean chargeIsAccrued(final ChargeDefinition chargeDefinition) {
    return chargeDefinition.getAccrualAccountDesignator() != null;
  }

  void addToBalance(
      final String accountDesignator,
      final BigDecimal chargeAmount) {
    final BigDecimal currentAdjustment = balanceAdjustments.getOrDefault(accountDesignator, BigDecimal.ZERO);
    final BigDecimal newAdjustment = currentAdjustment.add(chargeAmount);
    balanceAdjustments.put(accountDesignator, newAdjustment);
  }

  void addToCostComponent(
      final ChargeDefinition chargeDefinition,
      final BigDecimal amount) {
    final CostComponent costComponent = costComponents
        .computeIfAbsent(chargeDefinition, PaymentBuilder::constructEmptyCostComponent);
    costComponent.setAmount(costComponent.getAmount().add(amount));
  }

  private Stream<Map.Entry<ChargeDefinition, CostComponent>> stream() {
    return costComponents.entrySet().stream()
        .filter(costComponentEntry -> costComponentEntry.getValue().getAmount().compareTo(BigDecimal.ZERO) != 0);
  }


  private static boolean chargeReferencesAccountDesignators(
      final ChargeDefinition chargeDefinition,
      final Action action,
      final Set<String> forAccountDesignators) {
    final Set<String> accountsToCompare = Sets.newHashSet(
        chargeDefinition.getFromAccountDesignator(),
        chargeDefinition.getToAccountDesignator()
    );
    if (chargeDefinition.getAccrualAccountDesignator() != null)
      accountsToCompare.add(chargeDefinition.getAccrualAccountDesignator());

    final Set<String> expandedForAccountDesignators = expandAccountDesignators(forAccountDesignators);

    return !Sets.intersection(accountsToCompare, expandedForAccountDesignators).isEmpty();
  }

  static Set<String> expandAccountDesignators(final Set<String> accountDesignators) {
    final Set<RequiredAccountAssignment> accountAssignmentsRequired = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentsRequired();
    final Map<String, List<RequiredAccountAssignment>> accountAssignmentsByGroup = accountAssignmentsRequired.stream()
        .filter(x -> x.getGroup() != null)
        .collect(Collectors.groupingBy(RequiredAccountAssignment::getGroup, Collectors.toList()));
    final Set<String> groupExpansions = accountDesignators.stream()
        .flatMap(accountDesignator -> {
          final List<RequiredAccountAssignment> group = accountAssignmentsByGroup.get(accountDesignator);
          if (group != null)
            return group.stream();
          else
            return Stream.empty();
        })
        .map(RequiredAccountAssignment::getAccountDesignator)
        .collect(Collectors.toSet());
    final Set<String> ret = new HashSet<>(accountDesignators);
    ret.addAll(groupExpansions);
    return ret;
  }

  private static CostComponent constructEmptyCostComponent(final ChargeDefinition chargeDefinition) {
    final CostComponent ret = new CostComponent();
    ret.setChargeIdentifier(chargeDefinition.getIdentifier());
    ret.setAmount(BigDecimal.ZERO);
    return ret;
  }

  private static Optional<ChargeInstance> mapCostComponentEntryToChargeInstance(
      final Action action,
      final Map.Entry<ChargeDefinition, CostComponent> costComponentEntry,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {
    final ChargeDefinition chargeDefinition = costComponentEntry.getKey();
    final BigDecimal chargeAmount = costComponentEntry.getValue().getAmount();

    if (chargeIsAccrued(chargeDefinition)) {
      if (Action.valueOf(chargeDefinition.getAccrueAction()) == action)
        return Optional.of(new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getFromAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator()),
            chargeAmount));
      else if (Action.valueOf(chargeDefinition.getChargeAction()) == action)
        return Optional.of(new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getToAccountDesignator()),
            chargeAmount));
      else
        return Optional.empty();
    }
    else if (Action.valueOf(chargeDefinition.getChargeAction()) == action)
      return Optional.of(new ChargeInstance(
          designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getFromAccountDesignator()),
          designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getToAccountDesignator()),
          chargeAmount));
    else
      return Optional.empty();
  }
}
