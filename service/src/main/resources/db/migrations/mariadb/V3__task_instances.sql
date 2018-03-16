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

CREATE TABLE bastet_c_task_insts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id                  BIGINT         NOT NULL,
  task_def_id              BIGINT         NOT NULL,
  a_comment                VARCHAR(4096)  NOT NULL,
  executed_on              TIMESTAMP(3)   NULL,
  executed_by              VARCHAR(32)    NULL,
  CONSTRAINT bastet_c_task_inst_pk PRIMARY KEY (id),
  CONSTRAINT bastet_c_task_inst_uq UNIQUE (case_id, task_def_id),
  CONSTRAINT bastet_c_task_inst_case_fk FOREIGN KEY (case_id) REFERENCES bastet_cases (id),
  CONSTRAINT bastet_c_task_inst_def_fk FOREIGN KEY (task_def_id) REFERENCES bastet_p_task_defs (id)
);