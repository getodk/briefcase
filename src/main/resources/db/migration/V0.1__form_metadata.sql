CREATE SEQUENCE form_metadata_id_seq;

CREATE TABLE form_metadata (
    id                            INTEGER NOT NULL PRIMARY KEY,
    form_name                     VARCHAR(255),
    form_id                       VARCHAR(255),
    form_version                  VARCHAR(255),
    form_file                     VARCHAR(255),
    cursor_type                   VARCHAR(20),
    cursor_value                  CLOB,
    last_exported_submission_date TIMESTAMP WITH TIME ZONE,
    CHECK ((form_name IS NOT NULL) OR (form_id IS NOT NULL))
);
