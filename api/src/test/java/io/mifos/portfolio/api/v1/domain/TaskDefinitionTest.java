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

import io.mifos.Fixture;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Myrle Krantz
 */
public class TaskDefinitionTest extends ValidationTest<TaskDefinition> {

  public TaskDefinitionTest(ValidationTestCase<TaskDefinition> testCase) {
    super(testCase);
  }

  @Override
  protected TaskDefinition createValidTestSubject() {
    final TaskDefinition ret = new TaskDefinition();
    ret.setIdentifier(Fixture.generateUniqueIdentifer("task"));
    ret.setDescription("But how do you feel about this?");
    ret.setName("feep");
    ret.setMandatory(false);
    ret.setActions(new HashSet<>());
    ret.setFourEyes(true);
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<TaskDefinition>("valid")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<TaskDefinition>("nullIdentifier")
            .adjustment(x -> x.setIdentifier(null))
            .valid(false));

    return ret;
  }
}
