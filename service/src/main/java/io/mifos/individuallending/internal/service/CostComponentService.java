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
package io.mifos.individuallending.internal.service;

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class CostComponentService {

  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final IndividualLoanService individualLoanService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public CostComponentService(
          final ProductRepository productRepository,
          final CaseRepository caseRepository,
          final CaseParametersRepository caseParametersRepository,
          final IndividualLoanService individualLoanService,
          final AccountingAdapter accountingAdapter) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.individualLoanService = individualLoanService;
    this.accountingAdapter = accountingAdapter;
  }

  public DataContextOfAction checkedGetDataContext(
          final String productIdentifier,
          final String caseIdentifier,
          final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
            productRepository.findByIdentifier(productIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
    final CaseEntity customerCase =
            caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Case not found ''{0}.{1}''.", productIdentifier, caseIdentifier));

    final CaseParameters caseParameters =
            caseParametersRepository.findByCaseId(customerCase.getId())
                    .map(CaseParametersMapper::mapEntity)
                    .orElseThrow(() -> ServiceException.notFound(
                            "Individual loan not found ''{0}.{1}''.",
                            productIdentifier, caseIdentifier));

    return new DataContextOfAction(product, customerCase, caseParameters, oneTimeAccountAssignments);
  }

  public List<CostComponent> getCostComponents(final String productIdentifier, final String caseIdentifier, final Action action) {
    final DataContextOfAction context = checkedGetDataContext(productIdentifier, caseIdentifier, Collections.emptyList());
    final Case.State caseState = Case.State.valueOf(context.getCustomerCase().getCurrentState());
    final BigDecimal runningBalance;
    if (caseState == Case.State.ACTIVE) {
      final DesignatorToAccountIdentifierMapper mapper = new DesignatorToAccountIdentifierMapper(context);
      final String customerLoanAccountIdentifier = mapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
      runningBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier);
    }
    else
      runningBalance = BigDecimal.ZERO;

    return individualLoanService.getCostComponentsForRepaymentPeriod(productIdentifier, context.getCaseParameters(), runningBalance, action, LocalDate.now(ZoneId.of("UTC")), LocalDate.now(ZoneId.of("UTC")))
            .stream()
            .map(x -> new CostComponent(x.getKey().getIdentifier(), x.getValue().getAmount()))
            .collect(Collectors.toList()); //TODO: initial disbursal date.
  }
}
