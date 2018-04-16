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
package org.apache.fineract.cn.individuallending.internal.service;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseAccountAssignmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
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

    @SuppressWarnings("SameParameterValue")
    TestCase expectedMapCustomerLoanPrincipalResult(final String newVal) {
      this.expectedMapCustomerLoanPrincipalResult = Optional.of(newVal);
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

    final TestCase alternativeIdsGroupedInLedgerTestCase = new TestCase("for import alternative ids are necessary.  Grouped in this case.")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "x")
        )))
        .caseAccountAssignments(new HashSet<>(Collections.singletonList(
            cAssignLedger(AccountDesignators.CUSTOMER_LOAN_GROUP)
        )))
        .oneTimeAccountAssignments(Arrays.asList(
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "alternativeCLPName"),
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_INTEREST, "alternativeCLIName"),
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_FEES, "alternativeCLFName")
        ))
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "y", "alternativeCLPName"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y", "alternativeCLIName"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "y", "alternativeCLFName"))))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.of(assignAccount(AccountDesignators.CUSTOMER_LOAN_GROUP)))
        .expectedGroupsNeedingLedgers(new HashSet<>(Collections.singletonList(
            new DesignatorToAccountIdentifierMapper.GroupNeedingLedger(AccountDesignators.CUSTOMER_LOAN_GROUP, "x"))));
    ret.add(alternativeIdsGroupedInLedgerTestCase);


    final TestCase alternativeIdsNotGroupedTestCase = new TestCase("for import alternative ids are necessary.  Not grouped in this case.")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "z")
        )))
        .caseAccountAssignments(Collections.emptySet())
        .oneTimeAccountAssignments(Arrays.asList(
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "alternativeCLPName"),
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_INTEREST, "alternativeCLIName"),
            importParameterAccountAssignment(AccountDesignators.CUSTOMER_LOAN_FEES, "alternativeCLFName")
        ))
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x", "alternativeCLPName"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y", "alternativeCLIName"),
            assignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "z", "alternativeCLFName"))))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.empty())
        .expectedGroupsNeedingLedgers(Collections.emptySet());
    ret.add(alternativeIdsNotGroupedTestCase);

    final TestCase existingAccountsTestCase = new TestCase("for import connecting to existing accounts.  Grouping shouldn't be relevant.")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y"),
            pAssignLedger(AccountDesignators.CUSTOMER_LOAN_FEES, "z")
        )))
        .caseAccountAssignments(Collections.emptySet())
        .oneTimeAccountAssignments(Arrays.asList(
            importParameterExistingAccountAssignment(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "existingCLPName"),
            importParameterExistingAccountAssignment(AccountDesignators.CUSTOMER_LOAN_INTEREST, "existingCLIName"),
            importParameterExistingAccountAssignment(AccountDesignators.CUSTOMER_LOAN_FEES, "existingCLFName")
        ))
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedgerExistingAccount(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, "x", "existingCLPName"),
            assignLedgerExistingAccount(AccountDesignators.CUSTOMER_LOAN_INTEREST, "y", "existingCLIName"),
            assignLedgerExistingAccount(AccountDesignators.CUSTOMER_LOAN_FEES, "z", "existingCLFName")
        )))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.empty())
        .expectedGroupsNeedingLedgers(Collections.emptySet())
        .expectedMapCustomerLoanPrincipalResult("existingCLPName");
    ret.add(existingAccountsTestCase);

    final TestCase alternativeIdsNotGroupedInPatternTestCase = new TestCase("for import alternative ids are necessary.  Not grouped in this case.")
        .productAccountAssignments(new HashSet<>(Arrays.asList(
            pAssignLedger("rando1", "x"),
            pAssignLedger("rando2", "y")
        )))
        .caseAccountAssignments(Collections.emptySet())
        .oneTimeAccountAssignments(Arrays.asList(
            importParameterAccountAssignment("rando1", "alternativeRando1Name"),
            importParameterAccountAssignment("rando2", "alternativeRando2Name")
        ))
        .expectedLedgersNeedingAccounts(new HashSet<>(Arrays.asList(
            assignLedger("rando1", "x", "alternativeRando1Name"),
            assignLedger("rando2", "y", "alternativeRando2Name")
        )))
        .expectedCaseAccountAssignmentMappingForCustomerLoanGroup(
            Optional.empty())
        .expectedGroupsNeedingLedgers(Collections.emptySet());
    ret.add(alternativeIdsNotGroupedInPatternTestCase);
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

  private static AccountAssignment importParameterAccountAssignment(
      final String accountDesignator,
      final String alternativeAccountNumber) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setAlternativeAccountNumber(alternativeAccountNumber);
    return ret;
  }

  private static AccountAssignment importParameterExistingAccountAssignment(
      final String accountDesignator,
      final String existingAccountIdentifier) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setAccountIdentifier(existingAccountIdentifier);
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

  private static AccountAssignment assignLedger(
      final String accountDesignator,
      final String ledgerIdentifier,
      final String alternativeAccountNumber) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setLedgerIdentifier(ledgerIdentifier);
    ret.setAlternativeAccountNumber(alternativeAccountNumber);
    return ret;
  }

  private static AccountAssignment assignLedgerExistingAccount(
      final String accountDesignator,
      final String ledgerIdentifier,
      final String existingAccountIdentifier) {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(accountDesignator);
    ret.setLedgerIdentifier(ledgerIdentifier);
    ret.setAccountIdentifier(existingAccountIdentifier);
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