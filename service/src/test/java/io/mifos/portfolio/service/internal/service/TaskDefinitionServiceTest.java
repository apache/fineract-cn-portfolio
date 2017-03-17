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
package io.mifos.portfolio.service.internal.service;

import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.service.internal.mapper.TaskDefinitionMapper;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.TaskDefinitionEntity;
import io.mifos.portfolio.service.internal.repository.TaskDefinitionRepository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class TaskDefinitionServiceTest {
  private static class TestHarness
  {
    final TaskDefinitionRepository taskDefinitionRepository;
    final TaskDefinitionService testSubject;

    TestHarness()
    {
      taskDefinitionRepository = Mockito.mock(TaskDefinitionRepository.class);
      testSubject = new TaskDefinitionService(
              taskDefinitionRepository);
    }

    TaskDefinitionRepository getTaskDefinitionRepository() {
      return taskDefinitionRepository;
    }

    TaskDefinitionService getTestSubject() {
      return testSubject;
    }
  }

  @Test
  public void findAllEntities()
  {
    findAllEntitiesHelper(false);
    findAllEntitiesHelper(true);
  }

  private void findAllEntitiesHelper(boolean findTaskDefinition) {
    final TestHarness testHarness = new TestHarness();

    final List<TaskDefinitionEntity> taskDefinitionReturn = findTaskDefinition
            ? Collections.singletonList(exampleTaskDefinition()) : Collections.emptyList();

    Mockito.doReturn(taskDefinitionReturn).when(testHarness.getTaskDefinitionRepository()).findByProductId("ble");

    final List<TaskDefinition> result = testHarness.getTestSubject().findAllEntities("ble");

    final List<TaskDefinition> expected = findTaskDefinition
            ? Collections.singletonList(TaskDefinitionMapper.map(exampleTaskDefinition()))
            : Collections.emptyList();

    Assert.assertEquals(expected, result);
  }

  @Test
  public void findByIdentifier()
  {
    findByIdentifierTestHelper(true, true);
    findByIdentifierTestHelper(false, false);
  }

  private void findByIdentifierTestHelper(boolean findTaskDefinition, boolean returnIsPresent) {
    final TestHarness testHarness = new TestHarness();

    final Optional<TaskDefinitionEntity> taskDefinitionReturn
            = findTaskDefinition ? Optional.of(exampleTaskDefinition()) : Optional.empty();

    Mockito.doReturn(taskDefinitionReturn).when(testHarness.getTaskDefinitionRepository()).findByProductIdAndTaskIdentifier("ble", "bla");

    final Optional<TaskDefinition> found = testHarness.getTestSubject().findByIdentifier("ble", "bla");
    Assert.assertEquals(found.isPresent(), returnIsPresent);
  }

  private TaskDefinitionEntity exampleTaskDefinition() {
    final TaskDefinitionEntity taskDef = new TaskDefinitionEntity();
    taskDef.setIdentifier("z");
    taskDef.setName("m");
    taskDef.setMandatory(false);
    taskDef.setActions("OPEN;CLOSE");
    taskDef.setDescription("n");
    taskDef.setFourEyes(true);

    final ProductEntity prod = new ProductEntity();
    taskDef.setProduct(prod);

    return taskDef;
  }
}