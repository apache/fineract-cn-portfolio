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

import static org.mockito.Matchers.argThat;

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.validation.Validation;
import javax.validation.Validator;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountPage;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.Creditor;
import org.apache.fineract.cn.accounting.api.v1.domain.Debtor;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.api.util.NotFoundException;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.hamcrest.Description;
import org.junit.Assert;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("Duplicates")
class AccountingFixture {
  private static final LocalDateTime universalCreationDate = LocalDateTime.of(2017, 7, 18, 15, 16, 43, 10);
  private static final String INCOME_LEDGER_IDENTIFIER = "1000";
  private static final String LOAN_INCOME_LEDGER_IDENTIFIER = "1100";
  private static final String FEES_AND_CHARGES_LEDGER_IDENTIFIER = "1300";
  private static final String ASSET_LEDGER_IDENTIFIER = "7000";
  private static final String CASH_LEDGER_IDENTIFIER = "7300";
  static final String CUSTOMER_LOAN_LEDGER_IDENTIFIER = "7353";
  private static final String ACCRUED_INCOME_LEDGER_IDENTIFIER = "7800";

  static final String LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER = "7310";
  static final String LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER = "1310";
  static final String PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER = "1312";
  static final String DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER = "1313";
  static final String CUSTOMERS_DEPOSIT_ACCOUNT = "7352";
  static final String TELLER_ONE_ACCOUNT = "7354";
  static final String LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER = "7810";
  static final String CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER = "1103";
  static final String LATE_FEE_INCOME_ACCOUNT_IDENTIFIER = "1311";
  static final String LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER = "7840";
  static final String PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER = "7353.0";
  static final String GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER = "3010";
  static final String GENERAL_EXPENSE_ACCOUNT_IDENTIFIER = "3011";

  static final String IMPORTED_CUSTOMER_LOAN_PRINCIPAL_ACCOUNT = "clp.blah.blah2";
  static final String IMPORTED_CUSTOMER_LOAN_INTEREST_ACCOUNT = "cli.blah.blah2";
  static final String IMPORTED_CUSTOMER_LOAN_FEES_ACCOUNT = "clf.blah.blah3";

  static final Map<String, AccountData> accountMap = new HashMap<>();

  private static class AccountData {
    final Account account;
    final List<AccountEntry> accountEntries = new ArrayList<>();

    AccountData(final Account account) {
      this.account = account;
    }

    void setBalance(final double balance) {
      this.account.setBalance(balance);
    }

    synchronized void addAccountEntry(final String message, final String date, final double amount) {
      final AccountEntry accountEntry = new AccountEntry();
      accountEntry.setAmount(amount);
      accountEntry.setMessage(message);
      accountEntry.setTransactionDate(date);
      accountEntries.add(accountEntry);
    }

    synchronized List<AccountEntry> copyAccountEntries() {
      return new ArrayList<>(accountEntries);
    }
  }

  private static void makeAccountResponsive(final Account account, final LocalDateTime creationDate, final LedgerManager ledgerManagerMock) {
    account.setCreatedOn(DateConverter.toIsoString(creationDate));
    final AccountData accountData = new AccountData(account);
    accountMap.put(account.getIdentifier(), accountData);
    Mockito.doAnswer(new AccountEntriesStreamAnswer(accountData))
        .when(ledgerManagerMock)
        .fetchAccountEntriesStream(Mockito.eq(account.getIdentifier()), Matchers.anyString(), Matchers.anyString(), AdditionalMatchers.or(Matchers.eq("DESC"), Matchers.eq("ASC")));
  }

  private static void makeLedgerResponsive(
      final Ledger ledger,
      final LedgerManager ledgerManagerMock)
  {
    Mockito.doReturn(ledger).when(ledgerManagerMock).findLedger(ledger.getIdentifier());
    Mockito.doReturn(emptyAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(ledger.getIdentifier()),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
  }


  private static Ledger cashLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(ASSET_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;
  }

  private static Ledger incomeLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;
  }

  private static Ledger feesAndChargesLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;
  }

  private static Ledger customerLoanLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;
  }

  private static Ledger loanIncomeLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(LOAN_INCOME_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;

  }

  private static Ledger accruedIncomeLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(ASSET_LEDGER_IDENTIFIER); //TODO: This is inaccurate for a revenue account.
    ret.setType(AccountType.REVENUE.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;

  }

  private static Account loanFundsSourceAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER);
    ret.setLedger(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account processingFeeIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account loanOriginationFeesIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account disbursementFeeIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account tellerOneAccount() {
    final Account ret = new Account();
    ret.setIdentifier(TELLER_ONE_ACCOUNT);
    ret.setLedger(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account customerDepositAccount() {
    final Account ret = new Account();
    ret.setIdentifier(CUSTOMERS_DEPOSIT_ACCOUNT);
    ret.setLedger(CASH_LEDGER_IDENTIFIER); //TODO: The ledger here is wrong.
    ret.setType(AccountType.LIABILITY.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account loanInterestAccrualAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER);
    ret.setLedger(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account consumerLoanInterestAccount() {
    final Account ret = new Account();
    ret.setIdentifier(CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER);
    ret.setLedger(LOAN_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account lateFeeIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LATE_FEE_INCOME_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account lateFeeAccrualAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER);
    ret.setLedger(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account productLossAllowanceAccount() {
    final Account ret = new Account();
    ret.setIdentifier(PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER);
    ret.setLedger(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account generalLossAllowanceAccount() {
    final Account ret = new Account();
    ret.setIdentifier(GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER);
    ret.setType(AccountType.EXPENSE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account generalExpenseAccount() {
    final Account ret = new Account();
    ret.setIdentifier(GENERAL_EXPENSE_ACCOUNT_IDENTIFIER);
    ret.setType(AccountType.EXPENSE.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account importedCustomerLoanPrincipalAccount() {
    final Account ret = new Account();
    ret.setIdentifier(IMPORTED_CUSTOMER_LOAN_PRINCIPAL_ACCOUNT);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account importedCustomerLoanInterestAccount() {
    final Account ret = new Account();
    ret.setIdentifier(IMPORTED_CUSTOMER_LOAN_INTEREST_ACCOUNT);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static Account importedCustomerLoanFeeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(IMPORTED_CUSTOMER_LOAN_FEES_ACCOUNT);
    ret.setType(AccountType.ASSET.name());
    ret.setBalance(0.0);
    return ret;
  }

  private static AccountPage customerLoanAccountsPage() {
    final Account customerLoanAccount1 = new Account();
    customerLoanAccount1.setIdentifier("customerLoanAccount1");
    customerLoanAccount1.setBalance(0.0);
    final Account customerLoanAccount2 = new Account();
    customerLoanAccount2.setIdentifier("customerLoanAccount2");
    customerLoanAccount2.setBalance(0.0);
    final Account customerLoanAccount3 = new Account();
    customerLoanAccount3.setIdentifier("customerLoanAccount3");
    customerLoanAccount3.setBalance(0.0);

    final AccountPage ret = new AccountPage();
    ret.setTotalElements(3L);
    ret.setTotalPages(1);
    ret.setAccounts(Arrays.asList(customerLoanAccount1, customerLoanAccount2, customerLoanAccount3));
    return ret;
  }

  private static AccountPage emptyAccountsPage() {
    final AccountPage ret = new AccountPage();
    ret.setTotalElements(0L);
    ret.setTotalPages(1);
    ret.setAccounts(Collections.emptyList());
    return ret;
  }

  private static <T> Valid<T> isValid() {
    return new Valid<>();
  }

  private static class Valid<T> extends ArgumentMatcher<T> {
    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      final Set errors = validator.validate(argument);

      return errors.size() == 0;
    }
  }

  private static class AccountMatcher extends ArgumentMatcher<Account> {
    private final String ledgerIdentifer;
    private final String accountDesignator;
    private final String alternativeAccountNumber;
    private final AccountType type;
    private final BigDecimal balance;
    private Account matchedArgument;

    private AccountMatcher(
        final String ledgerIdentifier,
        final String accountDesignator,
        final @Nullable String alternativeAccountNumber,
        final AccountType type,
        final BigDecimal balance) {
      this.ledgerIdentifer = ledgerIdentifier;
      this.accountDesignator = accountDesignator;
      this.alternativeAccountNumber = alternativeAccountNumber;
      this.type = type;
      this.balance = balance;
      this.matchedArgument = null; //Set when matches called and returns true.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof Account))
        return false;

      final Account checkedArgument = (Account) argument;

      final boolean ret = Objects.equals(checkedArgument.getLedger(), ledgerIdentifer) &&
          checkedArgument.getIdentifier().contains(accountDesignator) &&
          Objects.equals(checkedArgument.getAlternativeAccountNumber(), alternativeAccountNumber) &&
          Objects.equals(checkedArgument.getType(), type.name()) &&
          checkedArgument.getBalance().compareTo(balance.doubleValue()) == 0;

      if (ret)
        matchedArgument = checkedArgument;

      return ret;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(this.toString());
    }

    Account getMatchedArgument() {
      return matchedArgument;
    }

    @Override
    public String toString() {
      return "AccountMatcher{" +
          "ledgerIdentifer='" + ledgerIdentifer + '\'' +
          ", accountDesignator='" + accountDesignator + '\'' +
          ", alternativeAccountNumber='" + alternativeAccountNumber + '\'' +
          ", type=" + type +
          ", balance=" + balance +
          '}';
    }
  }

  private static class LedgerMatcher extends ArgumentMatcher<Ledger> {
    private final String ledgerIdentifer;
    private final AccountType type;
    private Ledger matchedArgument;

    LedgerMatcher(String ledgerIdentifier, AccountType type) {
      this.ledgerIdentifer = ledgerIdentifier;
      this.type = type;
      this.matchedArgument = null; //Set when matches called and returns true.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof Ledger))
        return false;

      final Ledger checkedArgument = (Ledger) argument;

      final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      final Set errors = validator.validate(checkedArgument);

      Assert.assertEquals(0, errors.size());

      final boolean ret = checkedArgument.getParentLedgerIdentifier().equals(ledgerIdentifer) &&
          checkedArgument.getType().equals(type.name());

      if (ret)
        matchedArgument = checkedArgument;

      return ret;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(this.toString());
    }

    Ledger getMatchedArgument() {
      return matchedArgument;
    }

    @Override
    public String toString() {
      return "LedgerMatcher{" +
          "ledgerIdentifer='" + ledgerIdentifer + '\'' +
          ", type=" + type +
          '}';
    }
  }

  private static class JournalEntryMatcher extends ArgumentMatcher<JournalEntry> {
    private final Set<Debtor> debtors;
    private final Set<Creditor> creditors;
    private final String transactionPrefix;
    private JournalEntry checkedArgument;

    private JournalEntryMatcher(final Set<Debtor> debtors,
                                final Set<Creditor> creditors,
                                final String productIdentifier,
                                final String caseIdentifier,
                                final Action action) {
      this.debtors = debtors;
      this.creditors = creditors;
      this.checkedArgument = null; //Set when matches called.
      this.transactionPrefix = "portfolio." + productIdentifier + "." + caseIdentifier + "." + action.name();
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof JournalEntry))
        return false;

      checkedArgument = (JournalEntry) argument;

      return this.debtors.equals(checkedArgument.getDebtors()) &&
          this.creditors.equals(checkedArgument.getCreditors()) &&
          checkedArgument.getTransactionIdentifier().startsWith(transactionPrefix);
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(this.toString());
    }

    @Override
    public String toString() {
      return "JournalEntryMatcher{" +
              "debtors=" + debtors +
              ", creditors=" + creditors +
              '}';
    }
  }

  private static class CreateJournalEntryAnswer implements Answer {
    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final JournalEntry journalEntry = invocation.getArgumentAt(0, JournalEntry.class);
      journalEntry.getCreditors().forEach(creditor ->
          accountMap.get(creditor.getAccountNumber()).addAccountEntry(
              journalEntry.getMessage(),
              journalEntry.getTransactionDate(),
              Double.valueOf(creditor.getAmount())));
      journalEntry.getDebtors().forEach(debtor ->
          accountMap.get(debtor.getAccountNumber()).addAccountEntry(
              journalEntry.getMessage(),
              journalEntry.getTransactionDate(),
              Double.valueOf(debtor.getAmount())));

      final BigDecimal creditorSum = journalEntry.getCreditors().stream()
          .map(Creditor::getAmount)
          .map(Double::valueOf)
          .map(BigDecimal::valueOf)
          .map(x -> x.setScale(4, BigDecimal.ROUND_HALF_EVEN))
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      final BigDecimal debtorSum = journalEntry.getDebtors().stream()
          .map(Debtor::getAmount)
          .map(Double::valueOf)
          .map(BigDecimal::valueOf)
          .map(x -> x.setScale(4, BigDecimal.ROUND_HALF_EVEN))
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      Assert.assertEquals(creditorSum, debtorSum);
      return null;
    }
  }

  private static class FindAccountAnswer implements Answer {
    @Override
    public Account answer(final InvocationOnMock invocation) throws Throwable {
      final String identifier = invocation.getArgumentAt(0, String.class);
      final AccountData ret = accountMap.get(identifier);
      if (ret != null)
        return ret.account;
      else
        throw new NotFoundException("Account '" + identifier + "' not found.");
    }
  }

  private static class CreateAccountAnswer implements Answer {
    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final Account account = invocation.getArgumentAt(0, Account.class);
      makeAccountResponsive(account, LocalDateTime.now(), (LedgerManager) invocation.getMock());
      return null;
    }
  }

  private static class CreateLedgerAnswer implements Answer {
    private final AccountingListener accountingListener;

    CreateLedgerAnswer(AccountingListener accountingListener) {
      this.accountingListener = accountingListener;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final Ledger ledger = invocation.getArgumentAt(1, Ledger.class);
      makeLedgerResponsive(ledger, (LedgerManager) invocation.getMock());
      accountingListener.onPostLedger(TenantContextHolder.checkedGetIdentifier(), ledger.getIdentifier());
      return null;
    }
  }

  static class AccountEntriesStreamAnswer implements Answer {
    private final AccountData accountData;

    AccountEntriesStreamAnswer(final AccountData accountData) {
      this.accountData = accountData;
    }

    @Override
    public Stream<AccountEntry> answer(final InvocationOnMock invocation) throws Throwable {
      final String message = invocation.getArgumentAt(2, String.class);
      final String direction = invocation.getArgumentAt(3, String.class);
      final boolean asc = direction == null || direction.equals("ASC");
      final List<AccountEntry> accountEntries = accountData.copyAccountEntries();
      final int entryCount = accountEntries.size();
      final Stream<AccountEntry> orderedCorrectly = asc ?
          IntStream.rangeClosed(1, entryCount).mapToObj(i -> accountEntries.get(entryCount - i)) :
          accountEntries.stream();

      if (message != null) {
        return orderedCorrectly.filter(x -> x.getMessage().equals(message));
      }
      else {
        return orderedCorrectly;
      }
    }
  }

  static void mockAccountingPrereqs(final LedgerManager ledgerManagerMock, final AccountingListener accountingListener) {
    makeAccountResponsive(loanFundsSourceAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(loanOriginationFeesIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(processingFeeIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(disbursementFeeIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(tellerOneAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(customerDepositAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(loanInterestAccrualAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(consumerLoanInterestAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(lateFeeIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(lateFeeAccrualAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(productLossAllowanceAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(generalLossAllowanceAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(generalExpenseAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(importedCustomerLoanPrincipalAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(importedCustomerLoanInterestAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(importedCustomerLoanFeeAccount(), universalCreationDate, ledgerManagerMock);

    Mockito.doReturn(incomeLedger()).when(ledgerManagerMock).findLedger(INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(feesAndChargesLedger()).when(ledgerManagerMock).findLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    Mockito.doReturn(cashLedger()).when(ledgerManagerMock).findLedger(CASH_LEDGER_IDENTIFIER);
    Mockito.doReturn(customerLoanLedger()).when(ledgerManagerMock).findLedger(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    Mockito.doReturn(loanIncomeLedger()).when(ledgerManagerMock).findLedger(LOAN_INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(accruedIncomeLedger()).when(ledgerManagerMock).findLedger(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(customerLoanAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(CUSTOMER_LOAN_LEDGER_IDENTIFIER),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    Mockito.doAnswer(new FindAccountAnswer()).when(ledgerManagerMock).findAccount(Matchers.anyString());
    Mockito.doAnswer(new CreateAccountAnswer()).when(ledgerManagerMock).createAccount(Matchers.any());
    Mockito.doAnswer(new CreateJournalEntryAnswer()).when(ledgerManagerMock).createJournalEntry(Matchers.any(JournalEntry.class));
    Mockito.doAnswer(new CreateLedgerAnswer(accountingListener)).when(ledgerManagerMock).addSubLedger(Matchers.anyString(), Matchers.any(Ledger.class));
  }

  static void mockBalance(final String accountIdentifier, final BigDecimal balance) {
    accountMap.get(accountIdentifier).setBalance(balance.doubleValue());
  }

  static String verifyAccountCreationMatchingDesignator(
      final LedgerManager ledgerManager,
      final String ledgerIdentifier,
      final String accountDesignator,
      final AccountType type) {
    return verifyAccountCreationMatchingDesignator(ledgerManager, ledgerIdentifier, accountDesignator, null, type, BigDecimal.ZERO);
  }

  static String verifyAccountCreationMatchingDesignator(
      final LedgerManager ledgerManager,
      final String ledgerIdentifier,
      final String accountDesignator,
      final @Nullable String alternativeAccountNumber,
      final AccountType type,
      final BigDecimal balance) {
    final AccountMatcher specifiesCorrectAccount = new AccountMatcher(ledgerIdentifier, accountDesignator, alternativeAccountNumber, type, balance);
    Mockito.verify(ledgerManager).createAccount(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectAccount)));
    return specifiesCorrectAccount.getMatchedArgument().getIdentifier();
  }

  static String verifyLedgerCreation(
      final LedgerManager ledgerManager,
      final String ledgerIdentifier,
      final AccountType type) {
    final LedgerMatcher specifiesCorrectLedger = new LedgerMatcher(ledgerIdentifier, type);
    Mockito.verify(ledgerManager).addSubLedger(Matchers.anyString(), AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectLedger)));
    makeLedgerResponsive(specifiesCorrectLedger.getMatchedArgument(), ledgerManager);
    return specifiesCorrectLedger.getMatchedArgument().getIdentifier();
  }

  static void verifyTransfer(final LedgerManager ledgerManager,
                             final Set<Debtor> debtors,
                             final Set<Creditor> creditors,
                             final String productIdentifier,
                             final String caseIdentifier,
                             final Action action) {
    final Set<Debtor> filteredDebtors = debtors.stream()
        .filter(x -> BigDecimal.valueOf(Double.valueOf(x.getAmount())).compareTo(BigDecimal.ZERO) != 0)
        .collect(Collectors.toSet());
    final Set<Creditor> filteredCreditors = creditors.stream()
        .filter(x -> BigDecimal.valueOf(Double.valueOf(x.getAmount())).compareTo(BigDecimal.ZERO) != 0)
        .collect(Collectors.toSet());
    if (filteredCreditors.size() == 0 && filteredDebtors.size() == 0)
      return;
    final JournalEntryMatcher specifiesCorrectJournalEntry = new JournalEntryMatcher(
        filteredDebtors,
        filteredCreditors,
        productIdentifier,
        caseIdentifier,
        action);
    Mockito.verify(ledgerManager, Mockito.atLeastOnce())
        .createJournalEntry(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectJournalEntry)));
  }
}