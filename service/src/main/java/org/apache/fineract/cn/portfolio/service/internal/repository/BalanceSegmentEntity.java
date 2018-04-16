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
package org.apache.fineract.cn.portfolio.service.internal.repository;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@Entity
@Table(name = "bastet_p_balance_segs")
public class BalanceSegmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductEntity product;

  @Column(name = "seg_set_identifier", nullable = false)
  private String segmentSetIdentifier;

  @Column(name = "segment_identifier", nullable = false)
  private String segmentIdentifier;

  @Column(name = "lower_bound")
  private BigDecimal lowerBound;

  public BalanceSegmentEntity() {
  }

  public BalanceSegmentEntity(ProductEntity product, String segmentSetIdentifier, String segmentIdentifier, BigDecimal lowerBound) {
    this.product = product;
    this.segmentSetIdentifier = segmentSetIdentifier;
    this.segmentIdentifier = segmentIdentifier;
    this.lowerBound = lowerBound;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProductEntity getProduct() {
    return product;
  }

  public void setProduct(ProductEntity product) {
    this.product = product;
  }

  public String getSegmentSetIdentifier() {
    return segmentSetIdentifier;
  }

  public void setSegmentSetIdentifier(String segmentSetIdentifier) {
    this.segmentSetIdentifier = segmentSetIdentifier;
  }

  public String getSegmentIdentifier() {
    return segmentIdentifier;
  }

  public void setSegmentIdentifier(String segmentIdentifier) {
    this.segmentIdentifier = segmentIdentifier;
  }

  public BigDecimal getLowerBound() {
    return lowerBound;
  }

  public void setLowerBound(BigDecimal lowerBound) {
    this.lowerBound = lowerBound;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BalanceSegmentEntity that = (BalanceSegmentEntity) o;
    return Objects.equals(product, that.product) &&
        Objects.equals(segmentSetIdentifier, that.segmentSetIdentifier) &&
        Objects.equals(segmentIdentifier, that.segmentIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(product, segmentSetIdentifier, segmentIdentifier);
  }
}