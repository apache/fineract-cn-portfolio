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
package org.apache.fineract.cn.portfolio.api.v1.client;

import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CasePage;
import org.apache.fineract.cn.portfolio.api.v1.domain.CaseStatus;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Command;
import org.apache.fineract.cn.portfolio.api.v1.domain.ImportParameters;
import org.apache.fineract.cn.portfolio.api.v1.domain.Pattern;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.ProductPage;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskInstance;
import org.apache.fineract.cn.portfolio.api.v1.validation.ValidSortColumn;
import org.apache.fineract.cn.portfolio.api.v1.validation.ValidSortDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.apache.fineract.cn.api.annotation.ThrowsException;
import org.apache.fineract.cn.api.util.CustomFeignClientsConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@FeignClient(value = "portfolio-v1", path = "/portfolio/v1", configuration = CustomFeignClientsConfiguration.class)
public interface PortfolioManager {

  @RequestMapping(
      value = "/patterns/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  List<Pattern> getAllPatterns();

  @RequestMapping(
      value = "/products/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  ProductPage getProducts(
      @RequestParam(value = "includeDisabled", required = false) final Boolean includeDisabled,
      @RequestParam(value = "term", required = false) final String term,
      @RequestParam(value = "pageIndex") final Integer pageIndex,
      @RequestParam(value = "size") final Integer size,
      @RequestParam(value = "sortColumn", required = false) @ValidSortColumn(value = {"lastModifiedOn", "identifier", "name"}) final String sortColumn,
      @RequestParam(value = "sortDirection", required = false) @ValidSortDirection final String sortDirection);

  @RequestMapping(
      value = "/products",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductAlreadyExistsException.class)
  void createProduct(final Product product);

  @RequestMapping(
      value = "/products/{productidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Product getProduct(@PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void changeProduct(
      @PathVariable("productidentifier") final String productIdentifier,
      final Product product);

  @RequestMapping(
      value = "/products/{productidentifier}",
      method = RequestMethod.DELETE,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void deleteProduct(
      @PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/incompleteaccountassignments",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Set<AccountAssignment> getIncompleteAccountAssignments(
      @PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/enabled",
      method = RequestMethod.PUT,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductDefinitionIncomplete.class)
  void enableProduct(
      @PathVariable("productidentifier") final String productIdentifier,
      final Boolean enabled);

  @RequestMapping(
      value = "/products/{productidentifier}/enabled",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Boolean getProductEnabled(@PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/balancesegmentsets/",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void createBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      final BalanceSegmentSet balanceSegmentSet);

  @RequestMapping(
      value = "/products/{productidentifier}/balancesegmentsets/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  List<BalanceSegmentSet> getAllBalanceSegmentSets(
      @PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/balancesegmentsets/{balancesegmentsetidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  BalanceSegmentSet getBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/balancesegmentsets/{balancesegmentsetidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void changeBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier,
      BalanceSegmentSet balanceSegmentSet);

  @RequestMapping(
      value = "/products/{productidentifier}/balancesegmentsets/{balancesegmentsetidentifier}",
      method = RequestMethod.DELETE,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void deleteBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/tasks/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  List<TaskDefinition> getAllTaskDefinitionsForProduct(
      @PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/tasks/",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void createTaskDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      final TaskDefinition taskDefinition);

  @RequestMapping(
      value = "/products/{productidentifier}/tasks/{taskidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  TaskDefinition getTaskDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("taskidentifier") final String taskDefinitionIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/tasks/{taskidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void changeTaskDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("taskidentifier") final String taskDefinitionIdentifier,
      final TaskDefinition taskDefinition);

  @RequestMapping(
      value = "/products/{productidentifier}/tasks/{taskidentifier}",
      method = RequestMethod.DELETE,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ThrowsException(status = HttpStatus.CONFLICT, exception = ProductInUseException.class)
  void deleteTaskDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("taskidentifier") final String taskDefinitionIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/charges/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  List<ChargeDefinition> getAllChargeDefinitionsForProduct(
      @PathVariable("productidentifier") final String productIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/charges/",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void createChargeDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      final ChargeDefinition taskDefinition);

  @RequestMapping(
      value = "/products/{productidentifier}/charges/{chargedefinitionidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  ChargeDefinition getChargeDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/charges/{chargedefinitionidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void changeChargeDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier,
      final ChargeDefinition chargeDefinition);

  @RequestMapping(
      value = "/products/{productidentifier}/charges/{chargedefinitionidentifier}",
      method = RequestMethod.DELETE,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void deleteChargeDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  CasePage getAllCasesForProduct(
      @PathVariable("productidentifier") final String productIdentifier,
      @RequestParam(value = "includeClosed", required = false) final Boolean includeClosed,
      @RequestParam("pageIndex") final Integer pageIndex,
      @RequestParam("size") final Integer size);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = CaseAlreadyExistsException.class)
  void createCase(
      @PathVariable("productidentifier") final String productIdentifier,
      final Case caseInstance);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Case getCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/status",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  CaseStatus getCaseStatus(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void changeCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      final Case caseInstance);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/actions/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Set<String> getActionsForCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier);


  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/actions/{actionidentifier}/costcomponents",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Payment getCostComponentsForAction(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      @RequestParam(value="touchingaccounts", required = false, defaultValue = "") final Set<String> forAccountDesignators,
      @RequestParam(value="forpaymentsize", required = false, defaultValue = "") final BigDecimal forPaymentSize);



  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/actions/{actionidentifier}/costcomponents",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Payment getCostComponentsForAction(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      @RequestParam(value="touchingaccounts", required = false, defaultValue = "") final Set<String> forAccountDesignators,
      @RequestParam(value="forpaymentsize", required = false, defaultValue = "") final BigDecimal forPaymentSize,
      @RequestParam(value="fordatetime", required = false, defaultValue = "") final String forDateTime);


  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/actions/{actionidentifier}/costcomponents",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Payment getCostComponentsForAction(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      @RequestParam(value="touchingaccounts", required = false, defaultValue = "") final Set<String> forAccountDesignators);


  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/actions/{actionidentifier}/costcomponents",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Payment getCostComponentsForAction(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/commands/{actionidentifier}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = TaskOutstanding.class)
  void executeCaseCommand(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      final Command command);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/commands/IMPORT",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = TaskOutstanding.class)
  void executeImportCommand(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      final ImportParameters command);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/tasks/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  List<TaskInstance> getAllTasksForCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @RequestParam(value = "includeExecuted", required = false) final Boolean includeExecuted);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/tasks/{taskidentifier}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  TaskInstance getTaskForCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("taskidentifier") final String taskIdentifier);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/tasks/{taskidentifier}",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void changeTaskForCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("taskidentifier") final String taskIdentifier,
      final TaskInstance instance);

  @RequestMapping(
      value = "/products/{productidentifier}/cases/{caseidentifier}/tasks/{taskidentifier}/executed",
      method = RequestMethod.PUT,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = TaskExecutionBySameUserAsCaseCreation.class)
  void markTaskExecution(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("taskidentifier") final String taskIdentifier,
      final Boolean executed);

  @RequestMapping(
      value = "/cases/",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  CasePage getAllCases(
      @RequestParam("pageIndex") final Integer pageIndex,
      @RequestParam("size") final Integer size);
}
