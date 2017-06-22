--
-- Copyright 2017 The Mifos Initiative.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

# noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE bastet_products (
  id BIGINT NOT NULL AUTO_INCREMENT,
  identifier               VARCHAR(32)    NOT NULL,
  a_name                   VARCHAR(256)   NOT NULL,
  term_range_temporal_unit VARCHAR(128)   NOT NULL,
  term_range_minimum       INT            NOT NULL,
  term_range_maximum       INT            NOT NULL,
  balance_range_minimum    DECIMAL(19,4)  NOT NULL,
  balance_range_maximum    DECIMAL(19,4)  NOT NULL,
  interest_range_minimum   DECIMAL(5,2)   NOT NULL,
  interest_range_maximum   DECIMAL(5,2)   NOT NULL,
  interest_basis           VARCHAR(128)   NOT NULL,
  pattern_package          VARCHAR(512)   NOT NULL,
  description              VARCHAR(4096)  NULL,
  parameters               VARCHAR(8092)  NOT NULL,
  enabled                  BOOLEAN        NOT NULL,
  currency_code            VARCHAR(3)     NOT NULL,
  minor_currency_unit_digits INT          NOT NULL,
  created_on               TIMESTAMP(3)  NOT NULL,
  created_by               VARCHAR(32)    NOT NULL,
  last_modified_on         TIMESTAMP(3)  NULL,
  last_modified_by         VARCHAR(32)    NULL,
  CONSTRAINT bastet_product_pk PRIMARY KEY (id),
  CONSTRAINT bastet_product_uq UNIQUE (identifier)
);

CREATE TABLE bastet_p_acct_assigns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  designator               VARCHAR(32)    NOT NULL,
  identifier               VARCHAR(34)    NOT NULL,
  product_id               BIGINT         NOT NULL,
  thoth_type               VARCHAR(32)    NOT NULL,
  CONSTRAINT bastet_p_acct_assign_pk PRIMARY KEY (id),
  CONSTRAINT bastet_p_acct_assign_uq UNIQUE (product_id, designator),
  CONSTRAINT bastet_p_acct_assign_par_fk FOREIGN KEY (product_id) REFERENCES bastet_products (id)
);

CREATE TABLE bastet_p_task_defs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  identifier               VARCHAR(32)    NOT NULL,
  product_id               BIGINT         NOT NULL,
  a_name                   VARCHAR(256)   NOT NULL,
  description              VARCHAR(4096)  NOT NULL,
  actions                  VARCHAR(512)   NOT NULL,
  four_eyes                BOOLEAN        NOT NULL,
  mandatory                BOOLEAN        NOT NULL,
  CONSTRAINT bastet_p_task_def_pk PRIMARY KEY (id),
  CONSTRAINT bastet_p_task_def_uq UNIQUE (product_id, identifier),
  CONSTRAINT bastet_p_task_def_par_fk FOREIGN KEY (product_id) REFERENCES bastet_products (id)
);

CREATE TABLE bastet_p_chrg_defs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  identifier               VARCHAR(32)    NOT NULL,
  product_id               BIGINT         NOT NULL,
  a_name                   VARCHAR(256)   NOT NULL,
  description              VARCHAR(4096)  NOT NULL,
  accrue_action            VARCHAR(32)    NULL,
  charge_action            VARCHAR(32)    NOT NULL,
  amount                   DECIMAL(19,4)  NOT NULL,
  charge_method            VARCHAR(32)    NOT NULL,
  from_account_designator  VARCHAR(32)    NOT NULL,
  accru_account_designator VARCHAR(32)    NULL,
  to_account_designator    VARCHAR(32)    NOT NULL,
  for_cycle_size_unit      VARCHAR(32)    NULL,
  CONSTRAINT bastet_p_chrg_def_pk PRIMARY KEY (id),
  CONSTRAINT bastet_p_chrg_def_uq UNIQUE (product_id, identifier),
  CONSTRAINT bastet_p_chrg_def_par_fk FOREIGN KEY (product_id) REFERENCES bastet_products (id)
);

CREATE TABLE bastet_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  identifier               VARCHAR(32) NOT NULL,
  product_identifier       VARCHAR(32) NOT NULL,
  current_state            VARCHAR(32)   NOT NULL,
  created_on               TIMESTAMP(3)  NOT NULL,
  created_by               VARCHAR(32)    NOT NULL,
  last_modified_on         TIMESTAMP(3)  NULL,
  last_modified_by         VARCHAR(32)    NULL,
  CONSTRAINT bastet_case_pk PRIMARY KEY (id),
  CONSTRAINT bastet_case_uq UNIQUE (identifier, product_identifier)
);

CREATE TABLE bastet_c_acct_assigns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  designator               VARCHAR(32)    NOT NULL,
  identifier               VARCHAR(34)    NOT NULL,
  case_id                  BIGINT         NOT NULL,
  CONSTRAINT bastet_c_acct_assign_pk PRIMARY KEY (id),
  CONSTRAINT bastet_c_acct_assign_uq UNIQUE (case_id, designator),
  CONSTRAINT bastet_c_acct_assign_par_fk FOREIGN KEY (case_id) REFERENCES bastet_cases (id)
);

CREATE TABLE bastet_il_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id                  BIGINT         NOT NULL,
  customer_identifier      VARCHAR(32)    NOT NULL,
  term_range_temporal_unit VARCHAR(128)   NOT NULL,
  term_range_minimum       INT            NOT NULL,
  term_range_maximum       INT            NOT NULL,
  balance_range_maximum    DECIMAL(19,4)  NOT NULL,
  pay_cycle_temporal_unit  VARCHAR(128)   NOT NULL,
  pay_cycle_period         INT            NOT NULL,
  pay_cycle_align_day      INT            NULL,
  pay_cycle_align_week     INT            NULL,
  pay_cycle_align_month    INT            NULL,
  CONSTRAINT bastet_il_cases_pk PRIMARY KEY (id),
  CONSTRAINT bastet_il_cases_uq UNIQUE (case_id),
  CONSTRAINT bastet_il_cases_par_fk FOREIGN KEY (case_id) REFERENCES bastet_cases (id)
);

CREATE TABLE bastet_il_c_credit_facts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id                  BIGINT         NOT NULL,
  customer_identifier      VARCHAR(32)    NOT NULL,
  position_in_factor       INT            NOT NULL,
  position_in_customers    INT            NOT NULL,
  fact_type                VARCHAR(32)    NOT NULL,
  description              VARCHAR(4096)  NOT NULL,
  amount                   DECIMAL(19,4)  NOT NULL,
  CONSTRAINT bastet_il_c_credit_facts_pk PRIMARY KEY (id),
  CONSTRAINT bastet_il_c_credit_facts_uq UNIQUE (case_id, customer_identifier, position_in_factor, fact_type),
  CONSTRAINT bastet_il_c_credit_facts_par_fk FOREIGN KEY (case_id) REFERENCES bastet_il_cases (id)
);