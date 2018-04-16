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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.service.ChargeDefinitionService;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DefaultChargeDefinitionsMocker;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargesService;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.mockito.Mockito;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.function.Function;

class PaymentBuilderServiceTestHarness {
  static PaymentBuilder constructCallToPaymentBuilder (
      final Function<ScheduledChargesService, PaymentBuilderService> serviceFactory,
      final PaymentBuilderServiceTestCase testCase) {
    final BalanceSegmentRepository balanceSegmentRepository = Mockito.mock(BalanceSegmentRepository.class);
    final ChargeDefinitionService chargeDefinitionService = DefaultChargeDefinitionsMocker.getChargeDefinitionService(Collections.emptyList());
    final ScheduledChargesService scheduledChargesService = new ScheduledChargesService(chargeDefinitionService, balanceSegmentRepository);
    final PaymentBuilderService testSubject = serviceFactory.apply(scheduledChargesService);

    final ProductEntity product = new ProductEntity();
    product.setIdentifier("blah");
    product.setMinorCurrencyUnitDigits(2);
    final CaseEntity customerCase = new CaseEntity();
    customerCase.setEndOfTerm(testCase.endOfTerm);
    customerCase.setInterest(testCase.interestRate);
    final CaseParametersEntity caseParameters = new CaseParametersEntity();
    caseParameters.setPaymentSize(testCase.configuredPaymentSize);
    caseParameters.setBalanceRangeMaximum(testCase.balanceRangeMaximum);
    caseParameters.setPaymentCyclePeriod(1);
    caseParameters.setPaymentCycleTemporalUnit(ChronoUnit.MONTHS);
    caseParameters.setCreditWorthinessFactors(Collections.emptySet());

    final SimulatedRunningBalances runningBalances = new SimulatedRunningBalances(testCase.startOfTerm);
    runningBalances.adjustBalance(AccountDesignators.ENTRY, testCase.entryAccountBalance);
    runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, testCase.remainingPrincipal.negate());
    runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_INTEREST, testCase.accruedInterest.negate());
    runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_FEES, testCase.nonLateFees.negate());
    runningBalances.adjustBalance(AccountDesignators.INTEREST_ACCRUAL, testCase.accruedInterest);

    runningBalances.adjustBalance(AccountDesignators.GENERAL_LOSS_ALLOWANCE, testCase.generalLossAllowance.negate());

    final DataContextOfAction dataContextOfAction = new DataContextOfAction(
        product,
        customerCase,
        caseParameters,
        Collections.emptyList());
    return testSubject.getPaymentBuilder(
        dataContextOfAction,
        testCase.requestedPaymentSize,
        testCase.forDate.toLocalDate(),
        runningBalances);
  }
}