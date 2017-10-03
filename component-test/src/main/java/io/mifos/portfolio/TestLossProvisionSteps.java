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

import io.mifos.individuallending.api.v1.domain.product.LossProvisionConfiguration;
import io.mifos.individuallending.api.v1.domain.product.LossProvisionStep;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.portfolio.api.v1.domain.Product;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    final LossProvisionConfiguration lossProvisionConfigurationAsSaved = individualLending.getLossProvisionConfiguration(product.getIdentifier());
    Assert.assertEquals(lossProvisionConfiguration, lossProvisionConfigurationAsSaved);
  }
}