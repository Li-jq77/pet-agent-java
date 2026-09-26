# Deploy beside an existing Django site

The Django site continues to be served by Gunicorn. Spring Boot runs as a
separate systemd service because Gunicorn cannot start a Java application.

Final routing:

```text
https://lijunqi.cc/                 -> existing Django / Gunicorn
https://lijunqi.cc/static/          -> existing Django static files
https://lijunqi.cc/pet-agent-java/  -> Spring Boot on 127.0.0.1:8001
```

## 1. Build

```bash
mvn clean package -DskipTests
```

## 2. Upload

```bash
sudo mkdir -p /opt/pet-agent-java
sudo cp target/pet-agent-java-1.0.0.jar /opt/pet-agent-java/
sudo cp .env /opt/pet-agent-java/.env
sudo chown -R www-data:www-data /opt/pet-agent-java
sudo chmod 600 /opt/pet-agent-java/.env
```

The server `.env` must contain the real MySQL credentials and must never be
committed to Git.

## 3. Start Spring Boot

```bash
sudo cp deploy/pet-agent-java.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now pet-agent-java
sudo systemctl status pet-agent-java
```

Check locally:

```bash
curl -I http://127.0.0.1:8001/pet-agent-java/
```

## 4. Add the Nginx location

Copy the contents of `nginx-pet-agent-java.conf` into the existing
`lijunqi.cc` server block, before the general Django location.

Then:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Verify:

```bash
curl -I https://lijunqi.cc/
curl -I https://lijunqi.cc/pet-agent-java/
```

The `/pet-agent-java/` location does not replace or modify the existing
Django location.
