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
package org.apache.fineract.cn.portfolio.service.rest;

import org.apache.fineract.cn.portfolio.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeBalanceSegmentSetCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateBalanceSegmentSetCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.DeleteBalanceSegmentSetCommand;
import org.apache.fineract.cn.portfolio.service.internal.service.BalanceSegmentSetService;
import org.apache.fineract.cn.portfolio.service.internal.service.CaseService;
import org.apache.fineract.cn.portfolio.service.internal.service.ProductService;
import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Myrle Krantz
 */
@RestController
@RequestMapping("/products/{productidentifier}/balancesegmentsets/")
public class BalanceSegmentSetRestController {

  private final CommandGateway commandGateway;
  private final ProductService productService;
  private final BalanceSegmentSetService balanceSegmentSetService;
  private final CaseService caseService;

  public BalanceSegmentSetRestController(final CommandGateway commandGateway,
                                         final ProductService productService,
                                         final BalanceSegmentSetService balanceSegmentSetService,
                                         final CaseService caseService) {
    this.commandGateway = commandGateway;
    this.productService = productService;
    this.balanceSegmentSetService = balanceSegmentSetService;
    this.caseService = caseService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  ResponseEntity<Void> createBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @RequestBody @Valid final BalanceSegmentSet instance) {
    checkThatProductExists(productIdentifier);
    checkThatSegmentSetDoesntExist(productIdentifier, instance.getIdentifier());
    checkProductChangeable(productIdentifier);

    this.commandGateway.process(new CreateBalanceSegmentSetCommand(productIdentifier, instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }


  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  List<BalanceSegmentSet> getAllBalanceSegmentSets(
      @PathVariable("productidentifier") final String productIdentifier) {
    checkThatProductExists(productIdentifier);

    return balanceSegmentSetService.findByIdentifier(productIdentifier);
  }


  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      value = "{balancesegmentsetidentifier}",
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody BalanceSegmentSet getBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier) {
    return balanceSegmentSetService.findByIdentifier(productIdentifier, balanceSegmentSetIdentifier)
        .orElseThrow(() -> ServiceException.notFound(
            "Segment set with identifier ''{0}.{1}'' doesn''t exist.", productIdentifier, balanceSegmentSetIdentifier));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      value = "{balancesegmentsetidentifier}",
      method = RequestMethod.PUT,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<Void>  changeBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier,
      @RequestBody @Valid BalanceSegmentSet balanceSegmentSet) {
    checkThatProductAndBalanceSegmentSetExist(productIdentifier, balanceSegmentSetIdentifier);
    checkProductChangeable(productIdentifier);

    if (!balanceSegmentSetIdentifier.equals(balanceSegmentSet.getIdentifier()))
      throw ServiceException.badRequest("Instance identifier may not be changed.");

    this.commandGateway.process(new ChangeBalanceSegmentSetCommand(productIdentifier, balanceSegmentSet));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      value = "{balancesegmentsetidentifier}",
      method = RequestMethod.DELETE,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void>  deleteBalanceSegmentSet(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("balancesegmentsetidentifier") final String balanceSegmentSetIdentifier) {
    checkThatProductAndBalanceSegmentSetExist(productIdentifier, balanceSegmentSetIdentifier);
    checkProductChangeable(productIdentifier);

    this.commandGateway.process(new DeleteBalanceSegmentSetCommand(productIdentifier, balanceSegmentSetIdentifier));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  private void checkThatProductExists(final String productIdentifier) {
    if (!productService.existsByIdentifier(productIdentifier))
      throw ServiceException.notFound("Product with identifier ''{0}'' doesn''t exist.", productIdentifier);
  }

  private void checkThatSegmentSetDoesntExist(final String productIdentifier, final String segmentSetIdentifier) {
    if (balanceSegmentSetService.existsByIdentifier(productIdentifier, segmentSetIdentifier))
      throw ServiceException.conflict("Segment set with identifier ''{0}.{1}'' already exists.", productIdentifier, segmentSetIdentifier);
  }

  private void checkThatProductAndBalanceSegmentSetExist(final String productIdentifier, final String segmentSetIdentifier) {
    if (!balanceSegmentSetService.existsByIdentifier(productIdentifier, segmentSetIdentifier))
      throw ServiceException.notFound("Segment set with identifier ''{0}.{1}'' doesn''t exist.", productIdentifier, segmentSetIdentifier);
  }

  private void checkProductChangeable(final String productIdentifier) {
    if (caseService.existsByProductIdentifier(productIdentifier))
      throw ServiceException.conflict("Cases exist for product with the identifier ''{0}''. Product cannot be changed.", productIdentifier);
  }
}
