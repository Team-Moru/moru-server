DELETE re1 FROM routine_execution re1
INNER JOIN routine_execution re2
    ON re1.routine_id = re2.routine_id
   AND re1.executed_date = re2.executed_date
   AND re1.id < re2.id;

ALTER TABLE routine_execution
    ADD CONSTRAINT uk_routine_execution_routine_date
        UNIQUE (routine_id, executed_date);