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
package org.apache.fineract.cn.portfolio;

import org.apache.fineract.cn.portfolio.api.v1.client.ProductInUseException;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.api.v1.events.TaskDefinitionEvent;
import java.util.List;
import org.apache.fineract.cn.api.util.NotFoundException;
import org.junit.Assert;
import org.junit.Test;

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

  @Test(expected = ProductInUseException.class)
  public void shouldNotChangeTaskDefinitionForProductWithCases() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);

    createCase(product.getIdentifier());

    taskDefinition.setDescription("bleblablub");
    taskDefinition.setFourEyes(false);

    portfolioManager.changeTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier(), taskDefinition);
    Assert.assertFalse(this.eventRecorder.wait(EventConstants.PUT_TASK_DEFINITION,
        new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));
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

  @Test(expected = ProductInUseException.class)
  public void shouldNotCreateTaskDefinitionForProductWithCases() throws InterruptedException {
    final Product product = createAndEnableProduct();
    createCase(product.getIdentifier());

    final TaskDefinition taskDefinition = getTaskDefinition();
    portfolioManager.createTaskDefinition(product.getIdentifier(), taskDefinition);
    Assert.assertFalse(this.eventRecorder.wait(EventConstants.POST_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));
  }

  @Test
  public void shouldDeleteTaskDefinition() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    portfolioManager.deleteTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.DELETE_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));

    try {
      portfolioManager.getTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier());
      Assert.fail();
    }
    catch (final NotFoundException ignored) {
    }
  }

  @Test(expected = ProductInUseException.class)
  public void shouldNotDeleteTaskDefinitionForProductWithCases() throws InterruptedException {
    final Product product = createProduct();
    final TaskDefinition taskDefinition = createTaskDefinition(product);

    enableProduct(product);
    createCase(product.getIdentifier());

    portfolioManager.deleteTaskDefinition(product.getIdentifier(), taskDefinition.getIdentifier());
    Assert.assertFalse(this.eventRecorder.wait(EventConstants.DELETE_TASK_DEFINITION, new TaskDefinitionEvent(product.getIdentifier(), taskDefinition.getIdentifier())));
  }
}