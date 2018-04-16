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
package org.apache.fineract.cn.portfolio.api.v1.events;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface EventConstants {
  String DESTINATION = "portfolio-v1";
  String SELECTOR_NAME = "action";
  String INITIALIZE = "initialize";
  String POST_PRODUCT = "post-product";
  String PUT_PRODUCT = "put-product";
  String DELETE_PRODUCT = "delete-product";
  String PUT_PRODUCT_ENABLE = "put-enable";
  String POST_CASE = "post-case";
  String PUT_CASE = "put-case";
  String POST_BALANCE_SEGMENT_SET = "post-balance-segment-set";
  String PUT_BALANCE_SEGMENT_SET = "put-balance-segment-set";
  String DELETE_BALANCE_SEGMENT_SET = "delete-balance-segment-set";
  String POST_TASK_DEFINITION = "post-task-definition";
  String PUT_TASK_DEFINITION = "put-task-definition";
  String DELETE_TASK_DEFINITION = "delete-task-definition";
  String POST_CHARGE_DEFINITION = "post-charge-definition";
  String PUT_TASK_INSTANCE = "put-task-instance";
  String PUT_TASK_INSTANCE_EXECUTION = "put-task-instance-execution";
  String PUT_CHARGE_DEFINITION = "put-charge-definition";
  String DELETE_PRODUCT_CHARGE_DEFINITION = "delete-product-charge-definition";
  String SELECTOR_INITIALIZE = SELECTOR_NAME + " = '" + INITIALIZE + "'";
  String SELECTOR_POST_PRODUCT = SELECTOR_NAME + " = '" + POST_PRODUCT + "'";
  String SELECTOR_PUT_PRODUCT = SELECTOR_NAME + " = '" + PUT_PRODUCT + "'";
  String SELECTOR_DELETE_PRODUCT = SELECTOR_NAME + " = '" + DELETE_PRODUCT + "'";
  String SELECTOR_PUT_PRODUCT_ENABLE = SELECTOR_NAME + " = '" + PUT_PRODUCT_ENABLE + "'";
  String SELECTOR_POST_CASE = SELECTOR_NAME + " = '" + POST_CASE + "'";
  String SELECTOR_PUT_CASE = SELECTOR_NAME + " = '" + PUT_CASE + "'";
  String SELECTOR_POST_BALANCE_SEGMENT_SET = SELECTOR_NAME + " = '" + POST_BALANCE_SEGMENT_SET + "'";
  String SELECTOR_PUT_BALANCE_SEGMENT_SET = SELECTOR_NAME + " = '" + PUT_BALANCE_SEGMENT_SET + "'";
  String SELECTOR_DELETE_BALANCE_SEGMENT_SET = SELECTOR_NAME + " = '" + DELETE_BALANCE_SEGMENT_SET + "'";
  String SELECTOR_POST_TASK_DEFINITION = SELECTOR_NAME + " = '" + POST_TASK_DEFINITION + "'";
  String SELECTOR_PUT_TASK_DEFINITION = SELECTOR_NAME + " = '" + PUT_TASK_DEFINITION + "'";
  String SELECTOR_DELETE_TASK_DEFINITION = SELECTOR_NAME + " = '" + DELETE_TASK_DEFINITION + "'";
  String SELECTOR_PUT_TASK_INSTANCE = SELECTOR_NAME + " = '" + PUT_TASK_INSTANCE + "'";
  String SELECTOR_PUT_TASK_INSTANCE_EXECUTION = SELECTOR_NAME + " = '" + PUT_TASK_INSTANCE_EXECUTION + "'";
  String SELECTOR_POST_CHARGE_DEFINITION = SELECTOR_NAME + " = '" + POST_CHARGE_DEFINITION + "'";
  String SELECTOR_PUT_CHARGE_DEFINITION = SELECTOR_NAME + " = '" + PUT_CHARGE_DEFINITION + "'";
  String SELECTOR_DELETE_PRODUCT_CHARGE_DEFINITION = SELECTOR_NAME + " = '" + DELETE_PRODUCT_CHARGE_DEFINITION + "'";
}
