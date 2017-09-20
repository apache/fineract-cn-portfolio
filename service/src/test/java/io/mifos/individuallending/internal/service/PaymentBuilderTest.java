package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PaymentBuilderTest {
  @Test
  public void expandAccountDesignators() {
    final Set<String> ret = PaymentBuilder.expandAccountDesignators(new HashSet<>(Arrays.asList(AccountDesignators.CUSTOMER_LOAN_GROUP, AccountDesignators.ENTRY)));
    final Set<String> expected = new HashSet<>(Arrays.asList(
        AccountDesignators.ENTRY,
        AccountDesignators.CUSTOMER_LOAN_GROUP,
        AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
        AccountDesignators.CUSTOMER_LOAN_FEES,
        AccountDesignators.CUSTOMER_LOAN_INTEREST));

    Assert.assertEquals(expected, ret);
  }

}