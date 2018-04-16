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
package org.apache.fineract.cn.portfolio.service.internal.command.handler;

import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.service.internal.command.InitializeServiceCommand;
import org.apache.fineract.cn.portfolio.service.internal.util.RhythmAdapter;
import javax.sql.DataSource;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.mariadb.domain.FlywayFactoryBean;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({
    "unused"
})
@Aggregate
public class InitializeCommandHandler {

  private final DataSource dataSource;
  private final FlywayFactoryBean flywayFactoryBean;
  private final RhythmAdapter rhythmAdapter;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  public InitializeCommandHandler(final DataSource dataSource,
                                  final FlywayFactoryBean flywayFactoryBean,
                                  final RhythmAdapter rhythmAdapter) {
    super();
    this.dataSource = dataSource;
    this.flywayFactoryBean = flywayFactoryBean;
    this.rhythmAdapter = rhythmAdapter;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.INITIALIZE)
  public String initialize(final InitializeServiceCommand initializeServiceCommand) {
    final Flyway flyway = this.flywayFactoryBean.create(this.dataSource);
    System.out.println("Baseline version: " + flyway.getBaselineVersion());
    flyway.migrate();
    rhythmAdapter.request24Beats();
    return EventConstants.INITIALIZE;
  }
}
