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

CREATE TABLE bastet_il_c_docs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id                  BIGINT         NOT NULL,
  customer_identifier      VARCHAR(32)    NOT NULL,
  document_identifier      VARCHAR(32)    NOT NULL,
  list_order               INT            NOT NULL,

  CONSTRAINT bastet_il_c_docs_pk PRIMARY KEY (id),
  CONSTRAINT bastet_il_c_docs_uq UNIQUE (case_id, customer_identifier, document_identifier),
  CONSTRAINT bastet_il_c_docs_fk FOREIGN KEY (case_id) REFERENCES bastet_il_cases (id)
);