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
package org.apache.fineract.cn.portfolio.service.internal.mapper;

import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class BalanceSegmentSetMapper {
  public static List<BalanceSegmentEntity> map(final BalanceSegmentSet instance, final ProductEntity product) {
    return
        Stream.iterate(0, i -> i+1).limit(instance.getSegmentIdentifiers().size())
            .map(i -> {
              final BalanceSegmentEntity ret = new BalanceSegmentEntity();
              ret.setProduct(product);
              ret.setSegmentSetIdentifier(instance.getIdentifier());
              ret.setSegmentIdentifier(instance.getSegmentIdentifiers().get(i));
              ret.setLowerBound(instance.getSegments().get(i));
              return ret;
            })
            .collect(Collectors.toList());
  }

  public static Optional<BalanceSegmentSet> map(final Stream<BalanceSegmentEntity> instances) {
    final BalanceSegmentSet ret = new BalanceSegmentSet();
    ret.setSegments(new ArrayList<>());
    ret.setSegmentIdentifiers(new ArrayList<>());
    instances.sorted(Comparator.comparing(BalanceSegmentEntity::getLowerBound))
        .forEach(seg -> {
          ret.setIdentifier(seg.getSegmentSetIdentifier());
          ret.getSegments().add(seg.getLowerBound());
          ret.getSegmentIdentifiers().add(seg.getSegmentIdentifier());
        });
    if (ret.getSegments().isEmpty())
      return Optional.empty();
    else
      return Optional.of(ret);
  }
}