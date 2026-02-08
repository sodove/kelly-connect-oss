# Kelly Connect

[![Build](https://github.com/sodove/kelly-connect/actions/workflows/build.yml/badge.svg)](https://github.com/sodove/kelly-connect/actions/workflows/build.yml)

Kotlin Multiplatform application for configuring **Kelly KLS motor controllers** and monitoring **BMS battery packs**. Runs on Android and Desktop (JVM).

Communicates with the controller over the ETS serial protocol via USB (FT232), Bluetooth Classic, or BLE. Connects to popular BMS systems (JK, JBD, ANT, Daly) over BLE for real-time cell-level battery monitoring.

> For detailed protocol documentation see [PROTOCOL.md](PROTOCOL.md).

## Features

### Dashboard
- VESC Tool-inspired gauge cluster with animated needles
- Speed (RPM / km/h / mph), phase current, throttle position, battery voltage
- Controller and motor temperature gauges with color-coded warnings
- BMS power and SOC gauges when battery is connected
- Precise BMS voltage replaces controller's rounded B+ reading
- Bottom info panel: error status, Hall sensor dots, direction, switch states
- Adaptive layout for landscape and portrait orientations

### Calibration
- Read and write all controller parameters from flash memory (512 bytes)
- 69 parameters (KBLS_0106, firmware v262-264) or 91 parameters (KBLS_0109, firmware v265+)
- 8 categories: General, Protection, Throttle, Braking, Speed, Motor, PID Tuning, Advanced
- Safety levels per parameter (Safe / Caution / Dangerous / Read-Only)
- Parameter descriptions and tips

### Monitor
- 19 live values updated in real time: voltage, current, speed, temperatures, switches, Hall sensors
- 16-bit error code decoding with human-readable fault names
- Communication error detection

### BMS
- Cell voltage grid with min/max/delta and per-cell health color bars
- Summary: voltage, current, power, SOC
- Energy: charge/capacity (Ah), cycle count, charge/discharge switch status
- Temperature readings with color-coded thresholds

### Settings
- Speed unit selection (RPM, km/h, mph) with wheel diameter and gear ratio
- Configurable gauge ranges (max speed, max current)
- BMS type selection, device scanning, and connection management

## Supported Hardware

### Controllers
| Model | Firmware | Parameters |
|---|---|---|
| Kelly KBLS (KLS) 0106 | v262 -- v264 | 69 |
| Kelly KBLS (KLS) 0109 | v265+ | 91 |

### Transports
| Transport | Android | Desktop |
|---|---|---|
| USB Serial (FT232) | Yes | Yes |
| Bluetooth Classic (RFCOMM) | Yes | -- |
| BLE (Kable) | Yes | -- |

### BMS (over BLE)
- **JK BMS** -- streaming protocol (0 ms poll interval)
- **JBD / Xiaoxiang BMS** -- command 0x03 + 0x04 polling
- **ANT BMS** -- status polling with CRC-16/MODBUS
- **Daly BMS** -- multi-command polling (voltage, status, cells, temps)

## Tech Stack

| Component | Library | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.1.0 |
| UI | Compose Multiplatform | 1.7.3 |
| Navigation | Decompose | 3.2.2 |
| DI | Koin | 4.0.0 |
| BLE | Kable | 0.33.0 |
| Serial (Desktop) | jSerialComm | 2.11.0 |
| Serial (Android) | usb-serial-for-android | 3.8.1 |
| Coroutines | kotlinx-coroutines | 1.9.0 |

## Project Structure

```
kelly-connect/
  protocol/              # KMP library -- ETS protocol, flash ops, parameter definitions
  composeApp/
    src/
      commonMain/        # Shared UI, domain, data layers
        kotlin/com/kelly/
          app/
            bms/         # BMS protocol implementations (JK, JBD, ANT, Daly)
            data/        # Repositories, transport abstractions
            domain/      # Models, repository interfaces, settings
            presentation/
              bms/       # BMS detail screen
              calibration/
              common/    # Theme, status bar, keep-screen-on
              connection/
              dashboard/ # Gauge cluster
              monitor/
              settings/
          protocol/      # (in protocol module)
      androidMain/       # Android transports (USB, BT Classic, BLE)
      desktopMain/       # Desktop transports (Serial, BLE stub)
```

## Building

Requires JDK 11+.

```bash
# Desktop
./gradlew :composeApp:run

# Android APK
./gradlew :composeApp:assembleDebug
```

## ETS Protocol

The controller communicates using 19-byte packets:

```
[CMD (1)] [LEN (1)] [DATA (0-16)] [CHECKSUM (1)]
```

- **Flash read**: 32 commands x 16 bytes = 512 bytes
- **Flash write**: 39 commands x 13 bytes + 1 x 5 bytes = 512 bytes
- **Monitor**: 3 commands (0x3A, 0x3B, 0x3C) returning 48 bytes total
- **Baud rate**: 19200, 8N1

## License

Apache 2.0 with [Commons Clause](https://commonsclause.com/). You may use, modify, and redistribute this software freely, but you may not sell it or offer it as a paid service. See [LICENSE](LICENSE) for details.
