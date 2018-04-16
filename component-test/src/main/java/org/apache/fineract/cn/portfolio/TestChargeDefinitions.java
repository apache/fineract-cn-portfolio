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
package org.apache.fineract.cn.portfolio;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.ChargeDefinitionEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.api.util.NotFoundException;
import org.junit.Assert;
import org.junit.Test;

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

    final Set<String> expectedChangeableChargeDefinitionIdentifiers = Stream.of(
        ChargeIdentifiers.DISBURSEMENT_FEE_ID,
        ChargeIdentifiers.LATE_FEE_ID,
        ChargeIdentifiers.LOAN_ORIGINATION_FEE_ID,
        ChargeIdentifiers.PROCESSING_FEE_ID)
        .collect(Collectors.toSet());

    Assert.assertTrue(readOnlyChargeDefinitionIdentifiers.isEmpty()); //Not using readonly any more.  Simply not returning charges instead.
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
    chargeDefinitionToDelete.setToAccountDesignator(AccountDesignators.GENERAL_LOSS_ALLOWANCE);
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

  @Test(expected = NotFoundException.class)
  public void shouldNotDeleteReadOnlyChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    portfolioManager.deleteChargeDefinition(product.getIdentifier(), ChargeIdentifiers.INTEREST_ID);
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
  public void shouldChangeDisbursementFeeChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    final ChargeDefinition disbursementFeeDefinition
        = portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSEMENT_FEE_ID);
    disbursementFeeDefinition.setAmount(BigDecimal.valueOf(10_0000, 4));

    portfolioManager.changeChargeDefinition(
        product.getIdentifier(),
        disbursementFeeDefinition.getIdentifier(),
        disbursementFeeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(product.getIdentifier(), disbursementFeeDefinition.getIdentifier())));

    final ChargeDefinition chargeDefinitionAsChanged
        = portfolioManager.getChargeDefinition(product.getIdentifier(), disbursementFeeDefinition.getIdentifier());

    Assert.assertEquals(disbursementFeeDefinition, chargeDefinitionAsChanged);
  }

  @Test
  public void shouldNotGetDisbursalChargeDefinition() throws InterruptedException {
    final Product product = createProduct();

    try {
      portfolioManager.getChargeDefinition(product.getIdentifier(), ChargeIdentifiers.DISBURSE_PAYMENT_ID);
      Assert.fail("Getting a charge derived from configuration should fail.");
    }
    catch (final NotFoundException ignore) { }
  }
}
