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
package org.apache.fineract.cn.portfolio.service.config;

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.IndividualLendingConfiguration;
import org.apache.fineract.cn.portfolio.service.ServiceConstants;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.anubis.config.EnableAnubis;
import org.apache.fineract.cn.async.config.EnableAsync;
import org.apache.fineract.cn.cassandra.config.EnableCassandra;
import org.apache.fineract.cn.command.config.EnableCommandProcessing;
import org.apache.fineract.cn.customer.api.v1.client.CustomerManager;
import org.apache.fineract.cn.lang.config.EnableApplicationName;
import org.apache.fineract.cn.lang.config.EnableServiceException;
import org.apache.fineract.cn.lang.config.EnableTenantContext;
import org.apache.fineract.cn.mariadb.config.EnableMariaDB;
import org.apache.fineract.cn.rhythm.api.v1.client.RhythmManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@EnableAsync
@EnableTenantContext
@EnableMariaDB
@EnableCassandra
@EnableCommandProcessing
@EnableAnubis
@EnableServiceException
@ComponentScan({
    "org.apache.fineract.cn.portfolio.service.rest",
    "org.apache.fineract.cn.portfolio.service.internal",
    "org.apache.fineract.cn.portfolio.service.config",
})
@EnableJpaRepositories(basePackages = "org.apache.fineract.cn.portfolio.service.internal.repository")
@EntityScan(basePackages = "org.apache.fineract.cn.portfolio.service.internal.repository")
@EnableFeignClients(clients = {LedgerManager.class, RhythmManager.class, CustomerManager.class})
@RibbonClient(name = "portfolio-v1")
@EnableApplicationName
@Import(IndividualLendingConfiguration.class)
public class PortfolioServiceConfiguration extends WebMvcConfigurerAdapter {

  public PortfolioServiceConfiguration() {
    super();
  }

  @Bean(name = ServiceConstants.LOGGER_NAME)
  public Logger logger() {
    return LoggerFactory.getLogger(ServiceConstants.LOGGER_NAME);
  }

  @Bean(name = ServiceConstants.GSON_NAME)
  public Gson gson() {
    return new Gson();
  }

  @Override
  public void configurePathMatch(final PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(Boolean.FALSE);
  }
}
