A small university registration system:
- PostgreSQL schema, views (CTEs), and **INSTEAD OF** triggers on a `Registrations` view
- Java mini web server (JDBC) with endpoints: `/info`, `/reg`, `/unreg`
- Returns JSON matching the provided `information_schema.json`
