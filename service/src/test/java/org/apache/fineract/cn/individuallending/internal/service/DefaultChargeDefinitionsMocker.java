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

import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultChargeDefinitionsMocker {
  private static Stream<ChargeDefinition> charges() {
    return Stream.concat(ChargeDefinitionService.defaultConfigurableIndividualLoanCharges(),
        ChargeDefinitionService.individualLoanChargesDerivedFromConfiguration());
  }

  public static ChargeDefinitionService getChargeDefinitionService(final List<ChargeDefinition> changedCharges) {
    final Map<String, ChargeDefinition> changedChargesMap = changedCharges.stream()
        .collect(Collectors.toMap(ChargeDefinition::getIdentifier, x -> x));

    final List<ChargeDefinition> defaultChargesWithFeesReplaced =
        charges().map(x -> changedChargesMap.getOrDefault(x.getIdentifier(), x))
            .collect(Collectors.toList());


    final ChargeDefinitionService configurableChargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    final Map<String, List<ChargeDefinition>> chargeDefinitionsByChargeAction = defaultChargesWithFeesReplaced.stream()
        .collect(Collectors.groupingBy(ChargeDefinition::getChargeAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    final Map<String, List<ChargeDefinition>> chargeDefinitionsByAccrueAction = defaultChargesWithFeesReplaced.stream()
        .filter(x -> x.getAccrueAction() != null)
        .collect(Collectors.groupingBy(ChargeDefinition::getAccrueAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    Mockito.doReturn(chargeDefinitionsByChargeAction).when(configurableChargeDefinitionServiceMock).getChargeDefinitionsMappedByChargeAction(Mockito.any());
    Mockito.doReturn(chargeDefinitionsByAccrueAction).when(configurableChargeDefinitionServiceMock).getChargeDefinitionsMappedByAccrueAction(Mockito.any());

    return configurableChargeDefinitionServiceMock;
  }
}
