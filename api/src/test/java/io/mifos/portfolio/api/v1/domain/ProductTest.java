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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.Fixture;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class ProductTest extends ValidationTest<Product> {
  public ProductTest(ValidationTestCase<Product> testCase) {
    super(testCase);
  }

  @Override
  protected Product createValidTestSubject() {
    return Fixture.getTestProduct();
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<Product>("validProduct")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Product>("noIdentifier")
            .adjustment(product -> product.setIdentifier(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooShortIdentifier")
            .adjustment(product -> product.setIdentifier("b"))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooLongIdentifier")
            .adjustment(product -> product.setIdentifier(RandomStringUtils.randomAlphanumeric(33)))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nonURLSafeProductIdentifier")
            .adjustment(product -> product.setIdentifier("bad//name"))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullName")
            .adjustment(product -> product.setName(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooShortName")
            .adjustment(product -> product.setName("c"))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooLongName")
            .adjustment(product -> product.setName(StringUtils.repeat("x", 257)))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullTermRange")
            .adjustment(product -> product.setTermRange(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("negativeTermRangeMax")
            .adjustment(product -> product.setTermRange(new TermRange(ChronoUnit.CENTURIES, -1)))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullBalanceRange")
            .adjustment(product -> product.setBalanceRange(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("switchedBalanceRangeMinMax")
            .adjustment(product -> product.setBalanceRange(new BalanceRange(Fixture.fixScale(BigDecimal.TEN), Fixture.fixScale(BigDecimal.ZERO))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullInterestRange")
            .adjustment(product -> product.setInterestRange(null))
            .valid(false));
    //noinspection BigDecimalMethodWithoutRoundingCalled
    ret.add(new ValidationTestCase<Product>("switchedInterestRangeMinMax")
            .adjustment(product -> product.setInterestRange(new InterestRange(BigDecimal.valueOf(200, 2), BigDecimal.valueOf(0.9).setScale(2))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooBigMaximumInterestRange")
            .adjustment(product -> product.setInterestRange(new InterestRange(new BigDecimal("999.99"), new BigDecimal("1000.00"))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("negativeMinimumInterestRange")
            .adjustment(product -> product.setInterestRange(new InterestRange(BigDecimal.valueOf(-1, 2), BigDecimal.valueOf(1, 2))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooManyDigitsAfterTheDecimalInterestRange")
            .adjustment(product -> product.setInterestRange(new InterestRange(BigDecimal.valueOf(1, 2), new BigDecimal("1.001"))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullInterestBasis")
            .adjustment(product -> product.setInterestBasis(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("nullPatternPackage")
            .adjustment(product -> product.setPatternPackage(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooLongDescription")
            .adjustment(product -> product.setDescription(StringUtils.repeat("x", 4097)))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooLongAccountIdentifier")
            .adjustment(product -> product.getAccountAssignments().add(new AccountAssignment("xyz", StringUtils.repeat("0", 35))))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("duplicateAccountAssignment")
            .adjustment(product -> {
              product.getAccountAssignments().add(new AccountAssignment("xyz", "002-011"));
              product.getAccountAssignments().add(new AccountAssignment("xyz", "002-012"));
            })
            .valid(false));
    ret.add(new ValidationTestCase<Product>("additionalAccountAssignment")
            .adjustment(product -> {
              product.getAccountAssignments().add(new AccountAssignment("xyz", "002-011"));
              product.getAccountAssignments().add(new AccountAssignment("mno", "002-012"));
            })
            .valid(true));
    ret.add(new ValidationTestCase<Product>("nullCurrencyCode")
            .adjustment(product -> product.setCurrencyCode(null))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooLongCurrencyCode")
            .adjustment(product -> product.setCurrencyCode("ABCDE"))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("tooShortCurrencyCode")
            .adjustment(product -> product.setCurrencyCode("AB"))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("negativeMinorCurrencyDigits")
            .adjustment(product -> product.setMinorCurrencyUnitDigits(-1))
            .valid(false));
    ret.add(new ValidationTestCase<Product>("minorCurrencyDigitsLargerThan4")
            .adjustment(product -> product.setMinorCurrencyUnitDigits(5))
            .valid(false));

    return ret;
  }
}