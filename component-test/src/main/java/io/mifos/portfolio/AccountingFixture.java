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
import org.hamcrest.Description;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static io.mifos.portfolio.Fixture.*;
import static org.mockito.Matchers.argThat;

/**
 * @author Myrle Krantz
 */
class AccountingFixture {


  private static Ledger cashLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    return ret;
  }

  private static Ledger incomeLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Ledger feesAndChargesLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(INCOME_LEDGER_IDENTIFIER);
    ret.setType(AccountType.REVENUE.name());
    return ret;
  }

  private static Ledger pendingDisbursalLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(PENDING_DISBURSAL_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
    return ret;
  }

  private static Ledger customerLoanLedger() {
    final Ledger ret = new Ledger();
    ret.setIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    ret.setParentLedgerIdentifier(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
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

  private static Account tellerOneAccount() {
    final Account ret = new Account();
    ret.setIdentifier(TELLER_ONE_ACCOUNT_IDENTIFIER);
    ret.setLedger(CASH_LEDGER_IDENTIFIER);
    ret.setType(AccountType.ASSET.name());
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
    private Account checkedArgument;

    private AccountMatcher(final String ledgerIdentifier, final AccountType type) {
      this.ledgerIdentifer = ledgerIdentifier;
      this.type = type;
      this.checkedArgument = null; //Set when matches called.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof Account))
        return false;

      checkedArgument = (Account) argument;

      return checkedArgument.getLedger().equals(ledgerIdentifer) &&
              checkedArgument.getType().equals(type.name()) &&
              checkedArgument.getBalance() == 0.0;
    }

    Account getCheckedArgument() {
      return checkedArgument;
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

      checkedArgument.getDebtors();
      checkedArgument.getCreditors();

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

  static void mockAccountingPrereqs(final LedgerManager ledgerManagerMock) {
    Mockito.doReturn(incomeLedger()).when(ledgerManagerMock).findLedger(INCOME_LEDGER_IDENTIFIER);
    Mockito.doReturn(feesAndChargesLedger()).when(ledgerManagerMock).findLedger(FEES_AND_CHARGES_LEDGER_IDENTIFIER);
    Mockito.doReturn(cashLedger()).when(ledgerManagerMock).findLedger(CASH_LEDGER_IDENTIFIER);
    Mockito.doReturn(pendingDisbursalLedger()).when(ledgerManagerMock).findLedger(PENDING_DISBURSAL_LEDGER_IDENTIFIER);
    Mockito.doReturn(customerLoanLedger()).when(ledgerManagerMock).findLedger(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    Mockito.doReturn(loanFundsSourceAccount()).when(ledgerManagerMock).findAccount(LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER);
    Mockito.doReturn(loanOriginationFeesIncomeAccount()).when(ledgerManagerMock).findAccount(LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER);
    Mockito.doReturn(processingFeeIncomeAccount()).when(ledgerManagerMock).findAccount(PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER);
    Mockito.doReturn(tellerOneAccount()).when(ledgerManagerMock).findAccount(TELLER_ONE_ACCOUNT_IDENTIFIER);
    Mockito.doReturn(customerLoanAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(CUSTOMER_LOAN_LEDGER_IDENTIFIER),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.doReturn(pendingDisbursalAccountsPage()).when(ledgerManagerMock).fetchAccountsOfLedger(Mockito.eq(PENDING_DISBURSAL_LEDGER_IDENTIFIER),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
  }

  static String verifyAccountCreation(final LedgerManager ledgerManager,
                                      final String ledgerIdentifier,
                                      final AccountType type) {
    final AccountMatcher specifiesCorrectAccount = new AccountMatcher(ledgerIdentifier, type);
    Mockito.verify(ledgerManager).createAccount(AdditionalMatchers.and(argThat(isValid()), argThat(specifiesCorrectAccount)));
    return specifiesCorrectAccount.getCheckedArgument().getIdentifier();
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