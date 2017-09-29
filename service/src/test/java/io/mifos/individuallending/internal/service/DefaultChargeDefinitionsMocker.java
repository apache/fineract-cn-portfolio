package io.mifos.individuallending.internal.service;

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
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
