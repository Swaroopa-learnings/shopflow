-- Creates one logical database per service ("database per service" pattern).
-- Services must NEVER reach into each other's tables - all cross-service
-- communication happens over REST (sync) or Kafka (async).
--
-- Wired into the postgres container via the compose `configs` mechanism
-- (see docker-compose.yml); runs automatically on first startup only
-- (Postgres executes /docker-entrypoint-initdb.d/* when the data dir is empty).
CREATE DATABASE authdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
CREATE DATABASE paymentdb;
