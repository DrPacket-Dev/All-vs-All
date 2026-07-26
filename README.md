# All-vs-All

Ein Paper-Plugin für All-vs-All-Events mit Host-Rechten, Kit-Editor, SQLite-Speicherung und einer einfachen Settings-UI.

## Funktionen

- Host-Rechte über /host
- Settings-Menü über /settings
- Kit-Editor und Kit-Auswahl über /kit
- Border-Steuerung über /border oder /b
- Konfigurierbarer Chat-Prefix über die Config
- Kits werden in SQLite gespeichert
- Scoreboard für den Server-Event-Status

## Voraussetzungen

- Java 21
- Paper oder ein kompatibler Paper-Server
- Maven

## Installation

1. Baue das Plugin mit Maven:
   ```bash
   mvn clean package
   ```
2. Kopiere die generierte Datei aus dem Ordner target in den Plugins-Ordner deines Paper-Servers.
3. Starte den Server neu.

## Befehle

- /host - Vergibt Host-Rechte an den Spieler
- /settings - Öffnet das Settings-Menü
- /kit - Öffnet den Kit-Editor oder die Kit-Auswahl
- /border <size|add|subtract> - Verändert die Border
- /b <size|add|subtract> - Kurzform für /border

## Konfiguration

Die Datei config.yml wird automatisch erstellt und kann angepasst werden.

```yaml
database:
  type: sqlite # sqlite, mysql, mariadb
  path: kits.db
  host: 127.0.0.1
  port: 3306
  name: allvsall
  username: root
  password: ""

messages:
  prefix: "[Packet Serv]"
  welcome: "Welcome to the Packet Community Server."
  host: "You are now the host of the event."
  host_help: "Use /settings to configure the match and /kit to build kits."
  no_host: "You need host rights to use this feature."
scoreboard:
  enabled: true
  title: "Packet Community Server"
```

- `database.type`: Wähle aus `sqlite`, `mysql` oder `mariadb`.
- `database.path`: Pfad zur SQLite-Datei. Wird nur bei SQLite verwendet.
- `database.host`: Hostname oder IP der MySQL/MariaDB-Datenbank.
- `database.port`: Port der Datenbank.
- `database.name`: Name der Datenbank.
- `database.username`: Benutzername für die Datenbankverbindung.
- `database.password`: Passwort für die Datenbankverbindung.

## Entwicklung

- Tests ausführen:
  ```bash
  mvn test
  ```
- Das Plugin nutzt Maven und Paper-API.

## Hinweis

Das Plugin ist als Basis für ein All-vs-All-Event gedacht und kann weiter ausgebaut werden, zum Beispiel mit echter World-Border-Logik, erweiterten Kit-Kategorien oder einer besseren GUI.