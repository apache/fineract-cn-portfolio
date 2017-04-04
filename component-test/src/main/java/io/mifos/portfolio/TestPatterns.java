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
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Pattern;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestPatterns extends AbstractPortfolioTest {
  @Test
  public void shouldListPatterns() {
    final List<Pattern> allPatterns = portfolioManager.getAllPatterns();
    Assert.assertNotNull(allPatterns);
    Assert.assertTrue(allPatterns.size() > 0);
  }

  @Test
  public void shouldReturnDefaultCharges() {
    final List<ChargeDefinition> chargeDefinitions =
            portfolioManager.getAllDefaultChargeDefinitionsForPattern("io.mifos.individuallending.api.v1");
    Assert.assertNotNull(chargeDefinitions);
    Assert.assertTrue(chargeDefinitions.size() > 0);
  }

  @Test(expected = NotFoundException.class)
  public void shouldNotReturnDefaultChargesForNonExistentPackage() {
    portfolioManager.getAllDefaultChargeDefinitionsForPattern("io.mifos.nonexistentproduct.api.v1");
  }
}
