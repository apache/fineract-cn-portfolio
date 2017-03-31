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
package io.mifos.portfolio.service.internal.repository;

import io.mifos.portfolio.service.internal.util.AccountingAdapter;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "bastet_p_acct_assigns")
public class ProductAccountAssignmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductEntity product;

  @Column(name = "designator")
  private String designator;

  @Column(name = "identifier")
  private String identifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "thoth_type")
  private AccountingAdapter.IdentifierType thothType;

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

  public String getDesignator() {
    return designator;
  }

  public void setDesignator(String designator) {
    this.designator = designator;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String thothIdentifier) {
    this.identifier = thothIdentifier;
  }

  public AccountingAdapter.IdentifierType getType() {
    return thothType;
  }

  public void setType(AccountingAdapter.IdentifierType type) {
    this.thothType = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof ProductAccountAssignmentEntity)) return false;
    ProductAccountAssignmentEntity that = (ProductAccountAssignmentEntity) o;
    return Objects.equals(getDesignator(), that.getDesignator());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDesignator());
  }
}
