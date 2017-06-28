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
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

      final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      final Set errors = validator.validate(checkedArgument, Account.class);

      return errors.size() == 0 &&
              checkedArgument.getLedger().equals(ledgerIdentifer) &&
              checkedArgument.getType().equals(type.name()) &&
              checkedArgument.getBalance() == 0.0;
    }

    Account getCheckedArgument() {
      return checkedArgument;
    }
  }

  private static class JournalEntryMatcher extends ArgumentMatcher<JournalEntry> {
    private final String expectedFromAccountIdentifier;
    private final String expectedToAccountIdentifier;
    private final BigDecimal expectedAmount;
    private JournalEntry checkedArgument;

    private JournalEntryMatcher(final String expectedFromAccountIdentifier,
                                final String expectedToAccountIdentifier,
                                final BigDecimal amount) {
      this.expectedFromAccountIdentifier = expectedFromAccountIdentifier;
      this.expectedToAccountIdentifier = expectedToAccountIdentifier;
      this.expectedAmount = amount;
      this.checkedArgument = null; //Set when matches called.
    }

    @Override
    public boolean matches(final Object argument) {
      if (argument == null)
        return false;
      if (! (argument instanceof JournalEntry))
        return false;

      checkedArgument = (JournalEntry) argument;
      final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      final Set errors = validator.validate(checkedArgument);

      final Double debitAmount = checkedArgument.getDebtors().stream()
              .collect(Collectors.summingDouble(x -> Double.valueOf(x.getAmount())));

      final Optional<String> fromAccountIdentifier = checkedArgument.getDebtors().stream().findFirst().map(Debtor::getAccountNumber);

      final Double creditAmount = checkedArgument.getCreditors().stream()
              .collect(Collectors.summingDouble(x -> Double.valueOf(x.getAmount())));

      final Optional<String> toAccountIdentifier = checkedArgument.getCreditors().stream().findFirst().map(Creditor::getAccountNumber);

      return (errors.size() == 0 &&
              fromAccountIdentifier.isPresent() && fromAccountIdentifier.get().equals(expectedFromAccountIdentifier) &&
              toAccountIdentifier.isPresent() && toAccountIdentifier.get().equals(expectedToAccountIdentifier) &&
              creditAmount.equals(debitAmount) &&
              creditAmount.equals(expectedAmount.doubleValue()));
    }

    JournalEntry getCheckedArgument() {
      return checkedArgument;
    }

    @Override
    public String toString() {
      return "JournalEntryMatcher{" +
              "expectedFromAccountIdentifier='" + expectedFromAccountIdentifier + '\'' +
              ", expectedToAccountIdentifier='" + expectedToAccountIdentifier + '\'' +
              ", expectedAmount=" + expectedAmount +
              ", checkedArgument=" + checkedArgument +
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
  }

  static String verifyAccountCreation(final LedgerManager ledgerManager,
                                      final String ledgerIdentifier,
                                      final AccountType type) {
    final AccountMatcher specifiesCorrectAccount = new AccountMatcher(ledgerIdentifier, type);
    Mockito.verify(ledgerManager).createAccount(argThat(specifiesCorrectAccount));
    return specifiesCorrectAccount.getCheckedArgument().getIdentifier();
  }

  static void verifyTransfer(final LedgerManager ledgerManager,
                             final String fromAccountIdentifier,
                             final String toAccountIdentifier,
                             final BigDecimal amount) {
    final JournalEntryMatcher specifiesCorrectJournalEntry = new JournalEntryMatcher(fromAccountIdentifier, toAccountIdentifier, amount);
    Mockito.verify(ledgerManager).createJournalEntry(argThat(specifiesCorrectJournalEntry));
  }
}
