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

import com.google.common.collect.Sets;
import org.apache.fineract.cn.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.cn.accounting.api.v1.client.JournalEntryAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.accounting.api.v1.domain.Creditor;
import org.apache.fineract.cn.accounting.api.v1.domain.Debtor;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

/**
 * @author Myrle Krantz
 */
public class AccountingAdapterTest {
  @Test
  public void getJournalEntryWithMultipleIdenticalChargesMappedToSameAccount() {
    final BigDecimal two = BigDecimal.valueOf(2);
    final BigDecimal negativeTwo = two.negate();
    final Map<String, BigDecimal> balanceAdjustments = new HashMap<>();
    balanceAdjustments.put("a", BigDecimal.ONE);
    balanceAdjustments.put("b", BigDecimal.ONE);
    balanceAdjustments.put("c", negativeTwo);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper = Mockito.mock(DesignatorToAccountIdentifierMapper.class);
    Mockito.doReturn("a1").when(designatorToAccountIdentifierMapper).mapOrThrow("a");
    Mockito.doReturn("a1").when(designatorToAccountIdentifierMapper).mapOrThrow("b");
    Mockito.doReturn("c1").when(designatorToAccountIdentifierMapper).mapOrThrow("c");

    final JournalEntry journalEntry = AccountingAdapter.getJournalEntry(
        balanceAdjustments,
        designatorToAccountIdentifierMapper,
        "", "", "", "",  "");
    Assert.assertEquals(Sets.newHashSet(new Debtor("c1", two.toPlainString())), journalEntry.getDebtors());
    Assert.assertEquals(Sets.newHashSet(new Creditor("a1", two.toPlainString())), journalEntry.getCreditors());
  }

  @Test
  public void journalEntryCreationFailsBecauseIdentifierAlreadyExistsShouldCauseRetry() {
    final LedgerManager ledgerManagerMock = Mockito.mock(LedgerManager.class);
    final AccountingAdapter testSubject = new AccountingAdapter(ledgerManagerMock, null, null);


    final Map<String, BigDecimal> balanceAdjustments = new HashMap<>();
    balanceAdjustments.put("a", BigDecimal.ONE);
    balanceAdjustments.put("b", BigDecimal.ONE.negate());

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper = Mockito.mock(DesignatorToAccountIdentifierMapper.class);
    Mockito.doReturn("a1").when(designatorToAccountIdentifierMapper).mapOrThrow("a");
    Mockito.doReturn("b1").when(designatorToAccountIdentifierMapper).mapOrThrow("b");

    Mockito.doThrow(JournalEntryAlreadyExistsException.class)
        .doThrow(JournalEntryAlreadyExistsException.class)
        .doNothing()
        .when(ledgerManagerMock).createJournalEntry(Matchers.anyObject());

    UserContextHolder.setAccessToken("blah", "blah");

    testSubject.bookCharges(
        balanceAdjustments,
        designatorToAccountIdentifierMapper,
        "", "", "x", "");

    Mockito.verify(ledgerManagerMock, Mockito.atLeast(3)).createJournalEntry(Matchers.anyObject());
  }

}