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
package io.mifos.portfolio.service.config;

import com.google.gson.Gson;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.anubis.config.EnableAnubis;
import io.mifos.core.async.config.EnableAsync;
import io.mifos.core.cassandra.config.EnableCassandra;
import io.mifos.core.command.config.EnableCommandProcessing;
import io.mifos.core.lang.config.EnableApplicationName;
import io.mifos.core.lang.config.EnableServiceException;
import io.mifos.core.lang.config.EnableTenantContext;
import io.mifos.core.mariadb.config.EnableMariaDB;
import io.mifos.individuallending.IndividualLendingConfiguration;
import io.mifos.portfolio.service.ServiceConstants;
import io.mifos.rhythm.api.v1.client.RhythmManager;
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
    "io.mifos.portfolio.service.rest",
    "io.mifos.portfolio.service.internal",
    "io.mifos.portfolio.service.config",
})
@EnableJpaRepositories(basePackages = "io.mifos.portfolio.service.internal.repository")
@EntityScan(basePackages = "io.mifos.portfolio.service.internal.repository")
@EnableFeignClients(clients = {LedgerManager.class, RhythmManager.class})
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
