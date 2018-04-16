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
package org.apache.fineract.cn.individuallending.internal.service.schedule;

import org.apache.fineract.cn.individuallending.internal.service.ChargeDefinitionService;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Service
public class ScheduledChargesService {
  private final ChargeDefinitionService chargeDefinitionService;
  private final BalanceSegmentRepository balanceSegmentRepository;

  @Autowired
  public ScheduledChargesService(
      final ChargeDefinitionService chargeDefinitionService,
      final BalanceSegmentRepository balanceSegmentRepository) {
    this.chargeDefinitionService = chargeDefinitionService;
    this.balanceSegmentRepository = balanceSegmentRepository;
  }

  public List<ScheduledCharge> getScheduledCharges(
      final String productIdentifier,
      final @Nonnull List<ScheduledAction> scheduledActions) {
    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction
        = chargeDefinitionService.getChargeDefinitionsMappedByChargeAction(productIdentifier);

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction
        = chargeDefinitionService.getChargeDefinitionsMappedByAccrueAction(productIdentifier);

    return getScheduledCharges(
        productIdentifier,
        scheduledActions,
        chargeDefinitionsMappedByChargeAction,
        chargeDefinitionsMappedByAccrueAction);
  }

  private List<ScheduledCharge> getScheduledCharges(
      final String productIdentifier,
      final List<ScheduledAction> scheduledActions,
      final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction,
      final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction) {
    return scheduledActions.stream()
        .flatMap(scheduledAction ->
            getChargeDefinitionStream(
                chargeDefinitionsMappedByChargeAction,
                chargeDefinitionsMappedByAccrueAction,
                scheduledAction)
                .map(chargeDefinition -> new ScheduledCharge(
                    scheduledAction,
                    chargeDefinition,
                    findChargeRange(productIdentifier, chargeDefinition))))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static class Segment {
    final String identifier;
    final BigDecimal lowerBound;
    final Optional<BigDecimal> upperBound;

    private Segment(final String segmentIdentifier,
            final BigDecimal lowerBound,
            final Optional<BigDecimal> upperBound) {
      this.identifier = segmentIdentifier;
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
    }

    BigDecimal getLowerBound() {
      return lowerBound;
    }

    Optional<BigDecimal> getUpperBound() {
      return upperBound;
    }

    @Override
    public String toString() {
      return "Segment{" +
          "identifier='" + identifier + '\'' +
          ", lowerBound=" + lowerBound +
          ", upperBound=" + upperBound +
          '}';
    }
  }

  Optional<ChargeRange> findChargeRange(final String productIdentifier, final ChargeDefinition chargeDefinition) {
    if ((chargeDefinition.getForSegmentSet() == null) ||
        (chargeDefinition.getFromSegment() == null) ||
        (chargeDefinition.getToSegment() == null))
      return Optional.empty();

    final List<BalanceSegmentEntity> segmentSet = balanceSegmentRepository.findByProductIdentifierAndSegmentSetIdentifier(productIdentifier, chargeDefinition.getForSegmentSet())
        .sorted(Comparator.comparing(BalanceSegmentEntity::getLowerBound))
        .collect(Collectors.toList());

    final Map<String, Segment> segments = Stream.iterate(0, i -> i + 1).limit(segmentSet.size())
        .map(i -> new Segment(
            segmentSet.get(i).getSegmentIdentifier(),
            segmentSet.get(i).getLowerBound(),
            Optional.ofNullable(i + 1 < segmentSet.size() ?
                segmentSet.get(i + 1).getLowerBound() :
                null)
        ))
        .collect(Collectors.toMap(x -> x.identifier, x -> x));


    final Optional<Segment> fromSegment = Optional.ofNullable(segments.get(chargeDefinition.getFromSegment()));
    final Optional<Segment> toSegment = Optional.ofNullable(segments.get(chargeDefinition.getToSegment()));
    if (!fromSegment.isPresent() || !toSegment.isPresent())
      return Optional.empty();

    return Optional.of(new ChargeRange(fromSegment.get().getLowerBound(), toSegment.get().getUpperBound()));
  }

  private static Stream<ChargeDefinition> getChargeDefinitionStream(
      final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction,
      final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction,
      final ScheduledAction scheduledAction) {
    final List<ChargeDefinition> chargeMappingList = chargeDefinitionsMappedByChargeAction
        .get(scheduledAction.getAction().name());
    Stream<ChargeDefinition> chargeMapping = chargeMappingList == null ? Stream.empty() : chargeMappingList.stream();
    if (chargeMapping == null)
      chargeMapping = Stream.empty();

    final List<ChargeDefinition> accrueMappingList = chargeDefinitionsMappedByAccrueAction
        .get(scheduledAction.getAction().name());
    Stream<ChargeDefinition> accrueMapping = accrueMappingList == null ? Stream.empty() : accrueMappingList.stream();
    if (accrueMapping == null)
      accrueMapping = Stream.empty();

    return Stream.concat(
        accrueMapping.sorted(ScheduledChargeComparator::proportionalityApplicationOrder),
        chargeMapping.sorted(ScheduledChargeComparator::proportionalityApplicationOrder));
  }
}
