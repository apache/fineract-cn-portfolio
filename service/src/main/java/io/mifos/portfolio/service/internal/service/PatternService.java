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

import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Pattern;
import io.mifos.products.spi.PatternFactory;
import io.mifos.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class PatternService {
  private final PatternFactoryRegistry patternFactoryRegistry;

  @Autowired
  public PatternService(final PatternFactoryRegistry patternFactoryRegistry) {
    super();
    this.patternFactoryRegistry = patternFactoryRegistry;

  }

  public List<Pattern> findAllEntities() {
    return patternFactoryRegistry.getAllPatternFactories().stream()
            .map(PatternFactory::pattern).collect(Collectors.toList());
  }

  public Optional<Pattern> findByIdentifier(final String identifier)
  {
    return patternFactoryRegistry.getPatternFactoryForPackage(identifier).map(PatternFactory::pattern);
  }

  public List<ChargeDefinition> findDefaultChargeDefinitions(final String patternPackage) {
    return patternFactoryRegistry.getPatternFactoryForPackage(patternPackage)
            .orElseThrow(() -> ServiceException.notFound("Pattern with package " + patternPackage + " doesn't exist."))
            .charges();
  }
}
