CREATE TABLE ofPusher (
  username    VARCHAR(64)             NOT NULL,
  resource    VARCHAR(64)             NOT NULL,
  token       VARCHAR(1024)           NOT NULL,
  type        ENUM('ios', 'android')  NOT NULL,
  PRIMARY KEY (username, resource)
);

INSERT INTO ofVersion (name, version) VALUES ('pusher', 1);
