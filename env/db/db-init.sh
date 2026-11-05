#!/bin/bash
# /docker-entrypoint-initdb.d script for postgres: database, user and extensions as in sormas-base/setup/server-setup.sh.
# SORMAS_DB_PW comes from the container env (a throwaway generated per copy by up.sh).
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d postgres <<EOSQL
CREATE USER sormas_user WITH PASSWORD '${SORMAS_DB_PW}' CREATEDB;
CREATE DATABASE sormas_db WITH OWNER = 'sormas_user' ENCODING = 'UTF8';
\c sormas_db
CREATE OR REPLACE PROCEDURAL LANGUAGE plpgsql;
ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO sormas_user;
CREATE EXTENSION pg_trgm;
CREATE EXTENSION pgcrypto;
CREATE EXTENSION IF NOT EXISTS unaccent;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO sormas_user;
EOSQL
