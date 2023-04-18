CREATE DATABASE testdb;
CREATE ROLE dbuser WITH LOGIN ENCRYPTED PASSWORD 'dbpwd';
CREATE DATABASE tclstairway;
CREATE ROLE tclstairwayuser WITH LOGIN ENCRYPTED PASSWORD 'tclstairwaypwd';
GRANT ALL ON DATABASE tclstairway TO tclstairwayuser WITH GRANT OPTION;
\c tclstairway
GRANT ALL ON SCHEMA public TO tclstairwayuser WITH GRANT OPTION;
