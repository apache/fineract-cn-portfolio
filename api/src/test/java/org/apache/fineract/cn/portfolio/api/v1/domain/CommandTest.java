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
package org.apache.fineract.cn.portfolio.api.v1.domain;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.PROCESSING_FEE_ID;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CommandTest extends ValidationTest<Command> {
  public CommandTest(ValidationTestCase<Command> testCase) {
    super(testCase);
  }

  @Override
  protected Command createValidTestSubject() {
    final Command ret = new Command();
    ret.setOneTimeAccountAssignments(Collections.emptyList());
    ret.setCreatedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<Command>("valid"));
    ret.add(new ValidationTestCase<Command>("invalidAccountAssignment")
            .adjustment(x -> x.setOneTimeAccountAssignments(Collections.singletonList(new AccountAssignment("", ""))))
            .valid(false));
    ret.add(new ValidationTestCase<Command>("validAccountAssignment")
            .adjustment(x -> x.setOneTimeAccountAssignments(Collections.singletonList(new AccountAssignment(PROCESSING_FEE_ID, "7534"))))
            .valid(true));
    ret.add(new ValidationTestCase<Command>("nullAccountAssignment")
            .adjustment(x -> x.setOneTimeAccountAssignments(null))
            .valid(true));
    return ret;
  }
}
