# Industrial SNMP Reader

A JavaFX desktop application for real-time monitoring of industrial network devices (Siemens, MikroTik) via SNMP v1/v2c/v3. Tracks CPU load, temperature, uptime, and hostname with historical chart visualization and persistent SQLite storage.

---

## Features

| Feature | Description |
|---|---|
| **Multi-vendor support** | Pre-configured OIDs for Siemens, MikroTik, and Hirschmann; extensible for other vendors |
| **SNMP v1 / v2c / v3** | Full SNMPv3 USM support (AuthPriv: MD5/SHA + DES/AES) |
| **Real-time polling** | Auto-refresh every 10 seconds using a background thread pool |
| **Historical charts** | Interactive line charts (CPU %, temperature) with per-device or aggregate view |
| **Persistent storage** | SQLite database stored in the OS-appropriate app directory |
| **User authentication** | Login screen with salted SHA-256 password hashing |

---

## Prerequisites

- **JDK 21+** (project targets Java 25; JavaFX requires 21+)
- **Maven 3.8+** (or use the included `./mvnw` wrapper)

---

## Build & Run

```bash
# Compile
./mvnw clean compile

# Run from source
./mvnw javafx:run

# Run tests
./mvnw test

# Package fat JAR
./mvnw clean package
```

The packaged JAR is output to `target/IndustrialSNMPReader-1.0-SNAPSHOT.jar`.

```bash
java --enable-native-access=org.xerial.sqlitejdbc \
     -jar target/IndustrialSNMPReader-1.0-SNAPSHOT.jar
```

> The `--enable-native-access` flag is required for SQLite JDBC on some systems.

---

## Keyboard Shortcuts

All shortcuts use **⌘ Cmd** on macOS and **Ctrl** on Windows/Linux.

| Shortcut | Action |
|---|---|
| `⌘ R` | Refresh all devices |
| `⌘ N` | Add device |
| `⌘ ,` | Open / close SNMP settings for selected device |
| `⌘ G` | Toggle performance chart window |
| `⌘ ⌫` | Delete selected device |
| `⌘ O` | Log out |

---

## Testing

The project includes unit tests covering core logic (no JavaFX runtime required).

```bash
./mvnw test
```

### Test coverage

**`DatabaseManagerTest`** — integration tests against an in-memory temporary SQLite database:
- `addDevice` returns a valid generated ID
- `getAllDevices` returns previously inserted records
- `updateDeviceSnmpSettings` persists all SNMPv3 fields (version, security name, auth/priv protocols and passphrases)
- `deleteDevice` removes the record from the database
- `checkCredentials` validates the seeded admin account, rejects wrong passwords, unknown users, and null inputs

**`SNMPControllerTest`** — unit tests for OID resolution:
- Correct OIDs returned for Siemens, MikroTik, and Hirschmann
- Lookup is case-insensitive
- Unknown or null vendor returns `null`

---

## Architecture

```
IndustrialSNMPApplication   → JavaFX entry point
LoginController             → Authentication, async DB check
IndustrialSNMPController    → Main view: device table, polling, navigation
SNMPController              → SNMP4J polling logic, vendor OID registry
DatabaseManager             → SQLite CRUD (devices + users)
ChartController             → LineChart with per-device filter and tooltips
SNMPSettingsController      → Dialog for per-device SNMPv3 configuration
Device                      → Data model with JavaFX properties + history ring buffer
```

### Tech Stack

| Layer | Library |
|---|---|
| UI | JavaFX 21 |
| SNMP | SNMP4J |
| Database | SQLite JDBC (org.xerial) |
| Testing | JUnit 5 |
| Build | Maven + maven-shade-plugin |

---

## Database Location

The `devices.db` file is created automatically on first launch:

| OS | Path |
|---|---|
| macOS | `~/Library/Application Support/IndustrialSNMPReader/` |
| Linux | `~/.config/IndustrialSNMPReader/` |
| Windows | `%APPDATA%\IndustrialSNMPReader\` |

> If the default path is not writable, the application falls back to the user home directory.

---

## Supported Vendors & OIDs

Standard OIDs shared by all vendors:

| Metric | OID |
|---|---|
| Hostname | `1.3.6.1.2.1.1.5.0` (sysName) |
| Uptime | `1.3.6.1.2.1.1.3.0` (sysUpTime) |

### Siemens
MIB: Siemens private enterprise (`1.3.6.1.4.1.261`)

| Metric | OID |
|---|---|
| Temperature | `1.3.6.1.4.1.261.2.1.1.1.1.0` |
| CPU % | `1.3.6.1.4.1.261.2.1.1.3.1.0` |

### MikroTik
MIB: MikroTik private enterprise (`1.3.6.1.4.1.14988`)

| Metric | OID | Note |
|---|---|---|
| Temperature | `1.3.6.1.4.1.14988.1.1.3.11.0` | Raw value is tenths of °C (divided by 10) |
| CPU % | `1.3.6.1.2.1.25.3.3.1.2.2` | HOST-RESOURCES-MIB |

### Hirschmann
MIB: HMPRIV-MGMT-SNMP-MIB, Classic firmware only (`1.3.6.1.4.1.248.14.x`)

| Metric | OID |
|---|---|
| Temperature | `1.3.6.1.4.1.248.14.2.15.1.0` |
| CPU % | `1.3.6.1.4.1.248.14.2.14.1.0` |

> **HiOS firmware (e.g. GRS1042 on HiOS 9.x):** The OIDs above apply to Classic firmware only.
> HiOS uses a separate MIB set (`hm2DevMgmt`, subtree `.248.11.x`) with table-based temperature entries.
> To find the correct OIDs for your firmware version, download the HiOS MIB package from Hirschmann's
> website and walk the device tree:
> ```bash
> snmpwalk -v2c -c public <device-ip> 1.3.6.1.4.1.248.11
> ```

---

## Default Credentials

| Username | Password |
|---|---|
| `admin` | `admin123` |
