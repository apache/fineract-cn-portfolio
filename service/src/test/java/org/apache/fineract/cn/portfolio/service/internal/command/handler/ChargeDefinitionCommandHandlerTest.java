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
package org.apache.fineract.cn.portfolio.service.internal.command.handler;

import org.apache.fineract.cn.portfolio.service.internal.command.DeleteProductChargeDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionRepository;
import java.util.Optional;
import org.apache.fineract.cn.lang.ServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Myrle Krantz
 */
public class ChargeDefinitionCommandHandlerTest {
  @Test
  public void processDeleteHandlesCaseOfMissingChargeCorrectly() throws Exception {
    final String productIdentifier = "bibbledybobbeldy";
    final String chargeDefinitionIdentifier = "booboo";
    final ChargeDefinitionRepository chargeDefinitionRepositoryMock = Mockito.mock(ChargeDefinitionRepository.class);
    Mockito.doReturn(Optional.empty())
            .when(chargeDefinitionRepositoryMock)
            .findByProductIdAndChargeDefinitionIdentifier(productIdentifier, chargeDefinitionIdentifier);

    final BalanceSegmentRepository balanceSegmentRepository = Mockito.mock(BalanceSegmentRepository.class);

    final ChargeDefinitionCommandHandler testSubject = new ChargeDefinitionCommandHandler(null, chargeDefinitionRepositoryMock, balanceSegmentRepository);

    try {
      testSubject.process(new DeleteProductChargeDefinitionCommand(productIdentifier, chargeDefinitionIdentifier));
      Assert.assertTrue(false); //Should throw because query for element returned Optional.empty.
    }
    catch (final ServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains(productIdentifier));
      Assert.assertTrue(e.getMessage().contains(chargeDefinitionIdentifier));
    }
  }
}