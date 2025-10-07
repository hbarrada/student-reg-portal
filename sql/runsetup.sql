-- This file is used to run your whole database setup. It will:
-- * Delete the whole database (!)
-- * Run files that create tables, insert data, create views,
--   and later create triggers.
-- * Do any additional stuff you need, like testing views or triggers
-- You can and should modify this file as you progress in the assignment.

-- To connect to the database and manually run things
-- psql -v ON_ERROR_STOP=1 -U postgres portal

-- Alternative, command to run this file (modify this if needed):
-- psql -f runsetup.sql


-- This script deletes everything in your database
-- this is the only part of the script you do not need to understand
\set QUIET true
SET client_min_messages TO WARNING; -- Less talk please.
-- This script deletes everything in your database
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO CURRENT_USER;
-- This line makes psql stop on the first error it encounters
-- You may want to remove this when running tests that are intended to fail
\set ON_ERROR_STOP ON
-- Alternative: makes psql continue even if there are errors 
-- \set ON_ERROR_STOP OFF
SET client_min_messages TO NOTICE; -- More talk
\set QUIET false

\ir tables.sql
\ir inserts.sql
\ir views.sql
\ir triggers.sql
\ir tests.sql
