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
package io.mifos.portfolio.service.internal.command;

import io.mifos.portfolio.api.v1.domain.Case;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class ChangeCaseCommand {
  private Case instance;

  public ChangeCaseCommand() {
  }

  public ChangeCaseCommand(Case instance) {
    this.instance = instance;
  }

  public Case getInstance() {
    return instance;
  }

  public void setInstance(Case instance) {
    this.instance = instance;
  }

  @Override
  public String toString() {
    return "ChangeCaseCommand{" +
            "instance=" + instance.getIdentifier() +
            '}';
  }
}
