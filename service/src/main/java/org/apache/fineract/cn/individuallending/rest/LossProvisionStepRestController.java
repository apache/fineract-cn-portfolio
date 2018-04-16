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
package org.apache.fineract.cn.individuallending.rest;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionConfiguration;
import org.apache.fineract.cn.individuallending.internal.command.ChangeLossProvisionSteps;
import org.apache.fineract.cn.individuallending.internal.service.LossProvisionStepService;
import org.apache.fineract.cn.portfolio.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.portfolio.service.internal.service.ProductService;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/individuallending/products/{productidentifier}/lossprovisionconfiguration")
public class LossProvisionStepRestController {
  private final CommandGateway commandGateway;
  private final ProductService productService;
  private final LossProvisionStepService lossProvisionStepService;

  @Autowired
  public LossProvisionStepRestController(
      final CommandGateway commandGateway,
      final ProductService productService,
      final LossProvisionStepService lossProvisionStepService) {
    this.commandGateway = commandGateway;
    this.productService = productService;
    this.lossProvisionStepService = lossProvisionStepService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_LOSS_PROVISIONING_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.PUT,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  ResponseEntity<Void>
  changeLossProvisionConfiguration(
      @PathVariable("productidentifier") final String productIdentifier,
      @RequestBody @Valid LossProvisionConfiguration lossProvisionConfiguration) {
    checkProductExists(productIdentifier);

    commandGateway.process(new ChangeLossProvisionSteps(productIdentifier, lossProvisionConfiguration));

    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_LOSS_PROVISIONING_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  LossProvisionConfiguration
  getLossProvisionConfiguration(
      @PathVariable("productidentifier") final String productIdentifier) {
    checkProductExists(productIdentifier);

    return new LossProvisionConfiguration(lossProvisionStepService.findByProductIdentifier(productIdentifier));
  }

  private void checkProductExists(@PathVariable("productidentifier") String productIdentifier) {
    productService.findByIdentifier(productIdentifier)
        .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
  }
}
