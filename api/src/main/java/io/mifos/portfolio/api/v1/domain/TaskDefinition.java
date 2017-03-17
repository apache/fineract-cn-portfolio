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

import io.mifos.core.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class TaskDefinition {
  @ValidIdentifier
  private String identifier;

  @NotBlank
  private String name;

  @NotBlank
  private String description;

  @NotNull
  private Set<String> actions;

  @NotNull
  private Boolean fourEyes;

  @NotNull
  private Boolean mandatory;

  public TaskDefinition() {
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<String> getActions() {
    return actions;
  }

  public void setActions(Set<String> actions) {
    this.actions = actions;
  }

  public Boolean getFourEyes() {
    return fourEyes;
  }

  public void setFourEyes(Boolean fourEyes) {
    this.fourEyes = fourEyes;
  }

  public Boolean getMandatory() {
    return mandatory;
  }

  public void setMandatory(Boolean mandatory) {
    this.mandatory = mandatory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskDefinition that = (TaskDefinition) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(actions, that.actions) &&
            Objects.equals(fourEyes, that.fourEyes) &&
            Objects.equals(mandatory, that.mandatory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, description, actions, fourEyes, mandatory);
  }

  @Override
  public String toString() {
    return "TaskDefinition{" +
            "identifier='" + identifier + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", actions=" + actions +
            ", fourEyes=" + fourEyes +
            ", mandatory=" + mandatory +
            '}';
  }
}