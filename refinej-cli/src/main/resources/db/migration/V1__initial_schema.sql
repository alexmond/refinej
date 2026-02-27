CREATE TABLE symbol (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind           VARCHAR(30)   NOT NULL,
    simple_name    VARCHAR(255),
    qualified_name VARCHAR(1000) NOT NULL UNIQUE,
    file_path      VARCHAR(1000),
    line_start     INT,
    line_end       INT,
    type_fqn       VARCHAR(1000)
);
CREATE INDEX idx_symbol_qualified_name ON symbol(qualified_name);
CREATE INDEX idx_symbol_kind           ON symbol(kind);

CREATE TABLE reference (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol_id  BIGINT       NOT NULL REFERENCES symbol(id) ON DELETE CASCADE,
    file_path  VARCHAR(1000) NOT NULL,
    line       INT,
    col        INT,
    usage_kind VARCHAR(30)  NOT NULL
);
CREATE INDEX idx_reference_symbol_id ON reference(symbol_id);
CREATE INDEX idx_reference_file_path ON reference(file_path);
