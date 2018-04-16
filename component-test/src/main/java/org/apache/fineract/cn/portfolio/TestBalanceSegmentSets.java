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

import org.apache.fineract.cn.portfolio.api.v1.client.ProductInUseException;
import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.BalanceSegmentSetEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestBalanceSegmentSets extends AbstractPortfolioTest {
  @Test
  public void testBalanceSegmentSetManagement() throws InterruptedException {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifer("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
        BigDecimal.ZERO.setScale(4, BigDecimal.ROUND_HALF_EVEN),
        BigDecimal.TEN.setScale(4, BigDecimal.ROUND_HALF_EVEN),
        BigDecimal.valueOf(10_000_0000, 4)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("abc", "def", "ghi"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    final BalanceSegmentSet createdBalanceSegmentSet = portfolioManager.getBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertEquals(balanceSegmentSet, createdBalanceSegmentSet);

    balanceSegmentSet.setSegments(Arrays.asList(
        BigDecimal.ZERO.setScale(4, BigDecimal.ROUND_HALF_EVEN),
        BigDecimal.valueOf(100_0000, 4),
        BigDecimal.valueOf(10_000_0000, 4)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("abc", "def", "ghi"));

    portfolioManager.changeBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier(), balanceSegmentSet);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    final BalanceSegmentSet changedBalanceSegmentSet = portfolioManager.getBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertEquals(balanceSegmentSet, changedBalanceSegmentSet);


    final BalanceSegmentSet balanceSegmentSet2 = new BalanceSegmentSet();
    balanceSegmentSet2.setIdentifier(testEnvironment.generateUniqueIdentifer("bss"));
    balanceSegmentSet2.setSegments(Arrays.asList(
        BigDecimal.ZERO.setScale(4, BigDecimal.ROUND_HALF_EVEN),
        BigDecimal.TEN.setScale(4, BigDecimal.ROUND_HALF_EVEN),
        BigDecimal.valueOf(10_000_0000, 4)));
    balanceSegmentSet2.setSegmentIdentifiers(Arrays.asList("abc", "def", "ghi"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet2);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet2.getIdentifier())));

    final List<BalanceSegmentSet> balanceSegmentSets = portfolioManager.getAllBalanceSegmentSets(product.getIdentifier());
    Assert.assertTrue(balanceSegmentSets.contains(balanceSegmentSet));
    Assert.assertTrue(balanceSegmentSets.contains(balanceSegmentSet2));
    Assert.assertTrue(balanceSegmentSets.size() == 2);

    portfolioManager.deleteBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier())));

    final List<BalanceSegmentSet> balanceSegmentSetsAfterDelete = portfolioManager.getAllBalanceSegmentSets(product.getIdentifier());
    Assert.assertTrue(balanceSegmentSets.contains(balanceSegmentSet2));
    Assert.assertTrue(balanceSegmentSetsAfterDelete.size() == 1);

    enableProduct(product);

    createCase(product.getIdentifier());

    try {
      portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
      Assert.fail("shouldn't be able to create a balance segment set in a product in use.");
    }
    catch (final ProductInUseException ignored) { }
  }
}
