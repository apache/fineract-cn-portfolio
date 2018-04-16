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

import org.apache.fineract.cn.individuallending.IndividualLendingPatternFactory;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.RequiredAccountAssignment;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ProductMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseAccountAssignmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.fineract.cn.lang.ServiceException;

/**
 * @author Myrle Krantz
 */
public class DesignatorToAccountIdentifierMapper {
  private final @Nonnull Set<ProductAccountAssignmentEntity> productAccountAssignments;
  private final @Nonnull Set<CaseAccountAssignmentEntity> caseAccountAssignments;
  private final @Nonnull List<AccountAssignment> oneTimeAccountAssignments;

  public DesignatorToAccountIdentifierMapper(final @Nonnull DataContextOfAction dataContextOfAction) {
    this(dataContextOfAction.getProductEntity().getAccountAssignments(),
        dataContextOfAction.getCustomerCaseEntity().getAccountAssignments(),
        dataContextOfAction.getOneTimeAccountAssignments());
  }

  DesignatorToAccountIdentifierMapper(
      final @Nonnull Set<ProductAccountAssignmentEntity> productAccountAssignments,
      final @Nonnull Set<CaseAccountAssignmentEntity> caseAccountAssignments,
      final @Nonnull List<AccountAssignment> oneTimeAccountAssignments) {

    this.productAccountAssignments = productAccountAssignments;
    this.caseAccountAssignments = caseAccountAssignments;
    this.oneTimeAccountAssignments = oneTimeAccountAssignments;
  }

  private Stream<AccountAssignment> allAccountAssignmentsAsStream() {
    return Stream.concat(oneTimeAccountAssignments.stream(), fixedAccountAssignmentsAsStream());
  }

  private Stream<AccountAssignment> fixedAccountAssignmentsAsStream() {
    return Stream.concat(caseAccountAssignments.stream().map(CaseMapper::mapAccountAssignmentEntity),
        productAccountAssignmentsAsStream());
  }

  private Stream<AccountAssignment> productAccountAssignmentsAsStream() {
    return productAccountAssignments.stream().map(ProductMapper::mapAccountAssignmentEntity);
  }

  private Optional<AccountAssignment> mapToAccountAssignment(final @Nonnull String accountDesignator) {
    return allAccountAssignmentsAsStream()
        .filter(x -> x.getDesignator().equals(accountDesignator))
        .findFirst();
  }

  private Optional<AccountAssignment> mapToProductAccountAssignment(final @Nonnull String accountDesignator) {
    return productAccountAssignments.stream().map(ProductMapper::mapAccountAssignmentEntity)
        .filter(x -> x.getDesignator().equals(accountDesignator))
        .findFirst();
  }

  Optional<AccountAssignment> mapToCaseAccountAssignment(final @Nonnull String accountDesignator) {
    return caseAccountAssignments.stream().map(CaseMapper::mapAccountAssignmentEntity)
        .filter(x -> x.getDesignator().equals(accountDesignator))
        .findFirst();
  }

  public Optional<String> map(final @Nonnull String accountDesignator) {
    final Set<String> accountAssignmentGroups = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentGroups();
    if (accountAssignmentGroups.contains(accountDesignator))
      return Optional.empty();
    return mapToAccountAssignment(accountDesignator)
        .map(AccountAssignment::getAccountIdentifier);
  }

  public String mapOrThrow(final @Nonnull String accountDesignator) {
    return map(accountDesignator).orElseThrow(() ->
        ServiceException
            .badRequest("A required account designator was not set ''{0}''.", accountDesignator));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static class GroupNeedingLedger {
    final String groupName;
    final String parentLedger;

    GroupNeedingLedger(final String groupName, final String parentLedger) {
      this.groupName = groupName;
      this.parentLedger = parentLedger;
    }

    public String getGroupName() {
      return groupName;
    }

    public String getParentLedger() {
      return parentLedger;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      GroupNeedingLedger that = (GroupNeedingLedger) o;
      return Objects.equals(groupName, that.groupName) &&
          Objects.equals(parentLedger, that.parentLedger);
    }

    @Override
    public int hashCode() {
      return Objects.hash(groupName, parentLedger);
    }

    @Override
    public String toString() {
      return "GroupNeedingLedger{" +
          "groupName='" + groupName + '\'' +
          ", parentLedger='" + parentLedger + '\'' +
          '}';
    }
  }

  public Stream<GroupNeedingLedger> getGroupsNeedingLedgers() {
    //If all of the accounts in one group are assigned the same ledger, create a grouping ledger at the case level for
    // those accounts under that ledger.
    //Save that grouping ledger to an account assignment using the group name as its designator.
    //To this end, return a stream of group names requiring a ledger, and the parent ledger under which the ledger
    // should be created.

    final Set<String> accountAssignmentGroups = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentGroups();
    final Set<RequiredAccountAssignment> accountAssignmentsRequired = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentsRequired();

    return accountAssignmentGroups.stream()
        .filter(groupName -> !mapToProductAccountAssignment(groupName).isPresent()) //Only assign groups to ledgers which aren't already assigned.
        .map(groupName -> {
          final Stream<RequiredAccountAssignment> requiredAccountAssignmentsInThisGroup
              = accountAssignmentsRequired.stream().filter(x -> groupName.equals(x.getGroup()));
          final List<String> ledgersAssignedToThem = requiredAccountAssignmentsInThisGroup
              .map(requiredAccountAssignment -> mapToProductAccountAssignment(requiredAccountAssignment.getAccountDesignator()))
              .map(optionalAccountAssignment -> optionalAccountAssignment.map(AccountAssignment::getLedgerIdentifier))
              .distinct()
              .filter(Optional::isPresent)
              .map(Optional::get)
              .limit(2) //If there's more than one then we won't be creating this ledger.  We don't care about more than two.
              .collect(Collectors.toList());
          if (ledgersAssignedToThem.size() == 1) {
            //noinspection ConstantConditions
            return new GroupNeedingLedger(groupName, ledgersAssignedToThem.get(0));
          }
          else
            return null;
        })
        .filter(Objects::nonNull);
  }

  public Stream<AccountAssignment> getLedgersNeedingAccounts() {
    final Set<String> accountAssignmentGroups = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentGroups();
    final Set<RequiredAccountAssignment> accountAssignmentsRequired = IndividualLendingPatternFactory.individualLendingPattern().getAccountAssignmentsRequired();
    final Map<String, RequiredAccountAssignment> accountAssignmentsRequiredMap = accountAssignmentsRequired.stream().collect(Collectors.toMap(RequiredAccountAssignment::getAccountDesignator, x -> x));
    final Map<String, String> accountAssignmentAlternativeAccountIdsMap = oneTimeAccountAssignments.stream()
        .filter(x -> x.getAlternativeAccountNumber() != null)
        .collect(Collectors.toMap(AccountAssignment::getDesignator, AccountAssignment::getAlternativeAccountNumber));
    final Map<String, String> existingAccountsAssignmentsMap = oneTimeAccountAssignments.stream()
        .filter(x -> x.getAccountIdentifier() != null)
        .collect(Collectors.toMap(AccountAssignment::getDesignator, AccountAssignment::getAccountIdentifier));
    final Map<String, Optional<String>> groupToLedgerMapping = accountAssignmentGroups.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            group -> mapToCaseAccountAssignment(group).map(AccountAssignment::getAccountIdentifier)));

    final Stream<AccountAssignment> ledgerAccountAssignments = productAccountAssignmentsAsStream()
        .filter(x -> !x.getDesignator().equals(AccountDesignators.ENTRY))
        .filter(x -> (x.getAccountIdentifier() == null) && (x.getLedgerIdentifier() != null));

    return ledgerAccountAssignments
        .map(ledgerAccountAssignment -> {
          final AccountAssignment ret = new AccountAssignment(ledgerAccountAssignment);
          ret.setAlternativeAccountNumber(accountAssignmentAlternativeAccountIdsMap.get(ledgerAccountAssignment.getDesignator()));

          final String existingAccountSetting = existingAccountsAssignmentsMap.get(ledgerAccountAssignment.getDesignator());
          if (existingAccountSetting != null) ret.setAccountIdentifier(existingAccountSetting);

          final Optional<String> accountAssignmentGroup = Optional.ofNullable(accountAssignmentsRequiredMap.get(ledgerAccountAssignment.getDesignator())).map(RequiredAccountAssignment::getGroup);
          final Optional<String> changedLedger = accountAssignmentGroup.flatMap(groupToLedgerMapping::get);
          changedLedger.ifPresent(ret::setLedgerIdentifier);

          return ret;
        });
  }
}