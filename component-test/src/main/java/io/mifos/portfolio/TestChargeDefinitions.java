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
package io.mifos.portfolio;

import io.mifos.core.api.util.NotFoundException;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class TestChargeDefinitions extends AbstractPortfolioTest {
  @Test
  public void shouldProvisionAndListChargeDefinitions() throws InterruptedException {
    final Product product = createProduct();

    final List<ChargeDefinition> charges = portfolioManager.getAllChargeDefinitionsForProduct(product.getIdentifier());
    final Set<String> chargeDefinitionIdentifiers = charges.stream().map(ChargeDefinition::getIdentifier).collect(Collectors.toSet());
    final Set<String> expectedChargeDefinitionIdentifiers = Stream.of(
            ChargeIdentifiers.ALLOW_FOR_WRITE_OFF_ID,
            ChargeIdentifiers.DISBURSEMENT_FEE_ID,
            ChargeIdentifiers.INTEREST_ID,
            ChargeIdentifiers.LATE_FEE_ID,
            ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID,
            ChargeIdentifiers.PROCESSING_FEE_ID,
            ChargeIdentifiers.RETURN_DISBURSEMENT_ID)
            .collect(Collectors.toSet());
    Assert.assertEquals(expectedChargeDefinitionIdentifiers, chargeDefinitionIdentifiers);
  }

  @Test
  public void shouldDeleteChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final List<ChargeDefinition> charges = portfolioManager.getAllChargeDefinitionsForProduct(product.getIdentifier());
    final ChargeDefinition chargeDefinitionToDelete = charges.get(0);
    portfolioManager.deleteChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_PRODUCT_CHARGE_DEFINITION,
            new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier())));

    try {
      portfolioManager.getChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier());
      Assert.assertFalse(true);
    }
    catch (final NotFoundException ignored) { }
  }

  @Test
  public void shouldCreateChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final ChargeDefinition chargeDefinition = new ChargeDefinition();
    chargeDefinition.setIdentifier("rando123456");
    chargeDefinition.setName("ditto12356");
    chargeDefinition.setFromAccountDesignator("1234-4321");
    chargeDefinition.setToAccountDesignator("4321-1234");
    chargeDefinition.setAmount(BigDecimal.ONE.setScale(4, BigDecimal.ROUND_UNNECESSARY));
    chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    chargeDefinition.setChargeAction(Action.OPEN.name());
    chargeDefinition.setDescription("who cares what the description is?");

    portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
            new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinition.getIdentifier())));

    final ChargeDefinition chargeDefinitionAsCreated = portfolioManager.getChargeDefinition(product.getIdentifier(), chargeDefinition.getIdentifier());
    Assert.assertEquals(chargeDefinition, chargeDefinitionAsCreated);
  }
}
