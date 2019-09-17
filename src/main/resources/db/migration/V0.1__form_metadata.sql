CREATE TABLE form_metadata (
    form_id                            VARCHAR(512) NOT NULL,
    form_version                       VARCHAR(512) NOT NULL, -- Using '' for non-present versions to avoid problems with NULL values and unique constraints
    form_name                          VARCHAR(512),
    form_file                          VARCHAR(512),
    cursor_type                        VARCHAR(128),
    cursor_value                       LONGVARCHAR,
    is_encrypted                       BOOLEAN,
    url_manifest                       VARCHAR(512),
    url_download                       VARCHAR(512),
    last_exported_date_time            TIMESTAMP WITH TIME ZONE,
    last_exported_submission_date_time TIMESTAMP WITH TIME ZONE,
    pull_source                        LONGVARCHAR,
    PRIMARY KEY (form_id, form_version),
    CHECK (form_id != '')
);

