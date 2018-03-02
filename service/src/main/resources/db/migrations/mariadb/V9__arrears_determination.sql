--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

CREATE TABLE bastet_case_commands (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id                  BIGINT         NOT NULL,
  action_name              VARCHAR(32)    NOT NULL,
  created_on               TIMESTAMP(3)   NOT NULL,
  created_by               VARCHAR(32)    NOT NULL,
  thoth_transaction_uq     VARCHAR(26)  NOT NULL,

  CONSTRAINT bastet_case_commands_pk PRIMARY KEY (id),
  CONSTRAINT bastet_case_commands_uq UNIQUE (thoth_transaction_uq, action_name, case_id),
  CONSTRAINT bastet_case_commands_fk FOREIGN KEY (case_id) REFERENCES bastet_cases (id)
);

CREATE TABLE bastet_p_arrears_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id               BIGINT         NOT NULL,
  days_late                INT            NOT NULL,
  percent_provision        DECIMAL(5,2)   NOT NULL,

  CONSTRAINT bastet_p_arrears_config_pk PRIMARY KEY (id),
  CONSTRAINT bastet_p_arrears_config_uq UNIQUE (product_id, days_late),
  CONSTRAINT bastet_p_arrears_config_fk FOREIGN KEY (product_id) REFERENCES bastet_products (id)
);