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

import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskDefinitionEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestTaskDefinitions extends AbstractPortfolioTest {

  @Test
  public void shouldListTaskDefinitions() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    final List<TaskDefinition> tasks = portfolioManager.getAllTaskDefinitionsForProduct(product.getIdentifier());
    Assert.assertFalse(tasks.isEmpty());
    Assert.assertTrue(tasks.contains(taskDefinition));

    final TaskDefinition taskDefinitionRead = portfolioManager.getTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertEquals(taskDefinition, taskDefinitionRead);
  }

  @Test
  public void shouldChangeTaskDefinition() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);
    taskDefinition.setDescription("bleblablub");
    taskDefinition.setFourEyes(false);

    portfolioManager.changeTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier(), taskDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_TASK_DEFINITION,
            new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));

    final TaskDefinition taskDefinitionRead = portfolioManager.getTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertEquals(taskDefinition,taskDefinitionRead);
  }

  @Test
  public void shouldAddTaskDefinition() throws InterruptedException {
    final Product product = createProduct();

    final int initialTaskCount = portfolioManager.getAllTaskDefinitionsForProduct(product.getIdentifier()).size();

    final TaskDefinition taskDefinition = createTaskDefinition(product);

    final List<TaskDefinition> tasks = portfolioManager.getAllTaskDefinitionsForProduct(product.getIdentifier());
    Assert.assertTrue(tasks.contains(taskDefinition));
    Assert.assertTrue(tasks.size() == initialTaskCount + 1);
  }

  private TaskDefinition createTaskDefinition(Product product) throws InterruptedException {
    final TaskDefinition taskDefinition = getTaskDefinition();
    portfolioManager.createTaskDefinition(product.getIdentifier(), taskDefinition);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));
    return taskDefinition;
  }

  private TaskDefinition getTaskDefinition() {
    final TaskDefinition ret = new TaskDefinition();
    ret.setIdentifier(Fixture.generateUniqueIdentifer("task"));
    ret.setDescription("But how do you feel about this?");
    ret.setName("feep");
    ret.setMandatory(false);
    ret.setActions(new HashSet<>());
    ret.setFourEyes(true);
    return ret;
  }
}