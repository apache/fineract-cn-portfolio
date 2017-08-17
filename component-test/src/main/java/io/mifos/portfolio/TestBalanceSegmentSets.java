/*
 * Copyright 2017 Kuelap, Inc.
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

import io.mifos.portfolio.api.v1.client.ProductInUseException;
import io.mifos.portfolio.api.v1.domain.BalanceSegmentSet;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.BalanceSegmentSetEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * @author Myrle Krantz
 */
public class TestBalanceSegmentSets extends AbstractPortfolioTest {
  @Test
  public void test() throws InterruptedException {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifer("bss"));
    balanceSegmentSet.setSegments(new TreeSet<>(Arrays.asList(BigDecimal.TEN, BigDecimal.valueOf(10_000))));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("a", "b", "c"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    final BalanceSegmentSet createdBalanceSegmentSet = portfolioManager.getBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertEquals(balanceSegmentSet, createdBalanceSegmentSet);

    balanceSegmentSet.setSegments(new TreeSet<>(Arrays.asList(BigDecimal.valueOf(100), BigDecimal.valueOf(10_000))));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("a", "b", "c"));

    portfolioManager.changeBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier(), balanceSegmentSet);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    final BalanceSegmentSet changedBalanceSegmentSet = portfolioManager.getBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertEquals(balanceSegmentSet, changedBalanceSegmentSet);

    portfolioManager.deleteBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    enableProduct(product);

    createCase(product.getIdentifier());

    try {
      portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
      Assert.fail("shouldn't be able to create a balance segment set in a product in use.");
    }
    catch (final ProductInUseException ignored) { }
  }
}
