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
package io.mifos.portfolio;

import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.api.v1.domain.TaskInstance;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskInstanceEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestTaskInstances extends AbstractPortfolioTest {

  @Test
  public void shouldListTaskInstances() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    final List<TaskInstance> taskInstances = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), false);
    Assert.assertEquals(1, taskInstances.size());
    Assert.assertEquals(taskDefinition.getIdentifier(), taskInstances.get(0).getTaskIdentifier());
  }

  @Test
  public void shouldCommentTaskInstance() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    final TaskInstance taskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    taskInstance.setComment("And the words of the prophets are written on the subway walls");

    portfolioManager.changeTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier(), taskInstance);
    Assert.assertTrue(eventRecorder.wait(EventConstants.PUT_TASK_INSTANCE, new TaskInstanceEvent(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier())));

    final TaskInstance taskInstanceChanged = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    taskInstance.setComment("And the words of the prophets are written on the subway walls");
    Assert.assertEquals(taskInstance, taskInstanceChanged);
  }

  @Test
  public void shouldMarkBasicTaskExecuted() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    final TaskInstance taskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());

    final TimeStampChecker timeStampChecker = TimeStampChecker.roughlyNow();
    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(),  taskDefinition.getIdentifier(), true);
    Assert.assertTrue(eventRecorder.wait(EventConstants.PUT_TASK_INSTANCE_EXECUTION, new TaskInstanceEvent(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier())));

    final TaskInstance executedTaskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    timeStampChecker.assertCorrect(executedTaskInstance.getExecutedOn());
    Assert.assertEquals(TEST_USER, executedTaskInstance.getExecutedBy());

    final List<TaskInstance> tasksForCaseExcludingExecuted = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), false);
    Assert.assertFalse(tasksForCaseExcludingExecuted.contains(taskInstance));

    final List<TaskInstance> allTasksForCase = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), true);
    Assert.assertTrue(allTasksForCase.contains(executedTaskInstance));
  }


  @Test(expected = NotFoundException.class)
  public void shouldFailToMarkNonExistantTask() throws InterruptedException {
    final Product product = createProduct();
    createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(),  "blubble", true);
  }
}
