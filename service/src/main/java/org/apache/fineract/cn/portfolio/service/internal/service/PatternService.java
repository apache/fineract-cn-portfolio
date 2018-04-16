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
package org.apache.fineract.cn.portfolio.service.internal.service;

import org.apache.fineract.cn.portfolio.api.v1.domain.Pattern;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.apache.fineract.cn.products.spi.PatternFactory;
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
}
