/*
 * Copyright 2017 Kuelap, Inc.
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
package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.service.internal.repository.CaseAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class DesignatorToAccountIdentifierMapperTest {


  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  static private class TestCase {
    final String description;
    Set<ProductAccountAssignmentEntity> productAccountAssignments;
    Set<CaseAccountAssignmentEntity> caseAccountAssignments;
    List<AccountAssignment> oneTimeAccountAssignments;
    Set<AccountAssignment> expectedLedgersNeedingAccounts;
    Optional<AccountAssignment> expectedCaseAccountAssignmentMappingForCustomerLoanGroup;
    Set<DesignatorToAccountIdentifierMapper.GroupNeedingLedger> expectedGroupsNeedingLedgers;
    Optional<String> expectedMapCustomerLoanPrincipalResult = Optional.empty();

    private TestCase(String description) {
      this.description = description;
    }

    TestCase productAccountAssignments(Set<ProductAccountAssignmentEntity> newVal) {
      this.productAccountAssignments = newVal;
      return this;
    }

    TestCase caseAccountAssignments(Set<CaseAccountAssignmentEntity> newVal) {
      this.caseAccountAssignments = newVal;
      return this;
    }

    TestCase oneTimeAccountAssignments(List<AccountAssignment> newVal) {
      this.oneTimeAccountAssignments = newVal;
      return this;
    }

    TestCase expectedLedgersNeedingAccounts(Set<AccountAssignment> newVal) {
      this.expectedLedgersNeedingAccounts = newVal;
      return this;
    }

    TestCase expectedCaseAccountAssignmentMappingForCustomerLoanGroup(Optional<AccountAssignment> newVal) {
      this.expectedCaseAccountAssignmentMappingForCustomerLoanGroup = newVal;
      return this;
    }

    TestCase expectedGroupsNeedingLedgers(Set<DesignatorToAccountIdentifierMapper.GroupNeedingLedger> newVal) {
      this.expectedGroupsNeedingLedgers = newVal;
      return this;
    }

    @Override
    public String toString() {
      return "TestCase{" +
          "description='" + description + '\'' +
          '}';
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<TestCase> ret = new ArrayList<>();
    final TestCase groupedTestCase = new TestCase("basic grouped customer loan assignments")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "x")
        )))
        .caseAccountAssignments(new HashSet<>(Collections.singletonList(
            cAssignLedger(AccountDesignators.CUSTOMER_LOAN_GROUP)
        )))
        .oneTimeAccountAssignments(Collections.emptyList())
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "y"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "y"))))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.of(assignAccount(AccountDesignators.CUSTOMER_LOAN_GROUP)))
        .expectedGroupsNeedingLedgers(new HashSet<>(Collections.singletonList(
            new DesignatorToAccountIdentifierMapper.GroupNeedingLedger(AccountDesignators.CUSTOMER_LOAN_GROUP, "x"))));
    ret.add(groupedTestCase);

    final TestCase groupingIgnoredTestCase = new TestCase("customer loan assignments with ignored grouping")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "z")
        )))
        .caseAccountAssignments(Collections.emptySet())
        .oneTimeAccountAssignments(Collections.emptyList())
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "z"))))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.empty())
        .expectedGroupsNeedingLedgers(Collections.emptySet());
    ret.add(groupingIgnoredTestCase);
    return ret;
  }

  private static ProductAccountAssignmentEntity pAssignLedger(
      final String accountDesignator,
      final String ledgerIdentifier) {
    final ProductAccountAssignmentEntity ret = new ProductAccountAssignmentEntity();
    ret.setDesignator(accountDesignator);
    ret.setIdentifier(ledgerIdentifier);
    ret.setType(AccountingAdapter.IdentifierType.LEDGER);
    return ret;
  }

  private static CaseAccountAssignmentEntity cAssignLedger(
      final String accountDesignator) {
    final CaseAccountAssignmentEntity ret = new CaseAccountAssignmentEntity();
    ret.setDesignator(accountDesignator);
    ret.setIdentifier("y");
    return ret;
  }

  private static AccountAssignment assignLedger(
      final String accountDesignator,
      final String ledgerIdentifier) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setLedgerIdentifier(ledgerIdentifier);
    return ret;
  }

  private static AccountAssignment assignAccount(
      final String accountDesignator) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setAccountIdentifier("y");
    return ret;
  }

  private final TestCase testCase;
  private final DesignatorToAccountIdentifierMapper testSubject;

  public DesignatorToAccountIdentifierMapperTest(TestCase testCase) {
    this.testCase = testCase;
    this.testSubject = new DesignatorToAccountIdentifierMapper(
        testCase.productAccountAssignments,
        testCase.caseAccountAssignments,
        testCase.oneTimeAccountAssignments);
  }

  @Test
  public void map() {
    Assert.assertEquals(Optional.empty(), testSubject.map(AccountDesignators.CUSTOMER_LOAN_GROUP));
    Assert.assertEquals(testCase.expectedMapCustomerLoanPrincipalResult, testSubject.map(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL));
    Assert.assertEquals(Optional.empty(), testSubject.map("this-account-designator-doesnt-exist"));
  }

  @Test
  public void mapToCaseAccountAssignment() {
    final Optional<AccountAssignment> ret = testSubject.mapToCaseAccountAssignment(AccountDesignators.CUSTOMER_LOAN_GROUP);
    Assert.assertEquals(testCase.expectedCaseAccountAssignmentMappingForCustomerLoanGroup, ret);
  }

  @Test
  public void getLedgersNeedingAccounts() {
    final Set<AccountAssignment> ret = testSubject.getLedgersNeedingAccounts().collect(Collectors.toSet());
    Assert.assertEquals(testCase.expectedLedgersNeedingAccounts, ret);
  }

  @Test
  public void getGroupsNeedingLedgers() {
    Set<DesignatorToAccountIdentifierMapper.GroupNeedingLedger> ret = testSubject.getGroupsNeedingLedgers().collect(Collectors.toSet());
    //noinspection ResultOfMethodCallIgnored //Checking GroupNeedingLedger.toString that it doesn't cause exceptions.
    ret.toString();
    Assert.assertEquals(testCase.expectedGroupsNeedingLedgers, ret);
  }
}