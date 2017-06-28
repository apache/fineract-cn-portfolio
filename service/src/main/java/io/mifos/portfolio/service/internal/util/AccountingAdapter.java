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

import io.mifos.accounting.api.v1.client.AccountNotFoundException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.client.LedgerNotFoundException;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.Creditor;
import io.mifos.accounting.api.v1.domain.Debtor;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  @Autowired
  public AccountingAdapter(@SuppressWarnings("SpringJavaAutowiringInspection") final LedgerManager ledgerManager) {
    this.ledgerManager = ledgerManager;
  }

  public void bookCharges(final List<ChargeInstance> costComponents,
                          final String note,
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

    final JournalEntry journalEntry = new JournalEntry();
    journalEntry.setCreditors(creditors);
    journalEntry.setDebtors(debtors);
    journalEntry.setClerk(UserContextHolder.checkedGetUser());
    journalEntry.setTransactionDate(DateConverter.toIsoString(LocalDateTime.now()));
    journalEntry.setMessage(message);
    journalEntry.setTransactionType(transactionType);
    journalEntry.setNote(note);
    journalEntry.setTransactionIdentifier("bastet" + RandomStringUtils.random(26, true, true));

    ledgerManager.createJournalEntry(journalEntry);
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
    final Account account = ledgerManager.findAccount(accountIdentifier);
    return BigDecimal.valueOf(account.getBalance());
  }


  public static boolean accountAssignmentsCoverChargeDefinitions(
          final Set<AccountAssignment> accountAssignments,
          final List<ChargeDefinition> chargeDefinitionEntities) {
    final Set<String> allAccountDesignatorsRequired = getRequiredAccountDesignators(chargeDefinitionEntities);
    final Set<String> allAccountDesignatorsDefined = accountAssignments.stream().map(AccountAssignment::getDesignator)
            .collect(Collectors.toSet());
    return allAccountDesignatorsDefined.containsAll(allAccountDesignatorsRequired);

  }

  public static Set<String> getRequiredAccountDesignators(final Collection<ChargeDefinition> chargeDefinitionEntities) {
    return chargeDefinitionEntities.stream()
            .flatMap(AccountingAdapter::getAutomaticActionAccountDesignators)
            .filter(x -> x != null)
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

  public boolean accountAssignmentsRepresentRealAccounts(final Set<AccountAssignment> accountAssignments)
  {
    return accountAssignments.stream().allMatch(this::accountAssignmentRepresentsRealAccount);
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
}
