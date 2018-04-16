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
import org.apache.fineract.cn.products.spi.PatternFactory;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class PatternServiceTest {
  @Test
  public void findAllEntities() throws Exception {
    final PatternFactoryRegistry registryMock = Mockito.mock(PatternFactoryRegistry.class);
    final PatternFactory patternFactoryMock = Mockito.mock(PatternFactory.class);
    final Pattern patternMock = Mockito.mock(Pattern.class);
    Mockito.doReturn(Optional.of(patternFactoryMock)).when(registryMock).getPatternFactoryForPackage("org.apache.fineract.cn.individuallending.api.v1");
    Mockito.doReturn(Collections.singleton(patternFactoryMock)).when(registryMock).getAllPatternFactories();
    Mockito.doReturn(patternMock).when(patternFactoryMock).pattern();
    Mockito.doReturn("org.apache.fineract.cn.individuallending.api.v1").when(patternMock).getParameterPackage();

    final PatternService testSubject = new PatternService(registryMock);
    final List<Pattern> all = testSubject.findAllEntities();
    Assert.assertTrue(all.stream().anyMatch(pattern ->
      pattern.getParameterPackage().equals("org.apache.fineract.cn.individuallending.api.v1")));

    final Optional<Pattern> pattern = testSubject.findByIdentifier("org.apache.fineract.cn.individuallending.api.v1");
    Assert.assertTrue(pattern.isPresent());
  }
}