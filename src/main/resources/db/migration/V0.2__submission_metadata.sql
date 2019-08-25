CREATE TABLE submission_metadata (
    form_metadata_id      INTEGER      NOT NULL,
    instance_id           VARCHAR(100) NOT NULL,
    submission_file       VARCHAR(255),
    submission_date       TIMESTAMP WITH TIME ZONE,
    encrypted_xml_file    VARCHAR(255),
    base_64_encrypted_key CLOB,
    encrypted_signature   CLOB,
    attachment_filenames  VARCHAR(255) ARRAY[100],
    PRIMARY KEY (form_metadata_id, instance_id)
);

