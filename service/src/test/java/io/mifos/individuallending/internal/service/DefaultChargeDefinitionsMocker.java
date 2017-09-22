package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultChargeDefinitionsMocker {
  private static List<ChargeDefinition> charges() {
    final List<ChargeDefinition> ret = IndividualLendingPatternFactory.requiredIndividualLoanCharges();
    ret.addAll(IndividualLendingPatternFactory.defaultIndividualLoanCharges());
    return ret;
  }

  public static ChargeDefinitionService getChargeDefinitionService(final List<ChargeDefinition> changedCharges) {
    final Map<String, ChargeDefinition> changedChargesMap = changedCharges.stream()
        .collect(Collectors.toMap(ChargeDefinition::getIdentifier, x -> x));

    final List<ChargeDefinition> defaultChargesWithFeesReplaced =
        charges().stream().map(x -> changedChargesMap.getOrDefault(x.getIdentifier(), x))
            .collect(Collectors.toList());


    final ChargeDefinitionService chargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    final Map<String, List<ChargeDefinition>> chargeDefinitionsByChargeAction = defaultChargesWithFeesReplaced.stream()
        .collect(Collectors.groupingBy(ChargeDefinition::getChargeAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    final Map<String, List<ChargeDefinition>> chargeDefinitionsByAccrueAction = defaultChargesWithFeesReplaced.stream()
        .filter(x -> x.getAccrueAction() != null)
        .collect(Collectors.groupingBy(ChargeDefinition::getAccrueAction,
            Collectors.mapping(x -> x, Collectors.toList())));
    Mockito.doReturn(chargeDefinitionsByChargeAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByChargeAction(Mockito.any());
    Mockito.doReturn(chargeDefinitionsByAccrueAction).when(chargeDefinitionServiceMock).getChargeDefinitionsMappedByAccrueAction(Mockito.any());

    return chargeDefinitionServiceMock;
  }
}
