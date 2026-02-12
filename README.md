# Real-time Traffic Simulation (Java + SUMO)

A Java application that controls a SUMO traffic simulation in real time via TraCI/TraaS.
It provides a GUI to start/stop the simulation, inject vehicles, control traffic lights, and monitor basic metrics.
The project also includes logging and export functionality.

## Features
- Live SUMO connection (TraCI/TraaS)
- GUI for interaction (start/stop, controls)
- Vehicle injection
- Traffic light control
- Logging of simulation events
- Export of results (e.g., CSV/PDF if enabled in code)

## Requirements
- Java 17 (or the version configured in `pom.xml`)
- Maven
- SUMO installed and accessible from your system
- TraCI/TraaS dependencies (configured in `pom.xml`)

## How to Run
1. Install SUMO and make sure it runs from terminal (e.g., `sumo-gui` works).
2. Build the project:
   ```bash
   mvn clean package

