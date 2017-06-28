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
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessFactor;
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessSnapshot;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.ProductParameters;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.*;
import static java.math.BigDecimal.ROUND_HALF_EVEN;

/**
 * @author Myrle Krantz
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class Fixture {
  static final String INCOME_LEDGER_IDENTIFIER = "1000";
  static final String FEES_AND_CHARGES_LEDGER_IDENTIFIER = "1300";
  static final String CASH_LEDGER_IDENTIFIER = "7300";
  static final String PENDING_DISBURSAL_LEDGER_IDENTIFIER = "7320";
  static final String CUSTOMER_LOAN_LEDGER_IDENTIFIER = "7353";
  static final String LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER = "7310";
  static final String LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER = "1310";
  static final String PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER = "1312";
  static final String TELLER_ONE_ACCOUNT_IDENTIFIER = "7352";

  private static int uniquenessSuffix = 0;

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
    accountAssignments.add(new AccountAssignment(PENDING_DISBURSAL, PENDING_DISBURSAL_LEDGER_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(PROCESSING_FEE_INCOME, PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(ORIGINATION_FEE_INCOME, LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(DISBURSEMENT_FEE_INCOME, "001-004"));
    accountAssignments.add(new AccountAssignment(INTEREST_INCOME, "001-005"));
    accountAssignments.add(new AccountAssignment(INTEREST_ACCRUAL, "001-007"));
    accountAssignments.add(new AccountAssignment(LATE_FEE_INCOME, "001-008"));
    accountAssignments.add(new AccountAssignment(LATE_FEE_ACCRUAL, "001-009"));
    accountAssignments.add(new AccountAssignment(ARREARS_ALLOWANCE, "001-010"));
    //accountAssignments.add(new AccountAssignment(ENTRY, ...));
    // Don't assign entry account in test since it usually will not be assigned IRL.
    accountAssignments.add(new AccountAssignment(LOAN_FUNDS_SOURCE, LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER));
    final AccountAssignment customerLoanAccountAssignment = new AccountAssignment();
    customerLoanAccountAssignment.setDesignator(CUSTOMER_LOAN);
    customerLoanAccountAssignment.setLedgerIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    accountAssignments.add(customerLoanAccountAssignment);
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

    final CreditWorthinessSnapshot customerCreditWorthinessSnapshot = new CreditWorthinessSnapshot();
    customerCreditWorthinessSnapshot.setForCustomer("alice");
    customerCreditWorthinessSnapshot.setDebts(Collections.singletonList(new CreditWorthinessFactor("some debt", fixScale(BigDecimal.valueOf(300)))));
    customerCreditWorthinessSnapshot.setAssets(Collections.singletonList(new CreditWorthinessFactor("some asset", fixScale(BigDecimal.valueOf(500)))));
    customerCreditWorthinessSnapshot.setIncomeSources(Collections.singletonList(new CreditWorthinessFactor("some income source", fixScale(BigDecimal.valueOf(300)))));

    final CreditWorthinessSnapshot cosignerCreditWorthinessSnapshot = new CreditWorthinessSnapshot();
    cosignerCreditWorthinessSnapshot.setForCustomer("seema");
    cosignerCreditWorthinessSnapshot.setDebts(Collections.emptyList());
    cosignerCreditWorthinessSnapshot.setAssets(Collections.singletonList(new CreditWorthinessFactor("a house", fixScale(BigDecimal.valueOf(50000)))));
    cosignerCreditWorthinessSnapshot.setIncomeSources(Collections.singletonList(new CreditWorthinessFactor("retirement", fixScale(BigDecimal.valueOf(200)))));

    final List<CreditWorthinessSnapshot> creditWorthinessSnapshots = new ArrayList<>();
    creditWorthinessSnapshots.add(customerCreditWorthinessSnapshot);
    creditWorthinessSnapshots.add(cosignerCreditWorthinessSnapshot);

    ret.setCreditWorthinessSnapshots(creditWorthinessSnapshots);

    return ret;
  }

  static public CaseParameters createAdjustedCaseParameters(final Consumer<CaseParameters> adjustment) {
    final CaseParameters ret = Fixture.getTestCaseParameters();
    adjustment.accept(ret);
    return ret;
  }
}