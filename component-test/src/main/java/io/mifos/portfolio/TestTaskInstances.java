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

import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.portfolio.api.v1.client.TaskExecutionBySameUserAsCaseCreation;
import io.mifos.portfolio.api.v1.client.TaskOutstanding;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.api.v1.domain.TaskInstance;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskDefinitionEvent;
import io.mifos.portfolio.api.v1.events.TaskInstanceEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
    markTaskExecuted(product, customerCase, taskDefinition);

    final TaskInstance executedTaskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    timeStampChecker.assertCorrect(executedTaskInstance.getExecutedOn());
    Assert.assertEquals(TEST_USER, executedTaskInstance.getExecutedBy());

    final List<TaskInstance> tasksForCaseExcludingExecuted = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), false);
    Assert.assertFalse(tasksForCaseExcludingExecuted.contains(taskInstance));

    final List<TaskInstance> allTasksForCase = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), true);
    Assert.assertTrue(allTasksForCase.contains(executedTaskInstance));
  }

  @Test
  public void shouldMarkBasicTaskNotExecuted() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    markTaskExecuted(product, customerCase, taskDefinition);

    final TaskInstance executedTaskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertNotNull(executedTaskInstance.getExecutedBy());
    Assert.assertNotNull(executedTaskInstance.getExecutedOn());

    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(),  taskDefinition.getIdentifier(), false);
    Assert.assertTrue(eventRecorder.wait(EventConstants.PUT_TASK_INSTANCE_EXECUTION, new TaskInstanceEvent(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier())));

    final TaskInstance unExecutedTaskInstance = portfolioManager.getTaskForCase(product.getIdentifier(), customerCase.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertNull(unExecutedTaskInstance.getExecutedOn());
    Assert.assertNull(unExecutedTaskInstance.getExecutedBy());

    final List<TaskInstance> tasksForCaseExcludingExecuted = portfolioManager.getAllTasksForCase(product.getIdentifier(), customerCase.getIdentifier(), false);
    Assert.assertTrue(tasksForCaseExcludingExecuted.contains(unExecutedTaskInstance));
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailToMarkNonExistantTask() throws InterruptedException {
    final Product product = createProduct();
    createTaskDefinition(product);

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(),  "blubble", true);
  }


  @Test(expected = TaskExecutionBySameUserAsCaseCreation.class)
  public void fourEyesCannotBeMarkedByCaseCreator() throws InterruptedException {
    final Product product = createProduct();

    final TaskDefinition taskDefinition = createTaskDefinition(product);
    taskDefinition.setFourEyes(true);
    portfolioManager.changeTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier(), taskDefinition);
    eventRecorder.wait(EventConstants.PUT_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier()));

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    portfolioManager.markTaskExecution(product.getIdentifier(), customerCase.getIdentifier(),  taskDefinition.getIdentifier(), true);
  }

  @Test
  public void fourEyesCanBeMarkedByDifferentUser() throws InterruptedException {
    final Product product = createProduct();

    final TaskDefinition taskDefinition = createTaskDefinition(product);
    taskDefinition.setFourEyes(true);
    portfolioManager.changeTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier(), taskDefinition);
    eventRecorder.wait(EventConstants.PUT_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier()));

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    try (final AutoUserContext ignored = this.tenantApplicationSecurityEnvironment.createAutoUserContext("fred")) {
      markTaskExecuted(product, customerCase, taskDefinition);
    }
  }

  @Test
  public void caseCannotBeOpenedUntilTaskCompleted() throws InterruptedException {
    final Product product = createProduct();

    final TaskDefinition taskDefinition = createTaskDefinition(product);
    taskDefinition.setActions(new HashSet<>(Arrays.asList(Action.OPEN.name(), Action.APPROVE.name())));
    portfolioManager.changeTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier(), taskDefinition);
    eventRecorder.wait(EventConstants.PUT_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier()));

    enableProduct(product);

    final Case customerCase = createCase(product.getIdentifier());

    try {
      checkStateTransferFails(
          product.getIdentifier(),
          customerCase.getIdentifier(),
          Action.OPEN,
          Collections.singletonList(assignEntryToTeller()),
          IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE,
          Case.State.CREATED);
    }
    catch (final TaskOutstanding ignored) {}

    markTaskExecuted(product, customerCase, taskDefinition);

    checkStateTransfer(
        product.getIdentifier(),
        customerCase.getIdentifier(),
        Action.OPEN,
        Collections.singletonList(assignEntryToTeller()),
        IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE,
        Case.State.PENDING);
  }
}
