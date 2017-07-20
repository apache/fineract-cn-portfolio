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

import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.test.fixture.TenantDataStoreContextTestRule;
import io.mifos.core.test.listener.EnableEventRecording;
import io.mifos.core.test.listener.EventRecorder;
import io.mifos.individuallending.api.v1.client.IndividualLending;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.portfolio.api.v1.client.PortfolioManager;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskDefinitionEvent;
import io.mifos.portfolio.service.config.PortfolioServiceConfiguration;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.portfolio.service.internal.util.RhythmAdapter;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * @author Myrle Krantz
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {AbstractPortfolioTest.TestConfiguration.class},
    properties = {"portfolio.bookInterestAsUser=interest_user", "portfolio.bookInterestInTimeSlot=0"}
)
public class AbstractPortfolioTest extends SuiteTestEnvironment {
  private static final String LOGGER_NAME = "test-logger";

  @Configuration
  @EnableEventRecording
  @EnableFeignClients(basePackages = {"io.mifos.portfolio.api.v1", "io.mifos.individuallending.api.v1"})
  @RibbonClient(name = APP_NAME)
  @Import({PortfolioServiceConfiguration.class})
  @ComponentScan("io.mifos.portfolio.listener")
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean(name = LOGGER_NAME)
    public Logger logger() {
      return LoggerFactory.getLogger(LOGGER_NAME);
    }
    @Bean()
    public AccountingAdapter accountingAdapter(@SuppressWarnings("SpringJavaAutowiringInspection")
                                                   final LedgerManager ledgerManager)
    {
      final AccountingAdapter spy = Mockito.spy(new AccountingAdapter(ledgerManager));
      doReturn(true).when(spy).accountAssignmentRepresentsRealAccount(any());
      return spy;
    }
  }

  static final String TEST_USER = "setau";

  @ClassRule
  public final static TenantDataStoreContextTestRule tenantDataStoreContext
      = TenantDataStoreContextTestRule.forRandomTenantName(cassandraInitializer, mariaDBInitializer);

  @Rule
  public final TenantApplicationSecurityEnvironmentTestRule tenantApplicationSecurityEnvironment
          = new TenantApplicationSecurityEnvironmentTestRule(testEnvironment, this::waitForInitialize);

  private AutoUserContext userContext;

  @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "SpringJavaAutowiringInspection"})
  @Autowired
  protected EventRecorder eventRecorder;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  PortfolioManager portfolioManager;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  IndividualLending individualLending;

  @SuppressWarnings("unused")
  @MockBean
  RhythmAdapter rhythmAdapter;

  @MockBean
  LedgerManager ledgerManager;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  @Qualifier(LOGGER_NAME)
  Logger logger;

  @Before
  public void prepTest() {
    userContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TEST_USER);
    AccountingFixture.mockAccountingPrereqs(ledgerManager);
  }

  @After
  public void cleanTest() {
    userContext.close();
    eventRecorder.clear();
  }

  public boolean waitForInitialize() {
    try {
      return this.eventRecorder.wait(EventConstants.INITIALIZE, EventConstants.INITIALIZE);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  Product createProduct() throws InterruptedException {
    return createAdjustedProduct(x -> {});
  }

  Product createAndEnableProduct() throws InterruptedException {
    final Product product = createAdjustedProduct(x -> {});
    enableProduct(product);
    return product;
  }

  Product createAdjustedProduct(final Consumer<Product> adjustment) throws InterruptedException {
    final Product product = Fixture.createAdjustedProduct(adjustment);
    portfolioManager.createProduct(product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_PRODUCT, product.getIdentifier()));
    return product;
  }

  Case createCase(final String productIdentifier) throws InterruptedException {
    return createAdjustedCase(productIdentifier, x -> {});
  }

  Case createAdjustedCase(final String productIdentifier, final Consumer<Case> adjustment) throws InterruptedException {
    final Case caseInstance = Fixture.getTestCase(productIdentifier);
    adjustment.accept(caseInstance);

    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();
    final Set<ConstraintViolation<Case>> errors = validator.validate(caseInstance);
    Assert.assertEquals(0, errors.size());

    portfolioManager.createCase(productIdentifier, caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
            new CaseEvent(productIdentifier, caseInstance.getIdentifier())));

    return caseInstance;
  }

  void checkStateTransfer(final String productIdentifier,
                          final String caseIdentifier,
                          final Action action,
                          final List<AccountAssignment> oneTimeAccountAssignments,
                          final String event,
                          final Case.State nextState) throws InterruptedException {
    final Command command = new Command();
    command.setOneTimeAccountAssignments(oneTimeAccountAssignments);
    portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, action.name(), command);

    Assert.assertTrue(eventRecorder.wait(event, new IndividualLoanCommandEvent(productIdentifier, caseIdentifier)));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(nextState.name(), customerCase.getCurrentState());
  }

  boolean individualLoanCommandEventMatches(
          final IndividualLoanCommandEvent event,
          final String productIdentifier,
          final String caseIdentifier)
  {
    return event.getProductIdentifier().equals(productIdentifier) &&
            event.getCaseIdentifier().equals(caseIdentifier);
  }

  void checkNextActionsCorrect(final String productIdentifier, final String customerCaseIdentifier, final Action... nextActions)
  {
    final Set<String> actionList = Arrays.stream(nextActions).map(Enum::name).collect(Collectors.toSet());
    Assert.assertEquals(actionList, portfolioManager.getActionsForCase(productIdentifier, customerCaseIdentifier));
  }

  void checkCostComponentForActionCorrect(final String productIdentifier,
                                          final String customerCaseIdentifier,
                                          final Action action,
                                          final CostComponent... expectedCostComponents) {
    final List<CostComponent> costComponents = portfolioManager.getCostComponentsForAction(productIdentifier, customerCaseIdentifier, action.name());
    final Set<CostComponent> setOfCostComponents = new HashSet<>(costComponents);
    final Set<CostComponent> setOfExpectedCostComponents = new HashSet<>(Arrays.asList(expectedCostComponents));
    Assert.assertEquals(setOfExpectedCostComponents, setOfCostComponents);
  }

  void setFeeToFixedValue(final String productIdentifier,
                          final String feeId,
                          final BigDecimal amount) throws InterruptedException {
    final ChargeDefinition chargeDefinition
        = portfolioManager.getChargeDefinition(productIdentifier, feeId);
    chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    chargeDefinition.setAmount(amount);
    chargeDefinition.setProportionalTo(null);
    portfolioManager.changeChargeDefinition(productIdentifier, feeId, chargeDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
        new ChargeDefinitionEvent(productIdentifier, feeId)));
  }

  AccountAssignment assignEntryToTeller() {
    final AccountAssignment entryAccountAssignment = new AccountAssignment();
    entryAccountAssignment.setDesignator(AccountDesignators.ENTRY);
    entryAccountAssignment.setAccountIdentifier(AccountingFixture.TELLER_ONE_ACCOUNT_IDENTIFIER);
    return entryAccountAssignment;
  }

  TaskDefinition createTaskDefinition(Product product) throws InterruptedException {
    final TaskDefinition taskDefinition = getTaskDefinition();
    portfolioManager.createTaskDefinition(product.getIdentifier(), taskDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));
    return taskDefinition;
  }

  TaskDefinition getTaskDefinition() {
    final TaskDefinition ret = new TaskDefinition();
    ret.setIdentifier(Fixture.generateUniqueIdentifer("task"));
    ret.setDescription("But how do you feel about this?");
    ret.setName("feep");
    ret.setMandatory(false);
    ret.setActions(new HashSet<>());
    ret.setFourEyes(true);
    return ret;
  }

  void enableProduct(final Product product) throws InterruptedException {
    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));
  }

}
