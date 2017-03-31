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
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Component
public class AccountingAdapter {

  public enum IdentifierType {LEDGER, ACCOUNT}

  private final LedgerManager ledgerManager;

  @Autowired
  public AccountingAdapter(final LedgerManager ledgerManager) {
    this.ledgerManager = ledgerManager;
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
            .flatMap(x -> Stream.of(x.getAccrualAccountDesignator(), x.getFromAccountDesignator(), x.getToAccountDesignator()))
            .filter(x -> x != null)
            .collect(Collectors.toSet());
  }

  public boolean accountAssignmentsRepresentRealAccounts(final Set<AccountAssignment> accountAssignments)
  {
    return accountAssignments.stream().allMatch(this::accountAssignmentRepresentsRealAccount);
  }

  public boolean accountAssignmentRepresentsRealAccount(final AccountAssignment accountAssignment) {
    if (accountAssignment.getAccountIdentifier() != null) {
      try {
        final Account account = ledgerManager.findAccount(accountAssignment.getAccountIdentifier());
        return true;
      }
      catch (final AccountNotFoundException e){
        return false;
      }
    }
    else if (accountAssignment.getLedgerIdentifier() != null) {
      try {
        final Ledger ledger = ledgerManager.findLedger(accountAssignment.getLedgerIdentifier());
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
