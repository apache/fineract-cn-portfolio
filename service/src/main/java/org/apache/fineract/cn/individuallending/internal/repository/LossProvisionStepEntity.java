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
package org.apache.fineract.cn.individuallending.internal.repository;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "bastet_p_arrears_config")
public class LossProvisionStepEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "days_late", nullable = false)
  private Integer daysLate;

  @Column(name = "percent_provision", nullable = false)
  private BigDecimal percentProvision;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Integer getDaysLate() {
    return daysLate;
  }

  public void setDaysLate(Integer daysLate) {
    this.daysLate = daysLate;
  }

  public BigDecimal getPercentProvision() {
    return percentProvision;
  }

  public void setPercentProvision(BigDecimal percentProvision) {
    this.percentProvision = percentProvision;
  }
}