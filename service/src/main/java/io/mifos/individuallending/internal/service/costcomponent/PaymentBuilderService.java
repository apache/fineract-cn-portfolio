package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.internal.service.DataContextOfAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentBuilderService {
  PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedDisbursalSize,
      final LocalDate forDate);
}
