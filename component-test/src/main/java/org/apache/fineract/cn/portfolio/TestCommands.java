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

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants.*;

/**
 * @author Myrle Krantz
 */
public class TestCommands extends AbstractPortfolioTest {
  // Happy case test deleted because the case is covered in more detail in
  // TestAccountingInteractionInLoanWorkflow.
  //public void testHappyWorkflow() throws InterruptedException

  @Test
  public void testApproveBeforeOpen() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkStateTransferFails(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.APPROVE,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        APPROVE_INDIVIDUALLOAN_CASE,
        Case.State.CREATED);
  }

  @Test
  public void testDisburseBeforeApproval() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        LocalDateTime.now(Clock.systemUTC()),
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        OPEN_INDIVIDUALLOAN_CASE,
        Case.State.PENDING);

    checkStateTransferFails(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.DISBURSE,
        assignEntry(AccountingFixture.CUSTOMERS_DEPOSIT_ACCOUNT),
        DISBURSE_INDIVIDUALLOAN_CASE,
        Case.State.PENDING);
  }
}