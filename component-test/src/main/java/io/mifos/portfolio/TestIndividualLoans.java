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
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CasePage;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.individuallending.api.v1.client.IndividualLending;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
public class TestIndividualLoans extends AbstractPortfolioTest {

  @Test
  public void shouldReturnIndividualLoansCases() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Set<String> susisCaseIdentifiers = new HashSet<>();
    final Set<String> georgeCaseIdentifiers = new HashSet<>();
    for (int i = 0; i < 101; i++) {
      susisCaseIdentifiers.add(createCaseForCustomer(product.getIdentifier(), "susi").getIdentifier());
    }
    for (int i = 0; i < 82; i++) {
      georgeCaseIdentifiers.add(createCaseForCustomer(product.getIdentifier(), "george").getIdentifier());
    }

    final IndividualLending individualLending = this.individualLending;

    final Set<String> foundSusisCaseIdentifiers = new HashSet<>();
    for (int i = 0; i < 11; i++) {
      final CasePage page = individualLending.getAllCasesForCustomer("susi", i, 10);
      Assert.assertEquals(101, page.getTotalElements().longValue());
      Assert.assertEquals(11, page.getTotalPages().longValue());
      page.getElements().forEach(x -> foundSusisCaseIdentifiers.add(x.getIdentifier()));
    }
    Assert.assertEquals(susisCaseIdentifiers, foundSusisCaseIdentifiers);

    final Set<String> foundGeorgeCaseIdentifiers = new HashSet<>();
    for (int i = 0; i < 82; i++) {
      final CasePage page = individualLending.getAllCasesForCustomer("george", i, 1);
      Assert.assertEquals(82, page.getTotalElements().longValue());
      Assert.assertEquals(82, page.getTotalPages().longValue());
      page.getElements().forEach(x -> foundGeorgeCaseIdentifiers.add(x.getIdentifier()));
    }
    Assert.assertEquals(georgeCaseIdentifiers, foundGeorgeCaseIdentifiers);

    final CasePage page = individualLending.getAllCasesForCustomer("harold", 1, 1);
    Assert.assertEquals(0, page.getTotalElements().longValue());
    Assert.assertEquals(0, page.getTotalPages().longValue());
    Assert.assertEquals(0, page.getElements().size());
  }

  @Test
  public void shouldReturnSmallPaymentPlan() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case caseInstance = createCase(product.getIdentifier());

    final IndividualLending individualLending = this.individualLending;
    final PlannedPaymentPage paymentScheduleFirstPage
            = individualLending.getPaymentScheduleForCase(product.getIdentifier(), caseInstance.getIdentifier(), null, null, null);

    Assert.assertNotNull(paymentScheduleFirstPage);
    paymentScheduleFirstPage.getElements().forEach(x -> {
      x.getCostComponents().forEach(y -> Assert.assertEquals(product.getMinorCurrencyUnitDigits(), y.getAmount().scale()));
      Assert.assertEquals(product.getMinorCurrencyUnitDigits(), x.getRemainingPrincipal().scale());
    });
  }

  private Case createCaseForCustomer(final String productIdentifier, final String customerIdentifier) throws InterruptedException {
    final Case caseInstance = Fixture.getTestCase(productIdentifier);
    final CaseParameters caseParameters = Fixture.getTestCaseParameters();
    caseParameters.setCustomerIdentifier(customerIdentifier);
    final Gson gson = new Gson();
    caseInstance.setParameters(gson.toJson(caseParameters));

    portfolioManager.createCase(productIdentifier, caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
            new CaseEvent(productIdentifier, caseInstance.getIdentifier())));

    return caseInstance;}
}
