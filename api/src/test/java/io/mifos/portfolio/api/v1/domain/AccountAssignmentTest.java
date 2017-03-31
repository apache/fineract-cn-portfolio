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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class AccountAssignmentTest extends ValidationTest<AccountAssignment> {

  public AccountAssignmentTest(ValidationTestCase<AccountAssignment> testCase) {
    super(testCase);
  }

  @Override
  protected AccountAssignment createValidTestSubject() {
    return new AccountAssignment("xxx", "yyy");
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<AccountAssignment>("basicCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<AccountAssignment>("designatorNull")
            .adjustment(x -> x.setDesignator(null))
            .valid(false));
    ret.add(new ValidationTestCase<AccountAssignment>("bothAccountAndLedgerSet")
            .adjustment(x -> x.setLedgerIdentifier("zzz"))
            .valid(false));
    ret.add(new ValidationTestCase<AccountAssignment>("bothAccountAndLedgerNull")
            .adjustment(x -> x.setAccountIdentifier(null))
            .valid(false));
    ret.add(new ValidationTestCase<AccountAssignment>("justLedgerSet")
            .adjustment(x -> { x.setLedgerIdentifier("zzz"); x.setAccountIdentifier(null); })
            .valid(true));
    return ret;
  }
}
