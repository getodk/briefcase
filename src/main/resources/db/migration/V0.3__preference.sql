CREATE TABLE preference (
    category VARCHAR(128),
    name     VARCHAR(256),
    value    LONGVARCHAR,
    PRIMARY KEY (category, name, value)
);

INSERT INTO preference VALUES ('', 'Tracking consent', 'true');
