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

import org.apache.fineract.cn.individuallending.api.v1.client.CaseDocumentsManager;
import org.apache.fineract.cn.individuallending.api.v1.client.IndividualLending;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanCommandEvent;
import org.apache.fineract.cn.portfolio.api.v1.client.PortfolioManager;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Command;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.api.v1.events.CaseEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.ChargeDefinitionEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.api.v1.events.TaskDefinitionEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.TaskInstanceEvent;
import org.apache.fineract.cn.portfolio.service.config.PortfolioServiceConfiguration;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingListener;
import org.apache.fineract.cn.portfolio.service.internal.util.RhythmAdapter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.customer.api.v1.client.CustomerManager;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.test.fixture.TenantDataStoreContextTestRule;
import org.apache.fineract.cn.test.listener.EnableEventRecording;
import org.apache.fineract.cn.test.listener.EventRecorder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Myrle Krantz
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {AbstractPortfolioTest.TestConfiguration.class},
    properties = {"portfolio.bookLateFeesAndInterestAsUser=interest_user", "portfolio.bookInterestInTimeSlot=0", "portfolio.checkForLatenessInTimeSlot=0"}
)
public class AbstractPortfolioTest extends SuiteTestEnvironment {
  private static final String LOGGER_NAME = "test-logger";

  @Configuration
  @EnableEventRecording
  @EnableFeignClients(basePackages = {"org.apache.fineract.cn.portfolio.api.v1",
      "org.apache.fineract.cn.individuallending.api.v1"})
  @RibbonClient(name = APP_NAME)
  @Import({PortfolioServiceConfiguration.class})
  @ComponentScan("org.apache.fineract.cn.portfolio.listener")
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean(name = LOGGER_NAME)
    public Logger logger() {
      return LoggerFactory.getLogger(LOGGER_NAME);
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

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  CaseDocumentsManager caseDocumentsManager;

  @SuppressWarnings("unused")
  @MockBean
  RhythmAdapter rhythmAdapter;

  @MockBean
  LedgerManager ledgerManager;

  @MockBean
  CustomerManager customerManager;

  @SpyBean
  AccountingListener accountingListener;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  @Qualifier(LOGGER_NAME)
  Logger logger;

  @Before
  public void prepTest() {
    userContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TEST_USER);
    AccountingFixture.mockAccountingPrereqs(ledgerManager, accountingListener);
    Mockito.doReturn(true).when(customerManager).isCustomerInGoodStanding(Fixture.CUSTOMER_IDENTIFIER);
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

  void checkStateTransfer(
      final String productIdentifier,
      final String caseIdentifier,
      final Action action,
      final LocalDateTime actionDateTime,
      final List<AccountAssignment> oneTimeAccountAssignments,
      final String event,
      final Case.State nextState) throws InterruptedException {
    checkStateTransfer(
        productIdentifier,
        caseIdentifier,
        action,
        actionDateTime,
        oneTimeAccountAssignments,
        BigDecimal.ZERO,
        event,
        nextState);
  }

  void checkStateTransfer(
      final String productIdentifier,
      final String caseIdentifier,
      final Action action,
      final LocalDateTime actionDateTime,
      final List<AccountAssignment> oneTimeAccountAssignments,
      final BigDecimal paymentSize,
      final String event,
      final Case.State nextState) throws InterruptedException {
    final Command command = new Command();
    command.setOneTimeAccountAssignments(oneTimeAccountAssignments);
    command.setPaymentSize(paymentSize);
    command.setCreatedOn(DateConverter.toIsoString(actionDateTime));
    portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, action.name(), command);

    Assert.assertTrue(eventRecorder.wait(event, new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(actionDateTime))));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(nextState.name(), customerCase.getCurrentState());
  }

  void checkStateTransferFails(final String productIdentifier,
                                      final String caseIdentifier,
                                      final Action action,
                                      final List<AccountAssignment> oneTimeAccountAssignments,
                                      final String event,
                                      final Case.State initialState) throws InterruptedException {
    final Command command = new Command();
    command.setOneTimeAccountAssignments(oneTimeAccountAssignments);
    try {
      portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, action.name(), command);
      Assert.fail();
    }
    catch (final IllegalArgumentException ignored) {}

    Assert.assertFalse(eventRecorder.waitForMatch(event,
        (IndividualLoanCommandEvent x) -> individualLoanCommandEventMatches(x, productIdentifier, caseIdentifier)));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(customerCase.getCurrentState(), initialState.name());
  }

  private boolean individualLoanCommandEventMatches(
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

  Payment checkCostComponentForActionCorrect(
      final String productIdentifier,
      final String customerCaseIdentifier,
      final Action action,
      final Set<String> accountDesignators,
      final BigDecimal amount,
      final LocalDateTime forDateTime,
      final int minorCurrencyUnits,
      final CostComponent... expectedCostComponents) {
    final Payment payment = portfolioManager.getCostComponentsForAction(
        productIdentifier,
        customerCaseIdentifier,
        action.name(),
        accountDesignators,
        amount,
        DateConverter.toIsoString(forDateTime)
    );
    payment.getCostComponents().forEach(x -> x.setAmount(x.getAmount().setScale(minorCurrencyUnits, BigDecimal.ROUND_HALF_EVEN)));
    final Set<CostComponent> setOfCostComponents = new HashSet<>(payment.getCostComponents());
    final Set<CostComponent> setOfExpectedCostComponents = Stream.of(expectedCostComponents)
        .filter(x -> x.getAmount().compareTo(BigDecimal.ZERO) != 0)
        .collect(Collectors.toSet());
    Assert.assertEquals(setOfExpectedCostComponents, setOfCostComponents);

    return payment;
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

  List<AccountAssignment> assignEntry(final String accountIdentifier) {
    final AccountAssignment entryAccountAssignment = new AccountAssignment();
    entryAccountAssignment.setDesignator(AccountDesignators.ENTRY);
    entryAccountAssignment.setAccountIdentifier(accountIdentifier);
    return Collections.singletonList(entryAccountAssignment);
  }

  AccountAssignment assignExpenseToGeneralExpense() {
    final AccountAssignment entryAccountAssignment = new AccountAssignment();
    entryAccountAssignment.setDesignator(AccountDesignators.EXPENSE);
    entryAccountAssignment.setAccountIdentifier(AccountingFixture.GENERAL_EXPENSE_ACCOUNT_IDENTIFIER);
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
    ret.setMandatory(true);
    ret.setActions(Collections.singleton(Action.APPROVE.name()));
    ret.setFourEyes(false);
    return ret;
  }

  void enableProduct(final Product product) throws InterruptedException {
    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));
  }

  void markTaskExecuted(final Product product,
                        final Case customerCase,
                        final TaskDefinition taskDefinition) throws InterruptedException {
    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier(), true);
    Assert.assertTrue(eventRecorder.wait(EventConstants.PUT_TASK_INSTANCE_EXECUTION, new TaskInstanceEvent(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier())));
  }

  LocalDateTime midnightToday() {
    return LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
  }
}
