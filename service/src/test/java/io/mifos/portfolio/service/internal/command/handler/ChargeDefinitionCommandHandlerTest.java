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
package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.portfolio.service.internal.command.DeleteProductChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.repository.ChargeDefinitionRepository;
import io.mifos.core.lang.ServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

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

    final ChargeDefinitionCommandHandler testSubject = new ChargeDefinitionCommandHandler(null, chargeDefinitionRepositoryMock);

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