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
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.client.ChargeDefinitionIsReadOnly;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    final Map<Boolean, List<ChargeDefinition>> chargeIdentifersPartitionedByReadOnly
        = charges.stream().collect(Collectors.partitioningBy(ChargeDefinition::isReadOnly, Collectors.toList()));
    final Set<String> readOnlyChargeDefinitionIdentifiers
        = chargeIdentifersPartitionedByReadOnly.get(true).stream()
        .map(ChargeDefinition::getIdentifier)
        .collect(Collectors.toSet());
    final Set<String> changeableChargeDefinitionIdentifiers
        = chargeIdentifersPartitionedByReadOnly.get(false).stream()
        .map(ChargeDefinition::getIdentifier)
        .collect(Collectors.toSet());

    final Set<String> expectedReadOnlyChargeDefinitionIdentifiers = Stream.of(
        ChargeIdentifiers.ALLOW_FOR_WRITE_OFF_ID,
        ChargeIdentifiers.LOAN_FUNDS_ALLOCATION_ID,
        ChargeIdentifiers.RETURN_DISBURSEMENT_ID,
        ChargeIdentifiers.DISBURSE_PAYMENT_ID,
        ChargeIdentifiers.TRACK_DISBURSAL_PAYMENT_ID,
        ChargeIdentifiers.TRACK_RETURN_PRINCIPAL_ID,
        ChargeIdentifiers.REPAYMENT_ID)
        .collect(Collectors.toSet());
    final Set<String> expectedChangeableChargeDefinitionIdentifiers = Stream.of(
        ChargeIdentifiers.DISBURSEMENT_FEE_ID,
        ChargeIdentifiers.INTEREST_ID,
        ChargeIdentifiers.LATE_FEE_ID,
        ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID,
        ChargeIdentifiers.PROCESSING_FEE_ID)
        .collect(Collectors.toSet());

    Assert.assertEquals(expectedReadOnlyChargeDefinitionIdentifiers, readOnlyChargeDefinitionIdentifiers);
    Assert.assertEquals(expectedChangeableChargeDefinitionIdentifiers, changeableChargeDefinitionIdentifiers);
  }

  @Test
  public void shouldDeleteChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final ChargeDefinition chargeDefinitionToDelete = new ChargeDefinition();
    chargeDefinitionToDelete.setAmount(BigDecimal.TEN);
    chargeDefinitionToDelete.setIdentifier("blah");
    chargeDefinitionToDelete.setName("blah blah");
    chargeDefinitionToDelete.setDescription("blah blah blah");
    chargeDefinitionToDelete.setChargeAction(Action.APPROVE.name());
    chargeDefinitionToDelete.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    chargeDefinitionToDelete.setToAccountDesignator(AccountDesignators.ARREARS_ALLOWANCE);
    chargeDefinitionToDelete.setFromAccountDesignator(AccountDesignators.INTEREST_ACCRUAL);
    portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier())));

    portfolioManager.deleteChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_PRODUCT_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier())));

    try {
      portfolioManager.getChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier());
      //noinspection ConstantConditions
      Assert.assertFalse(true);
    }
    catch (final NotFoundException ignored) { }
  }

  @Test(expected = ChargeDefinitionIsReadOnly.class)
  public void shouldNotDeleteReadOnlyChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    portfolioManager.deleteChargeDefinition(product.getIdentifier(), ChargeIdentifiers.ALLOW_FOR_WRITE_OFF_ID);
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

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotCreateReadOnlyChargeDefinition() throws InterruptedException {
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
    chargeDefinition.setReadOnly(true);

    portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinition);
  }


  @Test
  public void shouldChangeInterestChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final ChargeDefinition interestChargeDefinition
        = portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.INTEREST_ID);
    interestChargeDefinition.setAmount(Fixture.INTEREST_RATE);

    portfolioManager.changeChargeDefinition(
        product.getIdentifier(),
        interestChargeDefinition.getIdentifier(),
        interestChargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), interestChargeDefinition.getIdentifier())));

    final ChargeDefinition chargeDefinitionAsChanged
        = portfolioManager.getChargeDefinition(product.getIdentifier(), interestChargeDefinition.getIdentifier());

    Assert.assertEquals(interestChargeDefinition, chargeDefinitionAsChanged);
  }

  @Test
  public void shouldNotChangeDisbursalChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final ChargeDefinition originalDisbursalChargeDefinition
        = portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSE_PAYMENT_ID);

    final ChargeDefinition disbursalChargeDefinition
        = portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSE_PAYMENT_ID);
    disbursalChargeDefinition.setProportionalTo(ChargeProportionalDesignator.NOT_PROPORTIONAL.getValue());
    disbursalChargeDefinition.setReadOnly(false);

    try {
      portfolioManager.changeChargeDefinition(
          product.getIdentifier(),
          disbursalChargeDefinition.getIdentifier(),
          disbursalChargeDefinition);
      Assert.fail("Changing a readonly charge definition should fail.");
    }
    catch (final ChargeDefinitionIsReadOnly ignore) { }

    final ChargeDefinition chargeDefinitionAsChanged
        = portfolioManager.getChargeDefinition(product.getIdentifier(), disbursalChargeDefinition.getIdentifier());

    Assert.assertEquals(originalDisbursalChargeDefinition, chargeDefinitionAsChanged);
  }
}
