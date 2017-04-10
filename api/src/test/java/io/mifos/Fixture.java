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
package io.mifos;

import com.google.gson.Gson;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.ProductParameters;
import io.mifos.portfolio.api.v1.domain.*;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.*;
import static java.math.BigDecimal.ROUND_HALF_EVEN;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class Fixture {
  static int uniquenessSuffix = 0;

  static public Product getTestProduct() {
    final Product product = new Product();
    product.setPatternPackage("io.mifos.individuallending.api.v1");
    product.setIdentifier(generateUniqueIdentifer("agro"));
    product.setName("Agricultural Loan");
    product.setDescription("Loan for seeds or agricultural equipment");
    product.setTermRange(new TermRange(ChronoUnit.MONTHS, 12));
    product.setBalanceRange(new BalanceRange(fixScale(BigDecimal.ZERO), fixScale(new BigDecimal(10000))));
    product.setInterestRange(new InterestRange(BigDecimal.valueOf(3, 2), BigDecimal.valueOf(12, 2)));
    product.setInterestBasis(InterestBasis.CURRENT_BALANCE);

    product.setCurrencyCode("XXX");
    product.setMinorCurrencyUnitDigits(2);

    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(CONSUMER_LOAN_LEDGER, "001-003"));
    accountAssignments.add(new AccountAssignment(PROCESSING_FEE_INCOME, "001-004"));
    accountAssignments.add(new AccountAssignment(ORIGINATION_FEE_INCOME, "001-004"));
    accountAssignments.add(new AccountAssignment(DISBURSEMENT_FEE_INCOME, "001-004"));
    accountAssignments.add(new AccountAssignment(INTEREST_INCOME, "001-005"));
    accountAssignments.add(new AccountAssignment(INTEREST_ACCRUAL, "001-007"));
    accountAssignments.add(new AccountAssignment(LATE_FEE_INCOME, "001-008"));
    accountAssignments.add(new AccountAssignment(LATE_FEE_ACCRUAL, "001-009"));
    accountAssignments.add(new AccountAssignment(ARREARS_ALLOWANCE, "001-010"));
    product.setAccountAssignments(accountAssignments);

    final ProductParameters productParameters = new ProductParameters();

    productParameters.setMoratoriums(Collections.emptyList());
    productParameters.setMaximumDispersalCount(5);

    final Gson gson = new Gson();
    product.setParameters(gson.toJson(productParameters));
    return product;
  }

  static public Product createAdjustedProduct(final Consumer<Product> adjustment) {
    final Product product = Fixture.getTestProduct();
    adjustment.accept(product);
    return product;
  }

  static public String generateUniqueIdentifer(final String prefix) {
    //prefix followed by a random positive number with less than 4 digits.
    return prefix + (uniquenessSuffix++);
  }

  static public BigDecimal fixScale(final BigDecimal bigDecimal)
  {
    return bigDecimal.setScale(4, ROUND_HALF_EVEN);
  }

  static public Case getTestCase(final String productIdentifier) {
    final Case ret = new Case();

    ret.setIdentifier(generateUniqueIdentifer("loan"));
    ret.setProductIdentifier(productIdentifier);


    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(CUSTOMER_LOAN, "001-011"));
    accountAssignments.add(new AccountAssignment(ENTRY, "001-012"));
    ret.setAccountAssignments(accountAssignments);
    ret.setCurrentState(Case.State.CREATED.name());

    final CaseParameters caseParameters = getTestCaseParameters();
    final Gson gson = new Gson();
    ret.setParameters(gson.toJson(caseParameters));

    return ret;
  }

  static public Case createAdjustedCase(final String productIdentifier, final Consumer<Case> adjustment) {
    final Case ret = Fixture.getTestCase(productIdentifier);
    adjustment.accept(ret);
    return ret;
  }

  static public CaseParameters getTestCaseParameters()
  {
    final CaseParameters ret = new CaseParameters(generateUniqueIdentifer("fred"));

    ret.setCustomerIdentifier("alice");
    ret.setMaximumBalance(fixScale(BigDecimal.valueOf(2000L)));
    ret.setTermRange(new TermRange(ChronoUnit.MONTHS, 18));
    ret.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 1, null, null));

    return ret;
  }

  static public CaseParameters createAdjustedCaseParameters(final Consumer<CaseParameters> adjustment) {
    final CaseParameters ret = Fixture.getTestCaseParameters();
    adjustment.accept(ret);
    return ret;
  }
}