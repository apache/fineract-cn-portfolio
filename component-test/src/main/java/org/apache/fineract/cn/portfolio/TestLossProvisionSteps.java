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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionConfiguration;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestLossProvisionSteps extends AbstractPortfolioTest {
  @Test
  public void shouldChangeAndGetLossProvisionSteps() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});

    final List<LossProvisionStep> lossProvisionSteps = new ArrayList<>();
    lossProvisionSteps.add(new LossProvisionStep(0, BigDecimal.valueOf(1_00, 2)));
    lossProvisionSteps.add(new LossProvisionStep(1, BigDecimal.valueOf(9_00, 2)));
    lossProvisionSteps.add(new LossProvisionStep(30, BigDecimal.valueOf(35_00, 2)));
    lossProvisionSteps.add(new LossProvisionStep(60, BigDecimal.valueOf(55_00, 2)));
    final LossProvisionConfiguration lossProvisionConfiguration = new LossProvisionConfiguration(lossProvisionSteps);

    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration);
    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.PUT_LOSS_PROVISION_STEPS, product.getIdentifier()));
    Thread.sleep(2000);

    final LossProvisionConfiguration lossProvisionConfigurationAsSaved = individualLending.getLossProvisionConfiguration(product.getIdentifier());
    Assert.assertEquals(lossProvisionConfiguration, lossProvisionConfigurationAsSaved);


    final List<LossProvisionStep> lossProvisionSteps2 = new ArrayList<>();
    lossProvisionSteps2.add(new LossProvisionStep(0, BigDecimal.valueOf(2_00, 2)));
    lossProvisionSteps2.add(new LossProvisionStep(1, BigDecimal.valueOf(15_00, 2)));
    lossProvisionSteps2.add(new LossProvisionStep(30, BigDecimal.valueOf(35_00, 2)));
    lossProvisionSteps2.add(new LossProvisionStep(60, BigDecimal.valueOf(55_00, 2)));
    final LossProvisionConfiguration lossProvisionConfiguration2 = new LossProvisionConfiguration(lossProvisionSteps2);

    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration2);
    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.PUT_LOSS_PROVISION_STEPS, product.getIdentifier()));
    Thread.sleep(2000);

    final LossProvisionConfiguration lossProvisionConfiguration2AsSaved = individualLending.getLossProvisionConfiguration(product.getIdentifier());
    Assert.assertEquals(lossProvisionConfiguration2, lossProvisionConfiguration2AsSaved);


    final LossProvisionConfiguration lossProvisionConfiguration3 = new LossProvisionConfiguration(Collections.emptyList());

    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration3);
    Assert.assertTrue(eventRecorder.wait(IndividualLoanEventConstants.PUT_LOSS_PROVISION_STEPS, product.getIdentifier()));
    Thread.sleep(2000);

    //TODO: final LossProvisionConfiguration lossProvisionConfiguration3AsSaved = individualLending.getLossProvisionConfiguration(product.getIdentifier());
    //Assert.assertEquals(lossProvisionConfiguration3, lossProvisionConfiguration3AsSaved);
  }
}