CREATE TABLE submission_metadata (
    form_id                VARCHAR(512) NOT NULL,
    form_version           VARCHAR(512) NOT NULL, -- Using '' for non-present versions to avoid problems with NULL values and unique constraints
    instance_id            VARCHAR(512) NOT NULL,
    submission_file        VARCHAR(512),
    submission_date_time   TIMESTAMP WITH TIME ZONE,
    encrypted_xml_filename VARCHAR(512),
    base_64_encrypted_key  VARCHAR(512),
    encrypted_signature    VARCHAR(512),
    attachment_filenames   VARCHAR(512) ARRAY[200],
    PRIMARY KEY (form_id, form_version, instance_id)
);

