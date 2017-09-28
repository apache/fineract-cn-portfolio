package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.internal.service.DataContextOfAction;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentBuilderService {

  PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final BigDecimal forPaymentSize,
      final LocalDate forDate,
      final @Nonnull RunningBalances runningBalances);
}
