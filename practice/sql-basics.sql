CREATE SCHEMA practice;

CREATE TABLE practice.lot (
    id      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    address VARCHAR(200)
);

CREATE TABLE practice.section (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lot_id   INT NOT NULL REFERENCES practice.lot(id),
    code     VARCHAR(5) NOT NULL,
    capacity INT NOT NULL
);

CREATE TABLE practice.slot (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_id INT NOT NULL REFERENCES practice.section(id),
    code       VARCHAR(10) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    UNIQUE (section_id, code)
);
INSERT INTO practice.lot (name, address)
VALUES ('Main Campus Lot', 'Gate 1');

INSERT INTO practice.section (lot_id, code, capacity)
VALUES (1, 'A', 5), (1, 'B', 5);

INSERT INTO practice.slot (section_id, code, status) VALUES
(1, 'A1', 'AVAILABLE'), (1, 'A2', 'RESERVED'), (1, 'A3', 'AVAILABLE'),
(1, 'A4', 'OCCUPIED'),  (1, 'A5', 'MAINTENANCE'),
(2, 'B1', 'AVAILABLE'), (2, 'B2', 'AVAILABLE'), (2, 'B3', 'OCCUPIED'),
(2, 'B4', 'AVAILABLE'), (2, 'B5', 'RESERVED');
-- 1. Read everything
SELECT * FROM practice.slot;

-- 2. Filter
SELECT code FROM practice.slot WHERE status = 'AVAILABLE';

-- 3. JOIN: slot with its section code
SELECT sl.code AS slot, se.code AS section, sl.status
FROM practice.slot sl
JOIN practice.section se ON sl.section_id = se.id;

-- 4. JOIN across all three tables
SELECT l.name AS lot, se.code AS section, sl.code AS slot, sl.status
FROM practice.slot sl
JOIN practice.section se ON sl.section_id = se.id
JOIN practice.lot l ON se.lot_id = l.id
WHERE sl.status = 'AVAILABLE';

-- 5. GROUP BY: how many slots per status
SELECT status, COUNT(*) FROM practice.slot GROUP BY status;

-- 6. UPDATE: reserve a slot
UPDATE practice.slot SET status = 'RESERVED' WHERE code = 'A1';