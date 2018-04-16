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
package org.apache.fineract.cn.individuallending.internal.mapper;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CreditWorthinessFactor;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CreditWorthinessSnapshot;
import org.apache.fineract.cn.individuallending.internal.repository.CaseCreditWorthinessFactorEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CreditWorthinessFactorType;
import org.apache.fineract.cn.portfolio.api.v1.domain.PaymentCycle;
import org.apache.fineract.cn.portfolio.api.v1.domain.TermRange;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class CaseParametersMapper {

  public static CaseParametersEntity map(
      final Long caseId,
      final CaseParameters instance) {
    final CaseParametersEntity ret = new CaseParametersEntity();

    ret.setCaseId(caseId);
    ret.setCustomerIdentifier(instance.getCustomerIdentifier());
    ret.setTermRangeTemporalUnit(instance.getTermRange().getTemporalUnit());
    ret.setTermRangeMinimum(0);
    ret.setTermRangeMaximum(instance.getTermRange().getMaximum());
    ret.setBalanceRangeMaximum(instance.getMaximumBalance());
    ret.setPaymentCycleTemporalUnit(instance.getPaymentCycle().getTemporalUnit());
    ret.setPaymentCyclePeriod(instance.getPaymentCycle().getPeriod());
    ret.setPaymentCycleAlignmentDay(instance.getPaymentCycle().getAlignmentDay());
    ret.setPaymentCycleAlignmentWeek(instance.getPaymentCycle().getAlignmentWeek());
    ret.setPaymentCycleAlignmentMonth(instance.getPaymentCycle().getAlignmentMonth());
    ret.setCreditWorthinessFactors(mapSnapshotsToFactors(instance.getCreditWorthinessSnapshots(), ret));
    ret.setPaymentSize(BigDecimal.ONE.negate()); //semaphore for not yet set.

    return ret;
  }

  public static Set<CaseCreditWorthinessFactorEntity> mapSnapshotsToFactors(
          final List<CreditWorthinessSnapshot> creditWorthinessSnapshots,
          final CaseParametersEntity caseParametersEntity) {
    if (creditWorthinessSnapshots == null)
      return Collections.emptySet();
    return Stream.iterate(0, i -> i+1).limit(creditWorthinessSnapshots.size())
            .flatMap(i -> mapSnapshotToFactors(
                    creditWorthinessSnapshots.get(i), i, caseParametersEntity)).collect(Collectors.toSet());
  }

  private static Stream<? extends CaseCreditWorthinessFactorEntity> mapSnapshotToFactors(
          final CreditWorthinessSnapshot creditWorthinessSnapshot,
          final int i,
          final CaseParametersEntity caseParametersEntity) {
    final String customerId = creditWorthinessSnapshot.getForCustomer();
    return Stream.concat(Stream.concat(Stream.concat(
            mapSnapshotPartToFactors(customerId, i,
                    CreditWorthinessFactorType.ASSET, creditWorthinessSnapshot.getAssets(),
                    caseParametersEntity),
            mapSnapshotPartToFactors(customerId, i,
                    CreditWorthinessFactorType.INCOME_SOURCE, creditWorthinessSnapshot.getIncomeSources(),
                    caseParametersEntity)),
            mapSnapshotPartToFactors(customerId, i,
                    CreditWorthinessFactorType.DEBT, creditWorthinessSnapshot.getDebts(),
                    caseParametersEntity)),
            createPlaceHolder(customerId, i, caseParametersEntity)
    );
  }

  private static Stream<CaseCreditWorthinessFactorEntity> createPlaceHolder(
          final String customerIdentifier,
          final int positionInCustomers,
          final CaseParametersEntity caseParametersEntity) {
    final CaseCreditWorthinessFactorEntity placeHolder = new CaseCreditWorthinessFactorEntity();
    placeHolder.setFactorType(CreditWorthinessFactorType.PLACE_HOLDER);
    placeHolder.setCustomerIdentifier(customerIdentifier);
    placeHolder.setPositionInCustomers(positionInCustomers);
    placeHolder.setPositionInFactor(0);
    placeHolder.setCaseId(caseParametersEntity);
    placeHolder.setAmount(BigDecimal.ZERO);
    placeHolder.setDescription("placeholder");
    return Stream.of(placeHolder);
  }

  private static Stream<CaseCreditWorthinessFactorEntity> mapSnapshotPartToFactors(
          final String forCustomer,
          final int i,
          final CreditWorthinessFactorType factorType,
          final List<CreditWorthinessFactor> part,
          final CaseParametersEntity caseParametersEntity) {
    return Stream.iterate(0, j -> j+1).limit(part.size())
            .map(j -> {
              final CaseCreditWorthinessFactorEntity ret = new CaseCreditWorthinessFactorEntity();
              ret.setAmount(part.get(j).getAmount());
              ret.setDescription(part.get(j).getDescription());
              ret.setCaseId(caseParametersEntity);
              ret.setCustomerIdentifier(forCustomer);
              ret.setFactorType(factorType);
              ret.setPositionInFactor(j);
              ret.setPositionInCustomers(i);
              return ret;
            });
  }

  private static TermRange getTermRange(final CaseParametersEntity instance) {
    final TermRange ret = new TermRange();

    ret.setTemporalUnit(instance.getTermRangeTemporalUnit());
    ret.setMaximum(instance.getTermRangeMaximum());

    return ret;
  }

  private static PaymentCycle getPaymentCycle(final CaseParametersEntity instance) {
    final PaymentCycle ret = new PaymentCycle();

    ret.setTemporalUnit(instance.getPaymentCycleTemporalUnit());
    ret.setPeriod(instance.getPaymentCyclePeriod());
    ret.setAlignmentDay(instance.getPaymentCycleAlignmentDay());
    ret.setAlignmentWeek(instance.getPaymentCycleAlignmentWeek());
    ret.setAlignmentMonth(instance.getPaymentCycleAlignmentMonth());

    //This is necessary because for a while inapproriate values were being accepted for the
    //alignment week.  Those values are being mapped to -1 which is a sentinel value used to represent the
    //last week of the month.  If we can fix the existing data, we can remove this code.
    if (instance.getPaymentCycleAlignmentWeek() != null &&
        ((instance.getPaymentCycleAlignmentWeek() == 3) ||
         (instance.getPaymentCycleAlignmentWeek() == 4)))
      ret.setAlignmentWeek(-1);

    return ret;
  }

  public static CaseParameters mapEntity(final CaseParametersEntity caseParametersEntity,
                                         final int minorCurrencyUnitDigits) {
    final CaseParameters ret = new CaseParameters();
    ret.setCustomerIdentifier(caseParametersEntity.getCustomerIdentifier());
    ret.setCreditWorthinessSnapshots(mapFactorsToSnapshots(caseParametersEntity.getCreditWorthinessFactors(), minorCurrencyUnitDigits));
    ret.setTermRange(getTermRange(caseParametersEntity));
    ret.setMaximumBalance(caseParametersEntity.getBalanceRangeMaximum().setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN));
    ret.setPaymentCycle(getPaymentCycle(caseParametersEntity));
    return ret;
  }

  private static List<CreditWorthinessSnapshot> mapFactorsToSnapshots(
      final Set<CaseCreditWorthinessFactorEntity> creditWorthinessFactors,
      final int minorCurrencyUnitDigits) {
    final Map<Integer, Set<CaseCreditWorthinessFactorEntity>> groupedByCustomerId
            = creditWorthinessFactors.stream()
            .collect(Collectors.groupingBy(CaseCreditWorthinessFactorEntity::getPositionInCustomers, Collectors.toSet()));

    return groupedByCustomerId.entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .map(customerEntry -> mapEntryToSnapshot(customerEntry, minorCurrencyUnitDigits)).collect(Collectors.toList());
  }

  private static CreditWorthinessSnapshot mapEntryToSnapshot(
      final Map.Entry<Integer, Set<CaseCreditWorthinessFactorEntity>> customerEntry,
      final int minorCurrencyUnitDigits) {
    final CreditWorthinessSnapshot ret = new CreditWorthinessSnapshot();

    final Map<CreditWorthinessFactorType, Set<CaseCreditWorthinessFactorEntity>> groupedByFactorType
            = customerEntry.getValue().stream()
            .collect(Collectors.groupingBy(CaseCreditWorthinessFactorEntity::getFactorType, Collectors.toSet()));
    ret.setAssets(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.ASSET, minorCurrencyUnitDigits));
    ret.setDebts(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.DEBT, minorCurrencyUnitDigits));
    ret.setIncomeSources(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.INCOME_SOURCE, minorCurrencyUnitDigits));

    final String customerId = customerEntry.getValue().stream()
            .findFirst()
            .map(CaseCreditWorthinessFactorEntity::getCustomerIdentifier)
            .orElse("");
    ret.setForCustomer(customerId);

    return ret;
  }

  private static List<CreditWorthinessFactor> getFactorsByType(
      final Map<CreditWorthinessFactorType, Set<CaseCreditWorthinessFactorEntity>> groupedByFactorType,
      final CreditWorthinessFactorType factorType,
      final int minorCurrencyUnitDigits) {
    final Set<CaseCreditWorthinessFactorEntity> byFactorType = groupedByFactorType.get(factorType);
    if (byFactorType == null)
      return Collections.emptyList();
    else {
      return byFactorType.stream()
              .sorted(Comparator.comparingInt(CaseCreditWorthinessFactorEntity::getPositionInFactor))
              .map(caseCreditWorthinessFactorEntity -> mapEntryToFactor(caseCreditWorthinessFactorEntity, minorCurrencyUnitDigits))
              .collect(Collectors.toList());

    }
  }

  private static CreditWorthinessFactor mapEntryToFactor(
      final CaseCreditWorthinessFactorEntity caseCreditWorthinessFactorEntity,
      final int minorCurrencyUnitDigits) {
    final CreditWorthinessFactor ret = new CreditWorthinessFactor();
    ret.setDescription(caseCreditWorthinessFactorEntity.getDescription());
    ret.setAmount(caseCreditWorthinessFactorEntity.getAmount().setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN));
    return ret;
  }
}
