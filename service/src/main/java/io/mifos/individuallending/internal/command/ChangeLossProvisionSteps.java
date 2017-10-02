package io.mifos.individuallending.internal.command;

import io.mifos.individuallending.api.v1.domain.product.LossProvisionStep;

import java.util.List;

/**
 * @author Myrle Krantz
 */
public class ChangeLossProvisionSteps {
  private final String productIdentifier;
  private final List<LossProvisionStep> lossProvisionSteps;

  public ChangeLossProvisionSteps(String productIdentifier, List<LossProvisionStep> lossProvisionSteps) {
    this.productIdentifier = productIdentifier;
    this.lossProvisionSteps = lossProvisionSteps;
  }
}
