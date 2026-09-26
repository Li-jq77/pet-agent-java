# Pet Agent Java

Spring Boot 2.7 + Java 8 rewrite of the existing Django pet management site.
The HTML structure, CSS, JavaScript, image assets and database table names are
kept compatible with the original project.

## Runtime

- Java 8
- MySQL 8
- Maven 3.6+

## Database

The application uses the `pet_agent_java` database. It does not write to
`pet_agent_db`.

For the existing local database, no action is needed. For a fresh database:

```powershell
mysql -u pet_agent -p -e "CREATE DATABASE pet_agent_java CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u pet_agent -p pet_agent_java < ..\pet_agent_db.sql
mysql -u pet_agent -p pet_agent_java < db\migrate_backup_to_current.sql
```

## Configuration

Copy `.env.example` to `.env` and fill in the MySQL credentials. The project
also reads `../ljq-pet-main/.env` when running beside the original project.

## Run

```powershell
mvn spring-boot:run
```

Open `http://127.0.0.1:8000/pet-agent-java/`.

Existing Django users remain valid because the application verifies Django
`pbkdf2_sha256` password hashes directly.

## Subdirectory deployment

The default context path is `/pet-agent-java`, so the Java site can coexist
with the Django site:

```text
https://lijunqi.cc/                 Django
https://lijunqi.cc/pet-agent-java/  Spring Boot
```

Spring Boot is a Java process and cannot be started by Gunicorn. Run it as a
separate systemd service and proxy `/pet-agent-java/` to it from Nginx. See
`deploy/README.md`, `deploy/pet-agent-java.service`, and
`deploy/nginx-pet-agent-java.conf`.
