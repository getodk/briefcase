CREATE TABLE preference (
    category VARCHAR(128),
    name     VARCHAR(256),
    value    LONGVARCHAR,
    PRIMARY KEY (category, name, value)
);

INSERT INTO preference VALUES ('global', 'tracking consent', 'true');

ALTER TABLE form_metadata
    ADD COLUMN pull_source_type VARCHAR(128);

ALTER TABLE form_metadata
    ADD COLUMN pull_source_value LONGVARCHAR;
