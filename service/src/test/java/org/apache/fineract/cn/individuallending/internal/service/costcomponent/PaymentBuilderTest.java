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
package org.apache.fineract.cn.individuallending.internal.service.costcomponent;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PaymentBuilderTest {
  @Test
  public void expandAccountDesignators() {
    final Set<String> ret = PaymentBuilder.expandAccountDesignators(new HashSet<>(Arrays.asList(AccountDesignators.CUSTOMER_LOAN_GROUP, AccountDesignators.ENTRY)));
    final Set<String> expected = new HashSet<>(Arrays.asList(
        AccountDesignators.ENTRY,
        AccountDesignators.CUSTOMER_LOAN_GROUP,
        AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.CUSTOMER_LOAN_INTEREST));

    Assert.assertEquals(expected, ret);
  }

}