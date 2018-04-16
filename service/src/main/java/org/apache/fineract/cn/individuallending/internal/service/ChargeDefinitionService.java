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
package org.apache.fineract.cn.individuallending.internal.service;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.service.ConfigurableChargeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
@Service
public class ChargeDefinitionService {
  public static Stream<ChargeDefinition> defaultConfigurableIndividualLoanCharges() {
    final List<ChargeDefinition> ret = new ArrayList<>();
    final ChargeDefinition processingFee = charge(
        PROCESSING_FEE_NAME,
        Action.DISBURSE,
        BigDecimal.ONE,
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.PROCESSING_FEE_INCOME);
    processingFee.setReadOnly(false);

    final ChargeDefinition loanOriginationFee = charge(
        LOAN_ORIGINATION_FEE_NAME,
        Action.DISBURSE,
        BigDecimal.ONE,
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.ORIGINATION_FEE_INCOME);
    loanOriginationFee.setReadOnly(false);

    final ChargeDefinition disbursementFee = charge(
        DISBURSEMENT_FEE_NAME,
        Action.DISBURSE,
        BigDecimal.valueOf(0.1),
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.DISBURSEMENT_FEE_INCOME);
    disbursementFee.setProportionalTo(ChargeProportionalDesignator.REQUESTED_DISBURSEMENT_DESIGNATOR.getValue());
    disbursementFee.setReadOnly(false);

    final ChargeDefinition lateFee = charge(
        LATE_FEE_NAME,
        Action.ACCEPT_PAYMENT,
        BigDecimal.TEN,
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.LATE_FEE_INCOME);
    lateFee.setAccrueAction(Action.MARK_LATE.name());
    lateFee.setAccrualAccountDesignator(AccountDesignators.LATE_FEE_ACCRUAL);
    lateFee.setProportionalTo(ChargeProportionalDesignator.CONTRACTUAL_REPAYMENT_DESIGNATOR.getValue());
    lateFee.setChargeOnTop(true);
    lateFee.setReadOnly(false);

    ret.add(processingFee);
    ret.add(loanOriginationFee);
    ret.add(disbursementFee);
    ret.add(lateFee);

    return ret.stream();
  }

  static Stream<ChargeDefinition> individualLoanChargesDerivedFromConfiguration() {
    final List<ChargeDefinition> ret = new ArrayList<>();

    final ChargeDefinition disbursePayment = new ChargeDefinition();
    disbursePayment.setChargeAction(Action.DISBURSE.name());
    disbursePayment.setIdentifier(DISBURSE_PAYMENT_ID);
    disbursePayment.setName(DISBURSE_PAYMENT_NAME);
    disbursePayment.setDescription(DISBURSE_PAYMENT_NAME);
    disbursePayment.setFromAccountDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    disbursePayment.setToAccountDesignator(AccountDesignators.ENTRY);
    disbursePayment.setProportionalTo(ChargeProportionalDesignator.REQUESTED_DISBURSEMENT_DESIGNATOR.getValue());
    disbursePayment.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    disbursePayment.setAmount(BigDecimal.valueOf(100));
    disbursePayment.setReadOnly(true);

    final ChargeDefinition interestCharge = new ChargeDefinition();
    interestCharge.setIdentifier(INTEREST_ID);
    interestCharge.setName(INTEREST_NAME);
    interestCharge.setDescription(INTEREST_NAME);
    interestCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    interestCharge.setAmount(BigDecimal.valueOf(100));
    interestCharge.setFromAccountDesignator(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    interestCharge.setToAccountDesignator(AccountDesignators.INTEREST_INCOME);
    interestCharge.setForCycleSizeUnit(ChronoUnit.YEARS);
    interestCharge.setAccrueAction(Action.APPLY_INTEREST.name());
    interestCharge.setAccrualAccountDesignator(AccountDesignators.INTEREST_ACCRUAL);
    interestCharge.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_DESIGNATOR.getValue());
    interestCharge.setChargeMethod(ChargeDefinition.ChargeMethod.INTEREST);
    interestCharge.setReadOnly(true);

    final ChargeDefinition customerFeeRepaymentCharge = new ChargeDefinition();
    customerFeeRepaymentCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    customerFeeRepaymentCharge.setIdentifier(REPAY_FEES_ID);
    customerFeeRepaymentCharge.setName(REPAY_FEES_NAME);
    customerFeeRepaymentCharge.setDescription(REPAY_FEES_NAME);
    customerFeeRepaymentCharge.setFromAccountDesignator(AccountDesignators.ENTRY);
    customerFeeRepaymentCharge.setToAccountDesignator(AccountDesignators.CUSTOMER_LOAN_FEES);
    customerFeeRepaymentCharge.setProportionalTo(ChargeProportionalDesignator.TO_ACCOUNT_DESIGNATOR.getValue());
    customerFeeRepaymentCharge.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    customerFeeRepaymentCharge.setAmount(BigDecimal.valueOf(100));
    customerFeeRepaymentCharge.setReadOnly(true);

    final ChargeDefinition customerInterestRepaymentCharge = new ChargeDefinition();
    customerInterestRepaymentCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    customerInterestRepaymentCharge.setIdentifier(REPAY_INTEREST_ID);
    customerInterestRepaymentCharge.setName(REPAY_INTEREST_NAME);
    customerInterestRepaymentCharge.setDescription(REPAY_INTEREST_NAME);
    customerInterestRepaymentCharge.setFromAccountDesignator(AccountDesignators.ENTRY);
    customerInterestRepaymentCharge.setToAccountDesignator(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    customerInterestRepaymentCharge.setProportionalTo(ChargeProportionalDesignator.TO_ACCOUNT_DESIGNATOR.getValue());
    customerInterestRepaymentCharge.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    customerInterestRepaymentCharge.setAmount(BigDecimal.valueOf(100));
    customerInterestRepaymentCharge.setReadOnly(true);

    final ChargeDefinition customerPrincipalRepaymentCharge = new ChargeDefinition();
    customerPrincipalRepaymentCharge.setChargeAction(Action.ACCEPT_PAYMENT.name());
    customerPrincipalRepaymentCharge.setIdentifier(REPAY_PRINCIPAL_ID);
    customerPrincipalRepaymentCharge.setName(REPAY_PRINCIPAL_NAME);
    customerPrincipalRepaymentCharge.setDescription(REPAY_PRINCIPAL_NAME);
    customerPrincipalRepaymentCharge.setFromAccountDesignator(AccountDesignators.ENTRY);
    customerPrincipalRepaymentCharge.setToAccountDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    customerPrincipalRepaymentCharge.setProportionalTo(ChargeProportionalDesignator.REQUESTED_REPAYMENT_DESIGNATOR.getValue());
    customerPrincipalRepaymentCharge.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    customerPrincipalRepaymentCharge.setAmount(BigDecimal.valueOf(100));
    customerPrincipalRepaymentCharge.setReadOnly(true);

    ret.add(disbursePayment);
    ret.add(interestCharge);
    ret.add(customerPrincipalRepaymentCharge);
    ret.add(customerInterestRepaymentCharge);
    ret.add(customerFeeRepaymentCharge);

    return ret.stream();
  }
  private final ConfigurableChargeDefinitionService configurableChargeDefinitionService;

  @Autowired
  public ChargeDefinitionService(
      final ConfigurableChargeDefinitionService configurableChargeDefinitionService) {
    this.configurableChargeDefinitionService = configurableChargeDefinitionService;
  }

  private Stream<ChargeDefinition> getAllChargeDefinitions(final String productIdentifier) {
    final Stream<ChargeDefinition> configurableChargeDefinitions = configurableChargeDefinitionService.findAllEntities(productIdentifier);
    final Stream<ChargeDefinition> derivedChargeDefinitions = individualLoanChargesDerivedFromConfiguration();
    return Stream.concat(configurableChargeDefinitions, derivedChargeDefinitions);
  }

  @Nonnull
  public Map<String, List<ChargeDefinition>> getChargeDefinitionsMappedByChargeAction(
      final String productIdentifier)
  {
    final Stream<ChargeDefinition> chargeDefinitions = getAllChargeDefinitions(productIdentifier);

    return chargeDefinitions
        .collect(Collectors.groupingBy(ChargeDefinition::getChargeAction,
            Collectors.mapping(x -> x, Collectors.toList())));
  }

  @Nonnull
  public Map<String, List<ChargeDefinition>> getChargeDefinitionsMappedByAccrueAction(
      final String productIdentifier)
  {
    final Stream<ChargeDefinition> chargeDefinitions = getAllChargeDefinitions(productIdentifier);

    return chargeDefinitions
        .filter(x -> x.getAccrueAction() != null)
        .collect(Collectors.groupingBy(ChargeDefinition::getAccrueAction,
            Collectors.mapping(x -> x, Collectors.toList())));
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
