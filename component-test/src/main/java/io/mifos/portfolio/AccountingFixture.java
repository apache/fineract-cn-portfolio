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

import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.domain.*;
import io.mifos.core.lang.DateConverter;
import org.hamcrest.Description;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.Matchers.argThat;

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
  static final String PENDING_DISBURSAL_LEDGER_IDENTIFIER = "7320";
  static final String CUSTOMER_LOAN_LEDGER_IDENTIFIER = "7353";
  private static final String ACCRUED_INCOME_LEDGER_IDENTIFIER = "7800";

  static final String LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER = "7310";
  static final String LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER = "1310";
  static final String PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER = "1312";
  static final String DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER = "1313";
  static final String TELLER_ONE_ACCOUNT_IDENTIFIER = "7352";
  static final String LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER = "7810";
  static final String CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER = "1103";
  static final String LOANS_PAYABLE_ACCOUNT_IDENTIFIER ="missingInChartOfAccounts";

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

    void addAccountEntry(final String message, final double amount) {
      final AccountEntry accountEntry = new AccountEntry();
      accountEntry.setAmount(amount);
      accountEntry.setMessage(message);
      accountEntry.setTransactionDate(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
      accountEntries.add(accountEntry);
    }
  }

  private static void makeAccountResponsive(final Account account, final LocalDateTime creationDate, final LedgerManager ledgerManagerMock) {
    account.setCreatedOn(DateConverter.toIsoString(creationDate));
    final AccountData accountData = new AccountData(account);
    accountMap.put(account.getIdentifier(), accountData);
    Mockito.doAnswer(new AccountEntriesStreamAnswer(accountData))
            .when(ledgerManagerMock)
            .fetchAccountEntriesStream(Mockito.eq(account.getIdentifier()), Matchers.anyString(), Matchers.anyString(), Matchers.eq("ASC"));
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

  private static Ledger pendingDisbursalLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(PENDING_DISBURSAL_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
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
    ret.setParentLedgerIdentifier(ASSET_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    ret.setCreatedOn(DateConverter.toIsoString(universalCreationDate));
    return ret;

  }

  private static Account loanFundsSourceAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER);
    ret.setLedger(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    return ret;
  }

  private static Account processingFeeIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Account loanOriginationFeesIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Account disbursementFeeIncomeAccount() {
    final Account ret = new Account();
    ret.setIdentifier(DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER);
    ret.setLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Account tellerOneAccount() {
    final Account ret = new Account();
    ret.setIdentifier(TELLER_ONE_ACCOUNT_IDENTIFIER);
    ret.setLedger(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    return ret;
  }

  private static Account loanInterestAccrualAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER);
    ret.setLedger(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    return ret;
  }

  private static Account consumerLoanInterestAccount() {
    final Account ret = new Account();
    ret.setIdentifier(CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER);
    ret.setLedger(LOAN_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Account loansPayableAccount() {
    final Account ret = new Account();
    ret.setIdentifier(LOANS_PAYABLE_ACCOUNT_IDENTIFIER);
    //ret.setLedger(LOAN_INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.LIABILITY.name());
    return ret;
  }

  private static AccountPage customerLoanAccountsPage() {
    final Account customerLoanAccount1 = new Account();
    customerLoanAccount1.setIdentifier("customerLoanAccount1");
    final Account customerLoanAccount2 = new Account();
    customerLoanAccount2.setIdentifier("customerLoanAccount2");
    final Account customerLoanAccount3 = new Account();
    customerLoanAccount3.setIdentifier("customerLoanAccount3");

    final AccountPage ret = new AccountPage();
    ret.setTotalElements(3L);
    ret.setTotalPages(1);
    ret.setAccounts(Arrays.asList(customerLoanAccount1, customerLoanAccount2, customerLoanAccount3));
    return ret;
  }

  private static Object pendingDisbursalAccountsPage() {
    final Account pendingDisbursalAccount1 = new Account();
    pendingDisbursalAccount1.setIdentifier("pendingDisbursalAccount1");

    final Account pendingDisbursalAccount2 = new Account();
    pendingDisbursalAccount2.setIdentifier("pendingDisbursalAccount2");

    final Account pendingDisbursalAccount3 = new Account();
    pendingDisbursalAccount3.setIdentifier("pendingDisbursalAccount3");

    final AccountPage ret = new AccountPage();
    ret.setTotalElements(3L);
    ret.setTotalPages(1);
    ret.setAccounts(Arrays.asList(pendingDisbursalAccount1, pendingDisbursalAccount2, pendingDisbursalAccount3));
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
    private final AccountType type;
    private Account matchedArgument;

    private AccountMatcher(final String ledgerIdentifier, final AccountType type) {
      this.ledgerIdentifer = ledgerIdentifier;
      this.type = type;
      this.matchedArgument = null; //Set when matches called and returns true.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof Account))
        return false;

      final Account checkedArgument = (Account) argument;

      final boolean ret = checkedArgument.getLedger().equals(ledgerIdentifer) &&
          checkedArgument.getType().equals(type.name()) &&
          checkedArgument.getBalance() == 0.0;

      if (ret)
        matchedArgument = checkedArgument;

      return ret;
    }

    Account getMatchedArgument() {
      return matchedArgument;
    }
  }

  private static class JournalEntryMatcher extends ArgumentMatcher<JournalEntry> {
    private final Set<Debtor> debtors;
    private final Set<Creditor> creditors;
    private JournalEntry checkedArgument;

    private JournalEntryMatcher(final Set<Debtor> debtors,
                                final Set<Creditor> creditors) {
      this.debtors = debtors;
      this.creditors = creditors;
      this.checkedArgument = null; //Set when matches called.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof JournalEntry))
        return false;

      checkedArgument = (JournalEntry) argument;

      return this.debtors.equals(checkedArgument.getDebtors()) &&
              this.creditors.equals(checkedArgument.getCreditors());
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
              Double.valueOf(creditor.getAmount())));
      journalEntry.getDebtors().forEach(debtor ->
          accountMap.get(debtor.getAccountNumber()).addAccountEntry(
              journalEntry.getMessage(),
              Double.valueOf(debtor.getAmount())));
      return null;
    }
  }

  private static class FindAccountAnswer implements Answer {
    @Override
    public Account answer(final InvocationOnMock invocation) throws Throwable {
      final String identifier = invocation.getArgumentAt(0, String.class);
      return accountMap.get(identifier).account;
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

  static class AccountEntriesStreamAnswer implements Answer {
    private final AccountData accountData;

    AccountEntriesStreamAnswer(final AccountData accountData) {
      this.accountData = accountData;
    }

    @Override
    public Stream<AccountEntry> answer(final InvocationOnMock invocation) throws Throwable {
      final String message = invocation.getArgumentAt(2, String.class);
      if (message != null)
        return accountData.accountEntries.stream().filter(x -> x.getMessage().equals(message));
      else
        return accountData.accountEntries.stream();
    }
  }

  static void mockAccountingPrereqs(final LedgerManager ledgerManagerMock) {
    makeAccountResponsive(loanFundsSourceAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(loanOriginationFeesIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(processingFeeIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(disbursementFeeIncomeAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(tellerOneAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(loanInterestAccrualAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(consumerLoanInterestAccount(), universalCreationDate, ledgerManagerMock);
    makeAccountResponsive(loansPayableAccount(), universalCreationDate, ledgerManagerMock);

    Mockito.doReturn(incomeLedger()).when(ledgerManagerMock).findLedger(INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(feesAndChargesLedger()).when(ledgerManagerMock).findLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    Mockito.doReturn(cashLedger()).when(ledgerManagerMock).findLedger(CASH_LEDGER_IDENTIFIER);
    Mockito.doReturn(pendingDisbursalLedger()).when(ledgerManagerMock).findLedger(PENDING_DISBURSAL_LEDGER_IDENTIFIER);
    Mockito.doReturn(customerLoanLedger()).when(ledgerManagerMock).findLedger(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    Mockito.doReturn(loanIncomeLedger()).when(ledgerManagerMock).findLedger(LOAN_INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(accruedIncomeLedger()).when(ledgerManagerMock).findLedger(ACCRUED_INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(customerLoanAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(CUSTOMER_LOAN_LEDGER_IDENTIFIER),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.doReturn(pendingDisbursalAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(PENDING_DISBURSAL_LEDGER_IDENTIFIER),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    Mockito.doAnswer(new FindAccountAnswer()).when(ledgerManagerMock).findAccount(Matchers.anyString());
    Mockito.doAnswer(new CreateAccountAnswer()).when(ledgerManagerMock).createAccount(Matchers.any());
    Mockito.doAnswer(new CreateJournalEntryAnswer()).when(ledgerManagerMock).createJournalEntry(Matchers.any(JournalEntry.class));
  }

  static void mockBalance(final String accountIdentifier, final BigDecimal balance) {
    accountMap.get(accountIdentifier).setBalance(balance.doubleValue());
  }

  static String verifyAccountCreation(final LedgerManager ledgerManager,
                                      final String ledgerIdentifier,
                                      final AccountType type) {
    final AccountMatcher specifiesCorrectAccount = new AccountMatcher(ledgerIdentifier, type);
    Mockito.verify(ledgerManager).createAccount(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectAccount)));
    return specifiesCorrectAccount.getMatchedArgument().getIdentifier();
  }

  static void verifyTransfer(final LedgerManager ledgerManager,
                             final String fromAccountIdentifier,
                             final String toAccountIdentifier,
                             final BigDecimal amount) {
    final JournalEntryMatcher specifiesCorrectJournalEntry = new JournalEntryMatcher(
            Collections.singleton(new Debtor(fromAccountIdentifier, amount.toPlainString())),
            Collections.singleton(new Creditor(toAccountIdentifier, amount.toPlainString())));
    Mockito.verify(ledgerManager).createJournalEntry(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectJournalEntry)));
  }

  static void verifyTransfer(final LedgerManager ledgerManager,
                             final Set<Debtor> debtors,
                             final Set<Creditor> creditors) {
    final JournalEntryMatcher specifiesCorrectJournalEntry = new JournalEntryMatcher(debtors, creditors);
    Mockito.verify(ledgerManager).createJournalEntry(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectJournalEntry)));

  }
}