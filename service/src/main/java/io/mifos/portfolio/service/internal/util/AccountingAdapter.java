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
package io.mifos.portfolio.service.internal.util;

import io.mifos.accounting.api.v1.client.AccountAlreadyExistsException;
import io.mifos.accounting.api.v1.client.AccountNotFoundException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.client.LedgerNotFoundException;
import io.mifos.accounting.api.v1.domain.*;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.DateRange;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.ServiceConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.ENTRY;

/**
 * @author Myrle Krantz
 */
@Component
public class AccountingAdapter {

  public enum IdentifierType {LEDGER, ACCOUNT}

  private final LedgerManager ledgerManager;
  private final Logger logger;

  @Autowired
  public AccountingAdapter(@SuppressWarnings("SpringJavaAutowiringInspection") final LedgerManager ledgerManager,
                           @Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger) {
    this.ledgerManager = ledgerManager;
    this.logger = logger;
  }

  public void bookCharges(final List<ChargeInstance> costComponents,
                          final String note,
                          final String transactionDate,
                          final String message,
                          final String transactionType) {
    final Set<Creditor> creditors = costComponents.stream()
            .map(AccountingAdapter::mapToCreditor)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    final Set<Debtor> debtors = costComponents.stream()
            .map(AccountingAdapter::mapToDebtor)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    if (creditors.isEmpty() && !debtors.isEmpty() ||
        debtors.isEmpty() && !creditors.isEmpty())
      throw ServiceException.internalError("either only creditors or only debtors were provided.");

    //noinspection ConstantConditions
    if (creditors.isEmpty() && debtors.isEmpty())
      return;

    final JournalEntry journalEntry = new JournalEntry();
    journalEntry.setCreditors(creditors);
    journalEntry.setDebtors(debtors);
    journalEntry.setClerk(UserContextHolder.checkedGetUser());
    journalEntry.setTransactionDate(transactionDate);
    journalEntry.setMessage(message);
    journalEntry.setTransactionType(transactionType);
    journalEntry.setNote(note);
    journalEntry.setTransactionIdentifier("portfolio." + message + "." + RandomStringUtils.random(26, true, true));

    ledgerManager.createJournalEntry(journalEntry);
  }

  public Optional<LocalDateTime> getDateOfOldestEntryContainingMessage(final String accountIdentifier,
                                                                       final String message) {
    final Account account = ledgerManager.findAccount(accountIdentifier);
    final LocalDateTime accountCreatedOn = DateConverter.fromIsoString(account.getCreatedOn());
    final DateRange fromAccountCreationUntilNow = oneSidedDateRange(accountCreatedOn.toLocalDate());

    return ledgerManager.fetchAccountEntriesStream(accountIdentifier, fromAccountCreationUntilNow.toString(), message, "ASC")
        .findFirst()
        .map(AccountEntry::getTransactionDate)
        .map(DateConverter::fromIsoString);
  }

  public Optional<LocalDateTime> getDateOfMostRecentEntryContainingMessage(
      final String accountIdentifier,
      final String message) {

    final Account account = ledgerManager.findAccount(accountIdentifier);
    final LocalDateTime accountCreatedOn = DateConverter.fromIsoString(account.getCreatedOn());
    final DateRange fromAccountCreationUntilNow = oneSidedDateRange(accountCreatedOn.toLocalDate());

    return ledgerManager.fetchAccountEntriesStream(accountIdentifier, fromAccountCreationUntilNow.toString(), message, "DESC")
        .findFirst()
        .map(AccountEntry::getTransactionDate)
        .map(DateConverter::fromIsoString);
  }

  public BigDecimal sumMatchingEntriesSinceDate(final String accountIdentifier, final LocalDate startDate, final String message)
  {
    final DateRange fromLastPaymentUntilNow = oneSidedDateRange(startDate);
    final Stream<AccountEntry> accountEntriesStream = ledgerManager.fetchAccountEntriesStream(accountIdentifier, fromLastPaymentUntilNow.toString(), message, "ASC");
    return accountEntriesStream
        .map(AccountEntry::getAmount)
        .map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static Optional<Debtor> mapToDebtor(final ChargeInstance chargeInstance) {
    if (chargeInstance.getAmount().compareTo(BigDecimal.ZERO) == 0)
      return Optional.empty();

    final Debtor ret = new Debtor();
    ret.setAccountNumber(chargeInstance.getFromAccount());
    ret.setAmount(chargeInstance.getAmount().toPlainString());
    return Optional.of(ret);
  }

  private static Optional<Creditor> mapToCreditor(final ChargeInstance chargeInstance) {
    if (chargeInstance.getAmount().compareTo(BigDecimal.ZERO) == 0)
      return Optional.empty();

    final Creditor ret = new Creditor();
    ret.setAccountNumber(chargeInstance.getToAccount());
    ret.setAmount(chargeInstance.getAmount().toPlainString());
    return Optional.of(ret);
  }

  public BigDecimal getCurrentBalance(final String accountIdentifier) {
    try {
      final Account account = ledgerManager.findAccount(accountIdentifier);
      if (account == null)
        throw ServiceException.internalError("Could not find the account with identifier ''{0}''", accountIdentifier);
      return BigDecimal.valueOf(account.getBalance());
    }
    catch (final AccountNotFoundException e) {
     throw ServiceException.internalError("Could not find the account with identifier ''{0}''", accountIdentifier);
    }
  }

  public String createAccountForLedgerAssignment(final String customerIdentifier, final AccountAssignment ledgerAssignment) {
    final Ledger ledger = ledgerManager.findLedger(ledgerAssignment.getLedgerIdentifier());
    final AccountPage accountsOfLedger = ledgerManager.fetchAccountsOfLedger(ledger.getIdentifier(), null, null, null, null);

    final Account generatedAccount = new Account();
    generatedAccount.setBalance(0.0);
    generatedAccount.setType(ledger.getType());
    generatedAccount.setState(Account.State.OPEN.name());
    long guestimatedAccountIndex = accountsOfLedger.getTotalElements() + 1;
    generatedAccount.setLedger(ledger.getIdentifier());
    final Optional<String> createdAccountNumber =
        Stream.iterate(guestimatedAccountIndex, i -> i + 1).limit(99999 - guestimatedAccountIndex)
        .map(i -> {
          final String accountNumber = createAccountNumber(customerIdentifier, ledgerAssignment.getDesignator(), i);
          generatedAccount.setIdentifier(accountNumber);
          generatedAccount.setName(accountNumber);
          try {
            ledgerManager.createAccount(generatedAccount);
            return Optional.of(accountNumber);
          } catch (final AccountAlreadyExistsException e) {
            logger.error("Account '{}' could not be created because it already exists.", accountNumber);
            return Optional.<String>empty();
          }
        })
        .filter(Optional::isPresent).map(Optional::get)
        .findFirst();

    return createdAccountNumber.orElseThrow(() ->
        ServiceException.conflict("Failed to create an account for customer ''{0}'' and ''{1}'', in ledger ''{2}''.",
            customerIdentifier, ledgerAssignment.getDesignator(), ledgerAssignment.getLedgerIdentifier()));
  }

  private String createAccountNumber(final String customerIdentifier, final String designator, final long accountIndex) {
    return customerIdentifier + "." + designator
            + "." + String.format("%05d", accountIndex);
  }


  public static Set<String> accountAssignmentsRequiredButNotProvided(
          final Set<AccountAssignment> accountAssignments,
          final List<ChargeDefinition> chargeDefinitionEntities) {
    final Set<String> allAccountDesignatorsRequired = getRequiredAccountDesignators(chargeDefinitionEntities);
    final Set<String> allAccountDesignatorsDefined = accountAssignments.stream().map(AccountAssignment::getDesignator)
            .collect(Collectors.toSet());
    if (allAccountDesignatorsDefined.containsAll(allAccountDesignatorsRequired))
      return Collections.emptySet();
    else {
      allAccountDesignatorsRequired.removeAll(allAccountDesignatorsDefined);
      return allAccountDesignatorsRequired;
    }
  }

  public static Set<String> getRequiredAccountDesignators(final Collection<ChargeDefinition> chargeDefinitionEntities) {
    return chargeDefinitionEntities.stream()
            .flatMap(AccountingAdapter::getAutomaticActionAccountDesignators)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  private static Stream<String> getAutomaticActionAccountDesignators(final ChargeDefinition chargeDefinition) {
    final Stream.Builder<String> retBuilder = Stream.builder();

    checkAddDesignator(chargeDefinition.getFromAccountDesignator(), retBuilder);
    checkAddDesignator(chargeDefinition.getAccrualAccountDesignator(), retBuilder);
    checkAddDesignator(chargeDefinition.getToAccountDesignator(), retBuilder);

    return retBuilder.build();
  }

  private static void checkAddDesignator(final String accountDesignator, final Stream.Builder<String> retBuilder) {
    if (accountDesignator != null && !accountDesignator.equals(ENTRY))
      retBuilder.add(accountDesignator);
  }

  public Set<String> accountAssignmentsMappedToNonexistentAccounts(final Set<AccountAssignment> accountAssignments)
  {
    return accountAssignments.stream()
        .filter(x -> !accountAssignmentRepresentsRealAccount(x))
        .map(AccountAssignment::getDesignator)
        .collect(Collectors.toSet());
  }

  public boolean accountAssignmentRepresentsRealAccount(final AccountAssignment accountAssignment) {
    if (accountAssignment.getAccountIdentifier() != null) {
      try {
        ledgerManager.findAccount(accountAssignment.getAccountIdentifier());
        return true;
      }
      catch (final AccountNotFoundException e){
        return false;
      }
    }
    else if (accountAssignment.getLedgerIdentifier() != null) {
      try {
        ledgerManager.findLedger(accountAssignment.getLedgerIdentifier());
        return true;
      }
      catch (final LedgerNotFoundException e){
        return false;
      }
    }
    else
      return false;
  }

  private static DateRange oneSidedDateRange(final LocalDate start) {
    return new DateRange(start, LocalDate.now(ZoneId.of("UTC")));
  }
}
