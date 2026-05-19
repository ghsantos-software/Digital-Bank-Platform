-- Each microservice owns its own database (database-per-service pattern)
CREATE DATABASE userdb;
CREATE DATABASE accountdb;
CREATE DATABASE transactiondb;
-- Keycloak persistent storage
CREATE DATABASE keycloakdb;
