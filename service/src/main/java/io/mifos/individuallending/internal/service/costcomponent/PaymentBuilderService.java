package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentBuilderService {
  static CostComponent getAccruedCostComponentToApply(
      final RunningBalances runningBalances,
      final DataContextOfAction dataContextOfAction,
      final LocalDate startOfTerm,
      final ChargeDefinition chargeDefinition) {
    final CostComponent ret = new CostComponent();
    ret.setChargeIdentifier(chargeDefinition.getIdentifier());
    ret.setAmount(runningBalances.getAccruedBalanceForCharge(dataContextOfAction, startOfTerm, chargeDefinition));
    return ret;
  }

  PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedDisbursalSize,
      final LocalDate forDate);
}
