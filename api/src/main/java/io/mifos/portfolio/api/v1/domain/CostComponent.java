package io.mifos.portfolio.api.v1.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CostComponent {
  private String chargeIdentifier;
  private BigDecimal amount;

  public CostComponent() {
  }

  public CostComponent(String chargeIdentifier, BigDecimal amount) {
    this.chargeIdentifier = chargeIdentifier;
    this.amount = amount;
  }

  public String getChargeIdentifier() {
    return chargeIdentifier;
  }

  public void setChargeIdentifier(String chargeIdentifier) {
    this.chargeIdentifier = chargeIdentifier;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CostComponent that = (CostComponent) o;
    return Objects.equals(chargeIdentifier, that.chargeIdentifier) &&
            Objects.equals(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chargeIdentifier, amount);
  }

  @Override
  public String toString() {
    return "CostComponent{" +
            "chargeIdentifier='" + chargeIdentifier + '\'' +
            ", amount=" + amount +
            '}';
  }
}