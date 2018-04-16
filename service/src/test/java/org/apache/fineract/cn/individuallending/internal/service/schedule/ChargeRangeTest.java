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
package org.apache.fineract.cn.individuallending.internal.service.schedule;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class ChargeRangeTest {
  @Test
  public void amountIsWithinRange() throws Exception {
    final ChargeRange testSubject1 = new ChargeRange(BigDecimal.TEN, Optional.empty());
    Assert.assertFalse(testSubject1.amountIsWithinRange(BigDecimal.ZERO));
    Assert.assertFalse(testSubject1.amountIsWithinRange(BigDecimal.ONE));
    Assert.assertTrue(testSubject1.amountIsWithinRange(BigDecimal.TEN));
    Assert.assertTrue(testSubject1.amountIsWithinRange(BigDecimal.TEN.add(BigDecimal.ONE)));

    final ChargeRange testSubject2 = new ChargeRange(BigDecimal.ZERO, Optional.of(BigDecimal.TEN));
    Assert.assertTrue(testSubject2.amountIsWithinRange(BigDecimal.ZERO));
    Assert.assertTrue(testSubject2.amountIsWithinRange(BigDecimal.ONE));
    Assert.assertFalse(testSubject2.amountIsWithinRange(BigDecimal.TEN));
    Assert.assertFalse(testSubject2.amountIsWithinRange(BigDecimal.TEN.add(BigDecimal.ONE)));
  }

}