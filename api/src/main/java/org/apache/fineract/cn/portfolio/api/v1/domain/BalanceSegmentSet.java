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
package org.apache.fineract.cn.portfolio.api.v1.domain;

import org.apache.fineract.cn.portfolio.api.v1.validation.ValidSegmentList;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifiers;

/**
 * @author Myrle Krantz
 */
@ValidSegmentList
public class BalanceSegmentSet {
  @ValidIdentifier
  private String identifier;

  private List<BigDecimal> segments;

  @ValidIdentifiers
  private List<String> segmentIdentifiers;

  public BalanceSegmentSet() {
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public List<BigDecimal> getSegments() {
    return segments;
  }

  public void setSegments(List<BigDecimal> segments) {
    this.segments = segments;
  }

  public List<String> getSegmentIdentifiers() {
    return segmentIdentifiers;
  }

  public void setSegmentIdentifiers(List<String> segmentNames) {
    this.segmentIdentifiers = segmentNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BalanceSegmentSet that = (BalanceSegmentSet) o;
    return Objects.equals(identifier, that.identifier) &&
        Objects.equals(segments, that.segments) &&
        Objects.equals(segmentIdentifiers, that.segmentIdentifiers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, segments, segmentIdentifiers);
  }

  @Override
  public String toString() {
    return "BalanceSegmentSet{" +
        "identifier='" + identifier + '\'' +
        ", segments=" + segments +
        ", segmentIdentifiers=" + segmentIdentifiers +
        '}';
  }
}