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

import com.google.gson.Gson;
import io.mifos.individuallending.api.v1.domain.product.ProductParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.client.ProductAlreadyExistsException;
import io.mifos.portfolio.api.v1.client.ProductDefinitionIncomplete;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Myrle Krantz
 */
public class TestProducts extends AbstractPortfolioTest {

  @Test
  public void shouldCreateAndEnableProduct() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});

    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_PRODUCT, product.getIdentifier()));

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

    Assert.assertEquals(product, productAsSaved);

    Assert.assertFalse(portfolioManager.getProductEnabled(product.getIdentifier()));

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    Assert.assertTrue(portfolioManager.getProductEnabled(product.getIdentifier()));
  }

  @Test
  public void shouldCreateProductWithMaximumLengthEverything() throws InterruptedException {
    final Product product = getTestProductWithMaximumLengthEverything();
    portfolioManager.createProduct(product);

    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_PRODUCT, product.getIdentifier()));

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

    Assert.assertEquals(product, productAsSaved);

  }

  @Test
  public void shouldChangeProductAccountAssignments() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_PRODUCT, product.getIdentifier()));

    final Set<AccountAssignment> accountAssignments = new HashSet<>(product.getAccountAssignments());
    accountAssignments.add(new AccountAssignment("xyz", "002-011"));
    accountAssignments.add(new AccountAssignment("mno", "002-012"));
    product.setAccountAssignments(accountAssignments);

    TimeUnit.SECONDS.sleep(3);

    final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
    portfolioManager.changeProduct(product.getIdentifier(), product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT, product.getIdentifier()));

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

    Assert.assertEquals(product, productAsSaved);
    Assert.assertEquals(TEST_USER, productAsSaved.getLastModifiedBy());
    TestCases.assertTimeStampWithinDelta(productAsSaved.getLastModifiedOn(), now, Duration.ofSeconds(3));
  }

  @Test
  public void incompleteProductDefinitionCantBeActivated() {
    final Product product = Fixture.createAdjustedProduct(x -> {});
    portfolioManager.createProduct(product);
    final ChargeDefinition chargeDefinitionContainingIncompleteAccountAssignment = new ChargeDefinition();
    chargeDefinitionContainingIncompleteAccountAssignment.setIdentifier("rando123456");
    chargeDefinitionContainingIncompleteAccountAssignment.setName("ditto12356");
    chargeDefinitionContainingIncompleteAccountAssignment.setFromAccountDesignator("not-there");
    chargeDefinitionContainingIncompleteAccountAssignment.setToAccountDesignator("not-here");
    chargeDefinitionContainingIncompleteAccountAssignment.setAmount(BigDecimal.ONE);
    chargeDefinitionContainingIncompleteAccountAssignment.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    chargeDefinitionContainingIncompleteAccountAssignment.setChargeAction(Action.OPEN.name());
    chargeDefinitionContainingIncompleteAccountAssignment.setDescription("who cares what the description is?");
    portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinitionContainingIncompleteAccountAssignment);

    try {
      portfolioManager.enableProduct(product.getIdentifier(), true);
      Assert.fail("Enable should fail if product definition contains an non-existant account assignment.");
    }
    catch (ProductDefinitionIncomplete ignored) {

    }

  }

  @Test(expected = IllegalArgumentException.class)
  public void nonExistentPatternPackageShouldThrow()
  {
    createAdjustedProduct(product -> product.setPatternPackage("be.bop.do.wap"));
  }

  @Test(expected = ProductAlreadyExistsException.class)
  public void duplicateProductIdentifierShouldThrow() throws InterruptedException {
    final Product productToCreate = createAdjustedProduct(product -> product.setIdentifier("ditto"));

    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_PRODUCT, productToCreate.getIdentifier()));

    createAdjustedProduct(product -> product.setIdentifier("ditto"));
  }

  private Product createAdjustedProduct(final Consumer<Product> adjustment) {
    final Product product = Fixture.createAdjustedProduct(adjustment);
    portfolioManager.createProduct(product);
    return product;
  }


  private Product getTestProductWithMaximumLengthEverything()
  {
    final Product product = new Product();
    product.setPatternPackage("io.mifos.individuallending.api.v1");
    product.setIdentifier(StringUtils.repeat("x", 8));
    product.setName(StringUtils.repeat("x", 256));
    product.setDescription(StringUtils.repeat("x", 4096));
    product.setTermRange(new TermRange(ChronoUnit.MONTHS, 12));
    product.setBalanceRange(new BalanceRange(Fixture.fixScale(BigDecimal.ZERO), Fixture.fixScale(new BigDecimal(10000))));
    product.setInterestRange(new InterestRange(new BigDecimal("999.98"), new BigDecimal("999.99")));
    product.setInterestBasis(InterestBasis.CURRENT_BALANCE);
    product.setCurrencyCode("XTS");
    product.setMinorCurrencyUnitDigits(4);

    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(StringUtils.repeat("x", 32), StringUtils.repeat("x", 8)));
    product.setAccountAssignments(accountAssignments);

    final ProductParameters productParameters = new ProductParameters();

    productParameters.setMoratoriums(Collections.emptyList());
    productParameters.setMaximumDispersalCount(5);

    final Gson gson = new Gson();
    product.setParameters(gson.toJson(productParameters));
    return product;
  }
}

//TODO: Add test for removing account assignments.