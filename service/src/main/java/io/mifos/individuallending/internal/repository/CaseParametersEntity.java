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
package io.mifos.individuallending.internal.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * @author Myrle Krantz
 */
@Entity
@Table(name = "bastet_il_cases")
public class CaseParametersEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "case_id")
  private Long caseId;

  @Column(name = "customer_identifier")
  private String customerIdentifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "term_range_temporal_unit")
  private ChronoUnit termRangeTemporalUnit;

  @Column(name = "term_range_minimum")
  private Integer termRangeMinimum;

  @Column(name = "term_range_maximum")
  private Integer termRangeMaximum;

  @Column(name = "balance_range_maximum")
  private BigDecimal balanceRangeMaximum;

  @Enumerated(EnumType.STRING)
  @Column(name = "pay_cycle_temporal_unit")
  private ChronoUnit paymentCycleTemporalUnit;

  @Column(name = "pay_cycle_period")
  private Integer paymentCyclePeriod;

  @Column(name = "pay_cycle_align_day")
  private Integer paymentCycleAlignmentDay;

  @Column(name = "pay_cycle_align_week")
  private Integer paymentCycleAlignmentWeek;

  @Column(name = "pay_cycle_align_month")
  private Integer paymentCycleAlignmentMonth;

  public CaseParametersEntity() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCaseId() {
    return caseId;
  }

  public void setCaseId(Long caseId) {
    this.caseId = caseId;
  }

  public String getCustomerIdentifier() {
    return customerIdentifier;
  }

  public void setCustomerIdentifier(String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public ChronoUnit getTermRangeTemporalUnit() {
    return termRangeTemporalUnit;
  }

  public void setTermRangeTemporalUnit(ChronoUnit termRangeTemporalUnit) {
    this.termRangeTemporalUnit = termRangeTemporalUnit;
  }

  public Integer getTermRangeMinimum() {
    return termRangeMinimum;
  }

  public void setTermRangeMinimum(Integer termRangeMinimum) {
    this.termRangeMinimum = termRangeMinimum;
  }

  public Integer getTermRangeMaximum() {
    return termRangeMaximum;
  }

  public void setTermRangeMaximum(Integer termRangeMaximum) {
    this.termRangeMaximum = termRangeMaximum;
  }

  public BigDecimal getBalanceRangeMaximum() {
    return balanceRangeMaximum;
  }

  public void setBalanceRangeMaximum(BigDecimal balanceRangeMaximum) {
    this.balanceRangeMaximum = balanceRangeMaximum;
  }

  public ChronoUnit getPaymentCycleTemporalUnit() {
    return paymentCycleTemporalUnit;
  }

  public void setPaymentCycleTemporalUnit(ChronoUnit paymentCycleTemporalUnit) {
    this.paymentCycleTemporalUnit = paymentCycleTemporalUnit;
  }

  public Integer getPaymentCyclePeriod() {
    return paymentCyclePeriod;
  }

  public void setPaymentCyclePeriod(Integer paymentCyclePeriod) {
    this.paymentCyclePeriod = paymentCyclePeriod;
  }

  public Integer getPaymentCycleAlignmentDay() {
    return paymentCycleAlignmentDay;
  }

  public void setPaymentCycleAlignmentDay(Integer paymentCycleAlignmentDay) {
    this.paymentCycleAlignmentDay = paymentCycleAlignmentDay;
  }

  public Integer getPaymentCycleAlignmentWeek() {
    return paymentCycleAlignmentWeek;
  }

  public void setPaymentCycleAlignmentWeek(Integer paymentCycleAlignmentWeek) {
    this.paymentCycleAlignmentWeek = paymentCycleAlignmentWeek;
  }

  public Integer getPaymentCycleAlignmentMonth() {
    return paymentCycleAlignmentMonth;
  }

  public void setPaymentCycleAlignmentMonth(Integer paymentCycleAlignmentMonth) {
    this.paymentCycleAlignmentMonth = paymentCycleAlignmentMonth;
  }
}
