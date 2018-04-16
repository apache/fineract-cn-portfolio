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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Myrle Krantz
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestAccountingInteractionInLoanWorkflow.class,
    TestBalanceSegmentSets.class,
    TestCases.class,
    TestChargeDefinitions.class,
    TestCommands.class,
    TestIndividualLoans.class,
    TestPatterns.class,
    TestProducts.class,
    TestTaskDefinitions.class,
    TestTaskInstances.class,
    TestLossProvisionSteps.class,
    TestCaseDocuments.class
})
public class TestSuite extends SuiteTestEnvironment {
}
