CREATE TABLE ofPusher (
  username              VARCHAR(64)     NOT NULL,
  resource              VARCHAR(64)     NOT NULL,
  token                 VARCHAR(1024)   NOT NULL
);
CREATE INDEX ofPusher_idx ON ofPusher (username);

INSERT INTO ofVersion (name, version) VALUES ('pusher', 1);