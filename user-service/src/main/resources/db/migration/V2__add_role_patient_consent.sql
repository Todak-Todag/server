ALTER TABLE user_schema.p_users DROP CONSTRAINT ck_p_users_role;

ALTER TABLE user_schema.p_users
    ADD CONSTRAINT ck_p_users_role CHECK (role IN (
      'PATIENT',
      'PATIENT_CONSENT',
      'HOSPITAL_STAFF',
      'SOCIAL_WORKER',
      'SERVICE_PROVIDER',
      'ADMIN',
      'MASTER'
    ));