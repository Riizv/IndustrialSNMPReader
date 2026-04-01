# Industrial SNMP Reader

A modern JavaFX application designed for monitoring industrial network devices (e.g., Siemens, MikroTik) using the SNMP protocol (v1, v2c, and v3). The application provides real-time monitoring of CPU usage, temperature, uptime, and hostname, with historical data visualization through interactive charts.

## Use Cases

- **Industrial Monitoring**: Real-time tracking of hardware health (CPU/Temp) for network infrastructure.
- **Multi-Vendor Support**: Pre-configured OIDs for Siemens and MikroTik devices, with an extensible architecture for other vendors.
- **Secure SNMP v3 Management**: Support for modern security standards (USM) including AuthPriv (MD5/SHA, DES/AES).
- **Performance Analysis**: Interactive line charts to analyze trends in CPU and temperature over time.
- **Local Database**: Persistent storage of device configurations using SQLite, ensuring settings are preserved across sessions.

## Prerequisites

- **Java Development Kit (JDK) 21 or newer** (Project target is Java 25, but 21+ is required for JavaFX compatibility).
- **Maven 3.8+** (or use the provided `./mvnw` wrapper).

## Build Instructions

To compile the project and download dependencies:

```bash
./mvnw clean compile
```

To run the application directly from the source code:

```bash
./mvnw javafx:run
```

## Packaging

The project is configured with `maven-shade-plugin` to create an executable "Fat JAR" containing all dependencies.

### Create Executable JAR

Run the following command:

```bash
./mvnw clean package
```

The resulting JAR will be located in the `target/` directory:
`target/IndustrialSNMPReader-1.0-SNAPSHOT.jar`

### Running the JAR

You can run the application using the standard `java -jar` command. Note that for SQLite support on some systems, you may need to enable native access:

```bash
java --enable-native-access=org.xerial.sqlitejdbc -jar target/IndustrialSNMPReader-1.0-SNAPSHOT.jar
```

## Architecture & Tech Stack

- **JavaFX 21**: Modern UI framework with CSS styling.
- **SNMP4J**: Enterprise-grade SNMP library.
- **SQLite JDBC**: Lightweight, file-based database.
- **Maven**: Dependency management and build automation.
- **GitHub Style CSS**: Custom stylesheet for a clean, professional look.

## Configuration

- **Database**: The application automatically creates a `devices.db` file in the application directory or user home folder, depending on OS permissions.
- **SNMP Settings**: Accessible via the "Ustawienia SNMP" button for each device to configure versions (v1/v2c/v3) and security credentials.
