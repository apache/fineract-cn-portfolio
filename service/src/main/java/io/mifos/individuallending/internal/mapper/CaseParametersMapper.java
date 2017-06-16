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
package io.mifos.individuallending.internal.mapper;

import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessFactor;
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessSnapshot;
import io.mifos.individuallending.internal.repository.CaseCreditWorthinessFactorEntity;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.repository.CreditWorthinessFactorType;
import io.mifos.portfolio.api.v1.domain.PaymentCycle;
import io.mifos.portfolio.api.v1.domain.TermRange;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class CaseParametersMapper {

  public static CaseParametersEntity map(final Long caseId, final CaseParameters instance) {
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

    return ret;
  }

  public static Set<CaseCreditWorthinessFactorEntity> mapSnapshotsToFactors(
          final List<CreditWorthinessSnapshot> creditWorthinessSnapshots,
          final CaseParametersEntity caseParametersEntity) {
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

    return ret;
  }

  public static CaseParameters mapEntity(final CaseParametersEntity caseParametersEntity) {
    final CaseParameters ret = new CaseParameters();
    ret.setCustomerIdentifier(caseParametersEntity.getCustomerIdentifier());
    ret.setCreditWorthinessSnapshots(mapFactorsToSnapshots(caseParametersEntity.getCreditWorthinessFactors()));
    ret.setTermRange(getTermRange(caseParametersEntity));
    ret.setMaximumBalance(caseParametersEntity.getBalanceRangeMaximum());
    ret.setPaymentCycle(getPaymentCycle(caseParametersEntity));
    return ret;
  }

  private static List<CreditWorthinessSnapshot> mapFactorsToSnapshots(
          final Set<CaseCreditWorthinessFactorEntity> creditWorthinessFactors) {
    final Map<Integer, Set<CaseCreditWorthinessFactorEntity>> groupedByCustomerId
            = creditWorthinessFactors.stream()
            .collect(Collectors.groupingBy(CaseCreditWorthinessFactorEntity::getPositionInCustomers, Collectors.toSet()));

    return groupedByCustomerId.entrySet().stream()
            .sorted((x, y) -> Integer.compare(x.getKey(), y.getKey()))
            .map(CaseParametersMapper::mapEntryToSnapshot).collect(Collectors.toList());
  }

  private static CreditWorthinessSnapshot mapEntryToSnapshot(
          final Map.Entry<Integer, Set<CaseCreditWorthinessFactorEntity>> customerEntry) {
    final CreditWorthinessSnapshot ret = new CreditWorthinessSnapshot();

    final Map<CreditWorthinessFactorType, Set<CaseCreditWorthinessFactorEntity>> groupedByFactorType
            = customerEntry.getValue().stream()
            .collect(Collectors.groupingBy(CaseCreditWorthinessFactorEntity::getFactorType, Collectors.toSet()));
    ret.setAssets(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.ASSET));
    ret.setDebts(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.DEBT));
    ret.setIncomeSources(getFactorsByType(groupedByFactorType, CreditWorthinessFactorType.INCOME_SOURCE));

    final String customerId = customerEntry.getValue().stream()
            .findFirst()
            .map(CaseCreditWorthinessFactorEntity::getCustomerIdentifier)
            .orElse("");
    ret.setForCustomer(customerId);

    return ret;
  }

  private static List<CreditWorthinessFactor> getFactorsByType(
          final Map<CreditWorthinessFactorType, Set<CaseCreditWorthinessFactorEntity>> groupedByFactorType,
          final CreditWorthinessFactorType factorType) {
    final Set<CaseCreditWorthinessFactorEntity> byFactorType = groupedByFactorType.get(factorType);
    if (byFactorType == null)
      return Collections.emptyList();
    else {
      return byFactorType.stream()
              .sorted((x, y) -> Integer.compare(x.getPositionInFactor(), y.getPositionInFactor()))
              .map(CaseParametersMapper::mapEntryToFactor)
              .collect(Collectors.toList());

    }
  }

  private static CreditWorthinessFactor mapEntryToFactor(
          final CaseCreditWorthinessFactorEntity caseCreditWorthinessFactorEntity) {
    final CreditWorthinessFactor ret = new CreditWorthinessFactor();
    ret.setDescription(caseCreditWorthinessFactorEntity.getDescription());
    ret.setAmount(caseCreditWorthinessFactorEntity.getAmount());
    return ret;
  }
}
