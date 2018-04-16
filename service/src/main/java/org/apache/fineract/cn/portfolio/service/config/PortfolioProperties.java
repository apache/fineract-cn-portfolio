/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.portfolio.service.config;

import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
@ConfigurationProperties(prefix="portfolio")
@Validated
public class PortfolioProperties {
  @ValidIdentifier
  private String bookLateFeesAndInterestAsUser;

  @Range(min=0, max=23)
  private int bookInterestInTimeSlot = 0;

  @Range(min=0, max=23)
  private int checkForLatenessInTimeSlot = 0;

  public PortfolioProperties() {
  }

  public String getBookLateFeesAndInterestAsUser() {
    return bookLateFeesAndInterestAsUser;
  }

  public void setBookLateFeesAndInterestAsUser(String bookLateFeesAndInterestAsUser) {
    this.bookLateFeesAndInterestAsUser = bookLateFeesAndInterestAsUser;
  }

  public int getBookInterestInTimeSlot() {
    return bookInterestInTimeSlot;
  }

  public void setBookInterestInTimeSlot(int bookInterestInTimeSlot) {
    this.bookInterestInTimeSlot = bookInterestInTimeSlot;
  }

  public int getCheckForLatenessInTimeSlot() {
    return checkForLatenessInTimeSlot;
  }

  public void setCheckForLatenessInTimeSlot(int checkForLatenessInTimeSlot) {
    this.checkForLatenessInTimeSlot = checkForLatenessInTimeSlot;
  }
}
