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
package org.apache.fineract.cn.individuallending.internal.service.costcomponent;

import com.google.common.collect.Sets;
import org.apache.fineract.cn.individuallending.IndividualLendingPatternFactory;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.api.v1.domain.RequiredAccountAssignment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.fineract.cn.lang.DateConverter;

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

  private Map<String, BigDecimal> copyBalanceAdjustments() {
    return balanceAdjustments.entrySet().stream()
        .filter(x -> x.getValue().compareTo(BigDecimal.ZERO) != 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Payment buildPayment(
      final Action action,
      final Set<String> forAccountDesignators,
      final @Nullable LocalDate forDate)
  {
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

      final Payment ret = new Payment(costComponentList, copyBalanceAdjustments());
      ret.setDate(forDate == null ? null : DateConverter.toIsoString(forDate.atStartOfDay()));
      return ret;
    }
    else {
      return buildPayment(forDate);
    }

  }

  private Payment buildPayment(final @Nullable LocalDate forDate) {
    final Stream<Map.Entry<ChargeDefinition, CostComponent>> costComponentStream  = stream();

    final List<CostComponent> costComponentList = costComponentStream
        .map(costComponentEntry -> new CostComponent(
            costComponentEntry.getKey().getIdentifier(),
            costComponentEntry.getValue().getAmount()))
        .collect(Collectors.toList());

    final Payment ret = new Payment(costComponentList, copyBalanceAdjustments());
    ret.setDate(forDate == null ? null : DateConverter.toIsoString(forDate.atStartOfDay()));
    return ret;
  }

  public PlannedPayment accumulatePlannedPayment(
      final SimulatedRunningBalances balances,
      final @Nullable LocalDate forDate) {
    final Payment payment = buildPayment(forDate);
    balanceAdjustments.forEach(balances::adjustBalance);
    final Map<String, BigDecimal> balancesCopy = balances.snapshot();

    return new PlannedPayment(payment, balancesCopy);
  }

  public Map<String, BigDecimal> getBalanceAdjustments() {
    return balanceAdjustments;
  }

  public BigDecimal getBalanceAdjustment(final String... accountDesignators) {
    return Arrays.stream(accountDesignators)
        .map(accountDesignator -> balanceAdjustments.getOrDefault(accountDesignator, BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  void adjustBalances(
      final Action action,
      final ChargeDefinition chargeDefinition,
      final BigDecimal chargeAmount) {
    BigDecimal adjustedChargeAmount;
    if (this.accrualAccounting && chargeIsAccrued(chargeDefinition)) {
      if (Action.valueOf(chargeDefinition.getAccrueAction()) == action) {
        adjustedChargeAmount = getMaxCharge(chargeDefinition.getFromAccountDesignator(), chargeDefinition.getAccrualAccountDesignator(), chargeAmount);

        this.addToBalance(chargeDefinition.getFromAccountDesignator(), adjustedChargeAmount.negate());
        this.addToBalance(chargeDefinition.getAccrualAccountDesignator(), adjustedChargeAmount);
      } else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
        adjustedChargeAmount = getMaxCharge(chargeDefinition.getAccrualAccountDesignator(), chargeDefinition.getToAccountDesignator(), chargeAmount);

        this.addToBalance(chargeDefinition.getAccrualAccountDesignator(), adjustedChargeAmount.negate());
        this.addToBalance(chargeDefinition.getToAccountDesignator(), adjustedChargeAmount);

        addToCostComponent(chargeDefinition, adjustedChargeAmount);
      }
    }
    else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
      adjustedChargeAmount = getMaxCharge(chargeDefinition.getFromAccountDesignator(), chargeDefinition.getToAccountDesignator(), chargeAmount);

      this.addToBalance(chargeDefinition.getFromAccountDesignator(), adjustedChargeAmount.negate());
      this.addToBalance(chargeDefinition.getToAccountDesignator(), adjustedChargeAmount);

      addToCostComponent(chargeDefinition, adjustedChargeAmount);
    }
  }

  private BigDecimal getMaxCharge(
      final String fromAccountDesignator,
      final String toAccountDesignator,
      final BigDecimal plannedCharge) {
    final BigDecimal expectedImpactOnDebitAccount = plannedCharge.subtract(this.getBalanceAdjustment(fromAccountDesignator));
    final BigDecimal maxImpactOnDebitAccount = prePaymentBalances.getMaxDebit(fromAccountDesignator, expectedImpactOnDebitAccount);
    final BigDecimal maxDebit = (!fromAccountDesignator.equals(AccountDesignators.PRODUCT_LOSS_ALLOWANCE)) ?
        maxImpactOnDebitAccount.add(this.getBalanceAdjustment(fromAccountDesignator)).max(BigDecimal.ZERO) :
        maxImpactOnDebitAccount.add(this.getBalanceAdjustment(fromAccountDesignator));

    final BigDecimal expectedImpactOnCreditAccount = plannedCharge.add(this.getBalanceAdjustment(toAccountDesignator));
    final BigDecimal maxImpactOnCreditAccount = prePaymentBalances.getMaxCredit(toAccountDesignator, expectedImpactOnCreditAccount);
    final BigDecimal maxCredit = (!toAccountDesignator.equals(AccountDesignators.GENERAL_LOSS_ALLOWANCE)) ?
        maxImpactOnCreditAccount.subtract(this.getBalanceAdjustment(toAccountDesignator)).max(BigDecimal.ZERO) :
        maxImpactOnCreditAccount.subtract(this.getBalanceAdjustment(toAccountDesignator));
    return maxCredit.min(maxDebit);
  }

  private static boolean chargeIsAccrued(final ChargeDefinition chargeDefinition) {
    return chargeDefinition.getAccrualAccountDesignator() != null && chargeDefinition.getAccrueAction() != null;
  }

  private void addToBalance(
      final String accountDesignator,
      final BigDecimal chargeAmount) {
    final BigDecimal currentAdjustment = balanceAdjustments.getOrDefault(accountDesignator, BigDecimal.ZERO);
    final BigDecimal newAdjustment = currentAdjustment.add(chargeAmount);
    balanceAdjustments.put(accountDesignator, newAdjustment);
  }

  private void addToCostComponent(
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
}
