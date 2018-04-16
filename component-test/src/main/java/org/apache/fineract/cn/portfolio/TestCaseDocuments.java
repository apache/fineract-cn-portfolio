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

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.CaseEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Myrle Krantz
 */
public class TestCaseDocuments extends AbstractPortfolioTest {
  @Test
  public void test() throws InterruptedException {
    //Prepare
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());


    //Check that initial state is empty.
    final CaseCustomerDocuments caseDocuments = caseDocumentsManager.getCaseDocuments(
        product.getIdentifier(), customerCase.getIdentifier());

    Assert.assertEquals(0, caseDocuments.getDocuments().size());


    //Insert some documents.
    final CaseCustomerDocuments.Document studentLoanDocument
        = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "student_loan_papers");
    final CaseCustomerDocuments.Document houseTitle
        = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "house_title");
    final CaseCustomerDocuments.Document workContract
        = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "work_contract");

    caseDocuments.setDocuments(Arrays.asList(studentLoanDocument, houseTitle, workContract));

    caseDocumentsManager.changeCaseDocuments(product.getIdentifier(), customerCase.getIdentifier(), caseDocuments);
    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.PUT_DOCUMENT,
        new CaseEvent(product.getIdentifier(), customerCase.getIdentifier())));


    //Check that they are as set.
    {
      final CaseCustomerDocuments changedCaseDocuments = caseDocumentsManager.getCaseDocuments(product.getIdentifier(), customerCase.getIdentifier());
      Assert.assertEquals(caseDocuments, changedCaseDocuments);
    }

    //Re-order the documents
    caseDocuments.setDocuments(Arrays.asList(houseTitle, studentLoanDocument, workContract));

    caseDocumentsManager.changeCaseDocuments(product.getIdentifier(), customerCase.getIdentifier(), caseDocuments);
    Assert.assertTrue(this.eventRecorder.wait(IndividualLoanEventConstants.PUT_DOCUMENT,
        new CaseEvent(product.getIdentifier(), customerCase.getIdentifier())));

    //Check that they are as set.
    {
      Thread.sleep(1000);
      final CaseCustomerDocuments changedCaseDocuments = caseDocumentsManager.getCaseDocuments(product.getIdentifier(), customerCase.getIdentifier());
      Assert.assertEquals(caseDocuments, changedCaseDocuments);
    }

    //TODO: mock customer and check that only existing and completed documents are referenced.
  }
}