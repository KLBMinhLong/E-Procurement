CREATE SEQUENCE pr.pr_number_sequence START WITH 1 INCREMENT BY 1;

COMMENT ON SEQUENCE pr.pr_number_sequence IS 'Sequence used to generate Purchase Request numbers PR-YYYY-MM-XXXXX.';
