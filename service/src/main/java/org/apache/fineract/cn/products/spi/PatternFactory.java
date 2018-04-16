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
package org.apache.fineract.cn.products.spi;


import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Pattern;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public interface PatternFactory {
  Pattern pattern();
  Stream<ChargeDefinition> defaultConfigurableCharges();
  void checkParameters(String parameters);
  void persistParameters(Long caseId, String parameters);
  void changeParameters(Long caseId, String parameters);
  Optional<String> getParameters(Long caseId, int minorCurrencyUnitDigits);
  Set<String> getNextActionsForState(Case.State state);
  Payment getCostComponentsForAction(
      String productIdentifier,
      String caseIdentifier,
      String actionIdentifier,
      LocalDateTime forDateTime,
      Set<String> forAccountDesignators,
      BigDecimal forPaymentSize);
  ProductCommandDispatcher getIndividualLendingCommandDispatcher();
}
