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
package io.mifos.individuallending.internal.mapper;

import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.portfolio.api.v1.domain.PaymentCycle;
import io.mifos.portfolio.api.v1.domain.TermRange;

/**
 * @author Myrle Krantz
 */
public class CaseParametersMapper {

  public static CaseParametersEntity map(final Long caseId, final CaseParameters instance) {
    final CaseParametersEntity ret = new CaseParametersEntity();

    ret.setCaseId(caseId);
    ret.setCustomerIdentifier(instance.getCustomerIdentifier());
    ret.setTermRangeTemporalUnit(instance.getTermRange().getTemporalUnit());
    ret.setTermRangeMinimum(0);
    ret.setTermRangeMaximum(instance.getTermRange().getMaximum());
    ret.setBalanceRangeMaximum(instance.getMaximumBalance());
    ret.setPaymentCycleTemporalUnit(instance.getPaymentCycle().getTemporalUnit());
    ret.setPaymentCyclePeriod(instance.getPaymentCycle().getPeriod());
    ret.setPaymentCycleAlignmentDay(instance.getPaymentCycle().getAlignmentDay());
    ret.setPaymentCycleAlignmentWeek(instance.getPaymentCycle().getAlignmentWeek());
    ret.setPaymentCycleAlignmentMonth(instance.getPaymentCycle().getAlignmentMonth());

    return ret;
  }

  private static TermRange getTermRange(final CaseParametersEntity instance) {
    final TermRange ret = new TermRange();

    ret.setTemporalUnit(instance.getTermRangeTemporalUnit());
    ret.setMaximum(instance.getTermRangeMaximum());

    return ret;
  }

  private static PaymentCycle getPaymentCycle(final CaseParametersEntity instance) {
    final PaymentCycle ret = new PaymentCycle();

    ret.setTemporalUnit(instance.getPaymentCycleTemporalUnit());
    ret.setPeriod(instance.getPaymentCyclePeriod());
    ret.setAlignmentDay(instance.getPaymentCycleAlignmentDay());
    ret.setAlignmentWeek(instance.getPaymentCycleAlignmentWeek());
    ret.setAlignmentMonth(instance.getPaymentCycleAlignmentMonth());

    return ret;
  }

  public static CaseParameters mapEntity(final CaseParametersEntity caseParametersEntity) {
    final CaseParameters ret = new CaseParameters();
    ret.setCustomerIdentifier(caseParametersEntity.getCustomerIdentifier());
    ret.setTermRange(getTermRange(caseParametersEntity));
    ret.setMaximumBalance(caseParametersEntity.getBalanceRangeMaximum());
    ret.setPaymentCycle(getPaymentCycle(caseParametersEntity));
    return ret;
  }
}
