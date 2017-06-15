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
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.individuallending.api.v1.domain.product.ProductParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.client.ProductAlreadyExistsException;
import io.mifos.portfolio.api.v1.client.ProductDefinitionIncomplete;
import io.mifos.portfolio.api.v1.client.ProductInUseException;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
public class TestProducts extends AbstractPortfolioTest {

  @Test
  public void shouldCreateAndEnableProduct() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

    Assert.assertEquals(product, productAsSaved);

    Assert.assertFalse(portfolioManager.getProductEnabled(product.getIdentifier()));

    {
      final ProductPage productsPage = portfolioManager.getProducts(true, null, 0, 100, null, null);
      Assert.assertTrue(productsPage.getElements().contains(productAsSaved));
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(true, product.getIdentifier().substring(2, 5), 0, 100, null, null);
      Assert.assertTrue(productsPage.getElements().contains(productAsSaved));
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(false, null, 0, 100, null, null);
      Assert.assertFalse(productsPage.getElements().contains(productAsSaved));
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(false, product.getIdentifier().substring(2, 5), 0, 100, null, null);
      Assert.assertFalse(productsPage.getElements().contains(productAsSaved));
    }

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    Assert.assertTrue(portfolioManager.getProductEnabled(product.getIdentifier()));

    {
      final ProductPage productsPage = portfolioManager.getProducts(false, null, 0, 100, null, null);
      Assert.assertTrue(productsPage.getElements().contains(productAsSaved));
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(false, product.getIdentifier().substring(2, 5), 0, 100, null, null);
      Assert.assertTrue(productsPage.getElements().contains(productAsSaved));
    }
  }

  @Test
  public void shouldCorrectlyOrderProducts() throws InterruptedException {

    final Product productA = createAdjustedProduct(x -> {
      x.setIdentifier("aaaaaaaa");
      x.setName("ZZZZZZZ");
    });
    final Product productZ = createAdjustedProduct(x -> {
      x.setIdentifier("zzzzzzzz");
      x.setName("AAAAAAA");
    });

    final Product productASaved = portfolioManager.getProduct(productA.getIdentifier());
    final Product productZSaved = portfolioManager.getProduct(productZ.getIdentifier());

    {
      final ProductPage productsPage = portfolioManager.getProducts(true, null, 0, 100, null, null);
      Assert.assertEquals(productZSaved, productsPage.getElements().get(0)); //Modified by ordering.
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(true, null, 0, 100, "identifier", "ASC");
      Assert.assertEquals(productASaved, productsPage.getElements().get(0)); //Alphabetic ordering by identifier
    }

    {
      final ProductPage productsPage = portfolioManager.getProducts(true, null, 0, 100, "name", "DESC");
      Assert.assertEquals(productASaved, productsPage.getElements().get(0)); //Alphabetic ordering by name. Descending.
    }
  }

  @Test
  public void badArgumentsToSortOrderAndDirectionShouldThrow() throws InterruptedException {
    try {
      portfolioManager.getProducts(true, null, 0, 100, null, "asc");
      Assert.fail("Should've thrown");
    }
    catch (final IllegalArgumentException ignored) { }

    try {
      portfolioManager.getProducts(true, null, 0, 100, null, "ACS");
      Assert.fail("Should've thrown");
    }
    catch (final IllegalArgumentException ignored) { }

    try {
      portfolioManager.getProducts(true, null, 0, 100, "non-existent-column", null);
      Assert.fail("Should've thrown");
    }
    catch (final IllegalArgumentException ignored) { }

    try {
      portfolioManager.getProducts(true, null, 0, 100, "", null);
      Assert.fail("Should've thrown");
    }
    catch (final IllegalArgumentException ignored) { }
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
    final Product product = createAdjustedProduct(x -> x.setAccountAssignments(Collections.emptySet()));

    //Add account assignments
    final Set<AccountAssignment> incompleteAccountAssignments = portfolioManager.getIncompleteAccountAssignments(product.getIdentifier());

    final Set<AccountAssignment> accountAssignments = incompleteAccountAssignments.stream()
            .map(x -> new AccountAssignment(x.getDesignator(), testEnvironment.generateUniqueIdentifer("account")))
            .collect(Collectors.toSet());

    product.setAccountAssignments(accountAssignments);

    TimeUnit.SECONDS.sleep(3);

    final TimeStampChecker timeStampChecker = TimeStampChecker.roughlyNow();
    portfolioManager.changeProduct(product.getIdentifier(), product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT, product.getIdentifier()));

    {
      final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

      Assert.assertEquals(product, productAsSaved);
      Assert.assertEquals(TEST_USER, productAsSaved.getLastModifiedBy());
      timeStampChecker.assertCorrect(productAsSaved.getLastModifiedOn());
    }

    //Remove account assignments
    product.setAccountAssignments(Collections.emptySet());
    portfolioManager.changeProduct(product.getIdentifier(), product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT, product.getIdentifier()));

    {
      final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

      Assert.assertEquals(product, productAsSaved);
    }
  }

  @Test
  public void shouldRemoveProductAccountAssignments() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});

    final Set<AccountAssignment> incompleteAccountAssignments = portfolioManager.getIncompleteAccountAssignments(product.getIdentifier());
    Assert.assertTrue(incompleteAccountAssignments.isEmpty());

    product.setAccountAssignments(Collections.emptySet());
    portfolioManager.changeProduct(product.getIdentifier(), product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT, product.getIdentifier()));

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());
    Assert.assertTrue("Account assignments should be empty, but contain: " + productAsSaved.getAccountAssignments(),
            productAsSaved.getAccountAssignments().isEmpty());
    Assert.assertEquals(product, productAsSaved);

    final Set<AccountAssignment> incompleteAccountAssignmentsAfterChange
            = portfolioManager.getIncompleteAccountAssignments(product.getIdentifier());
    Assert.assertFalse("Incomplete account assignments should not be empty, but is. (Beware the double negative.)",
            incompleteAccountAssignmentsAfterChange.isEmpty());
  }

  @Test
  public void incompleteProductDefinitionCantBeActivated() throws InterruptedException {
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
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
            new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinitionContainingIncompleteAccountAssignment.getIdentifier())));

    try {
      portfolioManager.enableProduct(product.getIdentifier(), true);
      Assert.fail("Enable should fail if product definition contains an non-existant account assignment.");
    }
    catch (ProductDefinitionIncomplete ignored) {

    }

  }

  @Test(expected = IllegalArgumentException.class)
  public void nonExistentPatternPackageShouldThrow() throws InterruptedException {
    createAdjustedProduct(product -> product.setPatternPackage("be.bop.do.wap"));
  }

  @Test(expected = ProductAlreadyExistsException.class)
  public void duplicateProductIdentifierShouldThrow() throws InterruptedException {
    createAdjustedProduct(product -> product.setIdentifier("ditto"));
    createAdjustedProduct(product -> product.setIdentifier("ditto"));
  }

  @Test
  public void shouldFailToChangeProductAfterCaseHasBeenCreated() throws InterruptedException {
    final Product product = createAndEnableProduct();

    createCase(product.getIdentifier());

    final Product slightlyChangedProduct = Fixture.createAdjustedProduct(x -> x.setDescription("changed description."));
    slightlyChangedProduct.setIdentifier(product.getIdentifier());
    try {
      portfolioManager.changeProduct(product.getIdentifier(), slightlyChangedProduct);
      Assert.fail("This should throw a ProductInUseException.");
    }
    catch (final ProductInUseException ignore) {
    }

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());

    Assert.assertEquals(product, productAsSaved);
    Assert.assertNotEquals(slightlyChangedProduct, productAsSaved);
    Assert.assertEquals(TEST_USER, productAsSaved.getLastModifiedBy());

    portfolioManager.enableProduct(product.getIdentifier(), false);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    Assert.assertFalse(portfolioManager.getProductEnabled(product.getIdentifier()));
  }

  @Test(expected = ProductInUseException.class)
  public void shouldFailToDeleteProductAfterCaseHasBeenCreated() throws InterruptedException {
    final Product product = createAndEnableProduct();

    createCase(product.getIdentifier());

    portfolioManager.deleteProduct(product.getIdentifier());
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailToDeleteNonExistentProduct() throws InterruptedException {
    portfolioManager.deleteProduct("habberdash");
  }

  @Test
  public void shouldDeleteProduct() throws InterruptedException {
    final Product product = createAndEnableProduct();

    try {
      portfolioManager.deleteProduct(product.getIdentifier());
      Assert.fail("Product is enabled.  It shouldn't be possible to delete it.");
    }
    catch (final ProductInUseException ignored) {}

    portfolioManager.enableProduct(product.getIdentifier(), false);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    portfolioManager.deleteProduct(product.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_PRODUCT, product.getIdentifier()));

    try {
      portfolioManager.getProduct(product.getIdentifier());
      Assert.fail("product should not be found after delete.");
    }
    catch (final NotFoundException ignored) {}
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