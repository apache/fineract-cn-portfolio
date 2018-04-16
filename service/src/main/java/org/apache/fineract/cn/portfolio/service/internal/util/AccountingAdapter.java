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
package org.apache.fineract.cn.portfolio.service.internal.util;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators.ENTRY;

import com.google.common.collect.Sets;
import org.apache.fineract.cn.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.ServiceConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.fineract.cn.accounting.api.v1.client.AccountAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.AccountNotFoundException;
import org.apache.fineract.cn.accounting.api.v1.client.JournalEntryAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerNotFoundException;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountPage;
import org.apache.fineract.cn.accounting.api.v1.domain.Creditor;
import org.apache.fineract.cn.accounting.api.v1.domain.Debtor;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.DateRange;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.lang.listening.EventExpectation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
public class AccountingAdapter {


  public enum IdentifierType {LEDGER, ACCOUNT}

  private final LedgerManager ledgerManager;
  private final AccountingListener accountingListener;
  private final Logger logger;

  @Autowired
  public AccountingAdapter(@SuppressWarnings("SpringJavaAutowiringInspection") final LedgerManager ledgerManager,
                           final AccountingListener accountingListener,
                           @Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger) {
    this.ledgerManager = ledgerManager;
    this.accountingListener = accountingListener;
    this.logger = logger;
  }

  private static class BalanceAdjustment {
    final private String accountIdentifier; //*Not* designator.
    final private BigDecimal adjustment;

    BalanceAdjustment(String accountIdentifier, BigDecimal adjustment) {
      this.accountIdentifier = accountIdentifier;
      this.adjustment = adjustment;
    }

    String getAccountIdentifier() {
      return accountIdentifier;
    }

    BigDecimal getAdjustment() {
      return adjustment;
    }
  }

  public Optional<String> bookCharges(
      final Map<String, BigDecimal> balanceAdjustments,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper,
      final String note,
      final String transactionDate,
      final String message,
      final String transactionType) {
    final JournalEntry journalEntry = getJournalEntry(
        balanceAdjustments,
        designatorToAccountIdentifierMapper,
        note,
        transactionDate,
        message,
        transactionType,
        UserContextHolder.checkedGetUser());

    //noinspection ConstantConditions
    if (journalEntry.getCreditors().isEmpty() && journalEntry.getDebtors().isEmpty())
      return Optional.empty();

    while (true) {
      try {
        final String transactionUniqueifier = RandomStringUtils.random(26, true, true);
        journalEntry.setTransactionIdentifier(formulateTransactionIdentifier(message, transactionUniqueifier));
        ledgerManager.createJournalEntry(journalEntry);
        return Optional.of(transactionUniqueifier);
      } catch (final JournalEntryAlreadyExistsException ignore) {
        //Try again with a new uniqueifier.
      }
    }
  }

  private static String formulateTransactionIdentifier(
      final String message,
      final String transactionUniqueifier) {
    return "portfolio." + message + "." + transactionUniqueifier;
  }

  static JournalEntry getJournalEntry(
      final Map<String, BigDecimal> balanceAdjustments,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper,
      final String note,
      final String transactionDate,
      final String message,
      final String transactionType,
      final String user) {
    final JournalEntry journalEntry = new JournalEntry();
    final Set<Creditor> creditors = new HashSet<>();
    journalEntry.setCreditors(creditors);
    final Set<Debtor> debtors = new HashSet<>();
    journalEntry.setDebtors(debtors);
    final Map<String, BigDecimal> summedBalanceAdjustments = balanceAdjustments.entrySet().stream()
        .map(entry -> {
          final String accountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(entry.getKey());
          return new BalanceAdjustment(accountIdentifier, entry.getValue());
        })
        .collect(Collectors.groupingBy(BalanceAdjustment::getAccountIdentifier,
            Collectors.mapping(BalanceAdjustment::getAdjustment,
                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

    summedBalanceAdjustments.forEach((accountIdentifier, balanceAdjustment) -> {
      final int sign = balanceAdjustment.compareTo(BigDecimal.ZERO);
      if (sign == 0)
        return;

      if (sign < 0) {
        final Debtor debtor = new Debtor();
        debtor.setAccountNumber(accountIdentifier);
        debtor.setAmount(balanceAdjustment.negate().toPlainString());
        debtors.add(debtor);
      } else {
        final Creditor creditor = new Creditor();
        creditor.setAccountNumber(accountIdentifier);
        creditor.setAmount(balanceAdjustment.toPlainString());
        creditors.add(creditor);
      }
    });

    if (creditors.isEmpty() && !debtors.isEmpty() ||
        debtors.isEmpty() && !creditors.isEmpty())
      throw ServiceException.internalError("either only creditors or only debtors were provided.");


    journalEntry.setCreditors(creditors);
    journalEntry.setDebtors(debtors);
    journalEntry.setClerk(user);
    journalEntry.setTransactionDate(transactionDate);
    journalEntry.setMessage(message);
    journalEntry.setTransactionType(transactionType);
    journalEntry.setNote(note);
    return journalEntry;
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

  public BigDecimal sumMatchingEntriesSinceDate(final String accountIdentifier, final LocalDate startDate, final String message)
  {
    final DateRange fromLastPaymentUntilNow = oneSidedDateRange(startDate);
    final Stream<AccountEntry> accountEntriesStream = ledgerManager.fetchAccountEntriesStream(accountIdentifier, fromLastPaymentUntilNow.toString(), message, "ASC");
    return accountEntriesStream
        .map(AccountEntry::getAmount)
        .map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Account getAccount(final String accountIdentifier) {
    try {
      final Account account = ledgerManager.findAccount(accountIdentifier);
      if (account == null || account.getBalance() == null || account.getType() == null)
        throw ServiceException.internalError("Could not find the account with identifier ''{0}''", accountIdentifier);
      return account;
    }
    catch (final AccountNotFoundException e) {
     throw ServiceException.internalError("Could not find the account with identifier ''{0}''", accountIdentifier);
    }
  }

  public String createLedger(
      final String customerIdentifier,
      final String groupName,
      final String parentLedger) throws InterruptedException {
    final Ledger ledger = ledgerManager.findLedger(parentLedger);
    final List<Ledger> subLedgers = ledger.getSubLedgers() == null ? Collections.emptyList() : ledger.getSubLedgers();

    final Ledger generatedLedger = new Ledger();
    generatedLedger.setShowAccountsInChart(true);
    generatedLedger.setParentLedgerIdentifier(parentLedger);
    generatedLedger.setType(ledger.getType());
    final IdentiferWithIndex ledgerIdentifer = createLedgerIdentifier(customerIdentifier, groupName, subLedgers);
    generatedLedger.setIdentifier(ledgerIdentifer.getIdentifier());
    generatedLedger.setDescription("Individual loan case specific ledger");
    generatedLedger.setName(ledgerIdentifer.getIdentifier());


    final EventExpectation expectation = accountingListener.expectLedgerCreation(generatedLedger.getIdentifier());
    boolean created = false;
    while (!created) {
      try {
        logger.info("Attempting to create ledger with identifier '{}'", ledgerIdentifer.getIdentifier());
        ledgerManager.addSubLedger(parentLedger, generatedLedger);
        created = true;
      } catch (final LedgerAlreadyExistsException e) {
        ledgerIdentifer.incrementIndex();
        generatedLedger.setIdentifier(ledgerIdentifer.getIdentifier());
        generatedLedger.setName(ledgerIdentifer.getIdentifier());
      }
    }
    final boolean ledgerCreationDetected = expectation.waitForOccurrence(10, TimeUnit.SECONDS);
    if (!ledgerCreationDetected)
      logger.warn("Waited 5 seconds for creation of ledger '{}', but it was not detected. This could cause subsequent " +
              "account creations to fail. Is there something wrong with the accounting service? Is ActiveMQ setup properly?",
          generatedLedger.getIdentifier());
    return ledgerIdentifer.getIdentifier();
  }

  public String createProductAccountForLedgerAssignment(
      final String productIdentifier,
      final String accountDesignator,
      final String ledgerIdentifier) {
    final Ledger ledger = ledgerManager.findLedger(ledgerIdentifier);

    final Account generatedAccount = new Account();
    generatedAccount.setBalance(0.0);
    generatedAccount.setType(ledger.getType());
    generatedAccount.setState(Account.State.OPEN.name());
    generatedAccount.setLedger(ledger.getIdentifier());
    final Optional<String> createdAccountNumber =
        Stream.iterate(0, i -> i + 1).limit(99999)
            .map(i -> {
              final String accountNumber = createProductAccountNumber(productIdentifier, accountDesignator, i);
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
        ServiceException.conflict("Failed to create an account for product ''{0}'' and designator ''{1}'', in ledger ''{2}''.",
            productIdentifier, accountDesignator, ledgerIdentifier));
  }

  public String createOrFindCaseAccountForLedgerAssignment(
      final String customerIdentifier,
      final AccountAssignment ledgerAssignment,
      final BigDecimal currentBalance) {
    if (ledgerAssignment.getAccountIdentifier() != null) try
    {
      final Account existingAccount = ledgerManager.findAccount(ledgerAssignment.getAccountIdentifier());
      return existingAccount.getIdentifier();
    }
    catch (final AccountNotFoundException ignored) {
      //If the "existing" account doesn't exist after all, create a new one.
    }
    final Ledger ledger = ledgerManager.findLedger(ledgerAssignment.getLedgerIdentifier());
    final AccountPage accountsOfLedger = ledgerManager.fetchAccountsOfLedger(ledger.getIdentifier(), null, null, null, null);

    final Account generatedAccount = new Account();
    generatedAccount.setBalance(currentBalance.doubleValue());
    generatedAccount.setType(ledger.getType());
    generatedAccount.setState(Account.State.OPEN.name());
    generatedAccount.setHolders(Sets.newHashSet(customerIdentifier));
    long guestimatedAccountIndex = accountsOfLedger.getTotalElements() + 1;
    generatedAccount.setLedger(ledger.getIdentifier());
    final Optional<String> createdAccountNumber =
        Stream.iterate(guestimatedAccountIndex, i -> i + 1).limit(99999 - guestimatedAccountIndex)
        .map(i -> {
          final String accountNumber = createCaseAccountNumber(customerIdentifier, ledgerAssignment.getDesignator(), i);
          generatedAccount.setIdentifier(accountNumber);
          generatedAccount.setName(accountNumber);
          generatedAccount.setAlternativeAccountNumber(ledgerAssignment.getAlternativeAccountNumber());
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

  private static class IdentiferWithIndex {
    private long index;
    private final String prefix;

    IdentiferWithIndex(long index, String prefix) {
      this.index = index;
      this.prefix = prefix;
    }

    String getIdentifier() {
      return prefix + String.format("%05d", index);
    }

    void incrementIndex() {
      index++;
    }
  }

  private IdentiferWithIndex createLedgerIdentifier(
      final String customerIdentifier,
      final String groupName,
      final List<Ledger> subLedgers) {
    final String partialCustomerIdentifer = StringUtils.left(customerIdentifier, 22);
    final String partialGroupName = StringUtils.left(groupName, 3);
    final Set<String> subLedgerIdentifiers = subLedgers.stream().map(Ledger::getIdentifier).collect(Collectors.toSet());
    final String generatedIdentifierPrefix = partialCustomerIdentifer + "." + partialGroupName + ".";
    final IdentiferWithIndex ret = new IdentiferWithIndex(0, generatedIdentifierPrefix);
    while (true) {
      ret.incrementIndex();
      if (!subLedgerIdentifiers.contains(ret.getIdentifier()))
        return ret;
    }
  }

  private String createProductAccountNumber(final String productIdentifier, final String designator, final long accountIndex) {
    return StringUtils.left(productIdentifier, 22) + "." + StringUtils.left(designator, 3)
        + "." + String.format("%05d", accountIndex);
  }

  private String createCaseAccountNumber(final String customerIdentifier, final String designator, final long accountIndex) {
    return StringUtils.left(customerIdentifier, 22) + "." + StringUtils.left(designator, 3)
            + "." + String.format("%05d", accountIndex);
  }


  public static Set<String> accountAssignmentsRequiredButNotProvided(
          final Set<AccountAssignment> accountAssignments,
          final Stream<ChargeDefinition> chargeDefinitionEntities) {
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

  public static Set<String> getRequiredAccountDesignators(final Stream<ChargeDefinition> chargeDefinitionEntities) {
    return chargeDefinitionEntities
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
