CREATE TABLE submission_metadata (
    form_id                VARCHAR(255) NOT NULL,
    form_version           VARCHAR(255) NOT NULL, -- Using '' for non-present versions to avoid problems with NULL values and unique constraints
    instance_id            VARCHAR(100) NOT NULL,
    submission_file        VARCHAR(255),
    submission_date_time   TIMESTAMP WITH TIME ZONE,
    encrypted_xml_filename VARCHAR(255),
    base_64_encrypted_key  CLOB,
    encrypted_signature    CLOB,
    attachment_filenames   VARCHAR(255) ARRAY[100],
    PRIMARY KEY (form_id, form_version, instance_id)
);

