/*
 * Copyright 2017 Kuelap, Inc.
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
package io.mifos.individuallending.internal.command;

/**
 * @author Myrle Krantz
 */
public class MarkLateCommand {
  private final String productIdentifier;
  private final String caseIdentifier;
  private final String forTime;

  public MarkLateCommand(String productIdentifier, String caseIdentifier, String forTime) {
    this.productIdentifier = productIdentifier;
    this.caseIdentifier = caseIdentifier;
    this.forTime = forTime;
  }

  public String getProductIdentifier() {
    return productIdentifier;
  }

  public String getCaseIdentifier() {
    return caseIdentifier;
  }

  public String getForTime() {
    return forTime;
  }

  @Override
  public String toString() {
    return "MarkLateCommand{" +
        "productIdentifier='" + productIdentifier + '\'' +
        ", caseIdentifier='" + caseIdentifier + '\'' +
        ", forTime='" + forTime + '\'' +
        '}';
  }
}