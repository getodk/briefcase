CREATE TABLE form_metadata (
    form_id                       VARCHAR(255) NOT NULL,
    form_version                  VARCHAR(255) NOT NULL, -- Using '' for non-present versions to avoid problems with NULL values and unique constraints
    form_name                     VARCHAR(255),
    form_file                     VARCHAR(255),
    cursor_type                   VARCHAR(20),
    cursor_value                  CLOB,
    is_encrypted                  BOOLEAN,
    url_manifest                  VARCHAR(255),
    url_download                  VARCHAR(255),
    last_exported_submission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (form_id, form_version),
    CHECK (form_id != '')
);

