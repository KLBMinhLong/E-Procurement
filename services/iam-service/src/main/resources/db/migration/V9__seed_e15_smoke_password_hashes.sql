CREATE SCHEMA IF NOT EXISTS iam;

-- Keep local/dev E15 smoke actors aligned with the documented IAM password strategy.
-- Hashes are BCrypt(rawPassword + userId), cost 12. Raw passwords are not stored.

UPDATE iam.users u
SET password_hash = seed.password_hash,
    updated_at = NOW(),
    updated_by = '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('requester', '$2a$12$FRezmpyV49x7f7xhSkfgReolZSV90fIUz9ueshxdoYNG4n2lKxB3u'),
        ('manager', '$2a$12$bNHMjE5u6C2aeBWEjDCx3.R6mBO6X9CRD4MxQRnGsbQbw6ASG4ngy'),
        ('purchasing', '$2a$12$DPIEnVFEV8QFYXVdUwsWZuSP.3XY/A9gtZgI.FU1JRbwXwr.aytJS'),
        ('warehouse', '$2a$12$KPVqDoB0f0UyeZMS6uXAV.lS/bAHwhNzV9UZCP6eAeXP4HDUtarQa'),
        ('accountant', '$2a$12$jKoSiOG24IvqPJ5EFb/6QOz6eIG7Pw/2YPkAgUw4.BI.leUf4seU.'),
        ('superadmin', '$2a$12$D1WkJX4IIe3C3pptoDT7Au2JI2vi3pHBRxVCJyPQWvJEuY/ClM.Ra')
) AS seed(username, password_hash)
WHERE u.is_deleted = FALSE
  AND UPPER(u.username) = UPPER(seed.username);
