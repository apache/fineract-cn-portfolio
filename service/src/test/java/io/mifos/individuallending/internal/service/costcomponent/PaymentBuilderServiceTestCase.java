package io.mifos.individuallending.internal.service.costcomponent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

class PaymentBuilderServiceTestCase {
  private final String description;
  private LocalDate startOfTerm = LocalDate.of(2015, 1, 15);
  LocalDateTime endOfTerm = LocalDate.of(2015, 8, 15).atStartOfDay();
  LocalDate forDate = startOfTerm.plusMonths(1);
  BigDecimal paymentSize = BigDecimal.valueOf(100_00, 2);
  BigDecimal balance = BigDecimal.valueOf(2000_00, 2);
  BigDecimal balanceRangeMaximum = BigDecimal.valueOf(1000_00, 2);
  BigDecimal interestRate = BigDecimal.valueOf(5_00, 2);
  BigDecimal accruedInterest = BigDecimal.valueOf(10_00, 2);
  SimulatedRunningBalances runningBalances;

  PaymentBuilderServiceTestCase(final String description) {
    this.description = description;
    runningBalances = new SimulatedRunningBalances(startOfTerm);
  }

  PaymentBuilderServiceTestCase endOfTerm(LocalDateTime endOfTerm) {
    this.endOfTerm = endOfTerm;
    return this;
  }

  PaymentBuilderServiceTestCase forDate(LocalDate forDate) {
    this.forDate = forDate;
    return this;
  }

  PaymentBuilderServiceTestCase paymentSize(BigDecimal paymentSize) {
    this.paymentSize = paymentSize;
    return this;
  }

  PaymentBuilderServiceTestCase balance(BigDecimal balance) {
    this.balance = balance;
    return this;
  }

  PaymentBuilderServiceTestCase balanceRangeMaximum(BigDecimal balanceRangeMaximum) {
    this.balanceRangeMaximum = balanceRangeMaximum;
    return this;
  }

  PaymentBuilderServiceTestCase interestRate(BigDecimal interestRate) {
    this.interestRate = interestRate;
    return this;
  }

  PaymentBuilderServiceTestCase accruedInterest(BigDecimal accruedInterest) {
    this.accruedInterest = accruedInterest;
    return this;
  }

  PaymentBuilderServiceTestCase runningBalances(SimulatedRunningBalances newVal) {
    this.runningBalances = newVal;
    return this;
  }

  @Override
  public String toString() {
    return "PaymentBuilderServiceTestCase{" +
        "description='" + description + '\'' +
        '}';
  }
}