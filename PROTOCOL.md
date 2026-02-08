# Kelly KLS ETS Protocol

Reverse-engineered serial protocol used by Kelly KLS (KBLS) motor controllers. Based on the original ACAduserEnglish Android application.

## Physical Layer

| Parameter | Value |
|---|---|
| Baud rate | 19200 |
| Data bits | 8 |
| Stop bits | 1 |
| Parity | None |
| Interface | FT232 USB-UART, Bluetooth Classic (RFCOMM), BLE |

## Packet Format

All communication uses fixed-structure packets with a maximum size of **19 bytes**:

```
[CMD (1)] [LEN (1)] [DATA (0..16)] [CHECKSUM (1)]
```

| Field | Size | Description |
|---|---|---|
| CMD | 1 byte | Command code |
| LEN | 1 byte | Number of data bytes (0--16) |
| DATA | 0--16 bytes | Payload |
| CHECKSUM | 1 byte | `(CMD + LEN + DATA[0] + ... + DATA[n-1]) & 0xFF` |

For zero-data packets (LEN = 0) the checksum equals the command byte itself: `[CMD, 0x00, CMD]`.

## Checksum

Simple byte sum truncated to 8 bits:

```
checksum = (sum of all bytes before checksum) mod 256
```

Validation: the receiver recalculates the checksum over `CMD + LEN + DATA` and compares it to the received checksum byte. Mismatched packets are discarded.

## Command Table

### Flash & Configuration

| Command | Code | Direction | Description |
|---|---|---|---|
| FLASH_OPEN | `0xF1` | TX/RX | Open flash for read/write session |
| FLASH_READ | `0xF2` | TX/RX | Read 16-byte block from flash |
| FLASH_WRITE | `0xF3` | TX/RX | Write data block to flash |
| FLASH_CLOSE | `0xF4` | TX/RX | Commit (burn) flash and close session |
| ERASE_FLASH | `0xB1` | TX/RX | Erase flash memory |
| BURNT_FLASH | `0xB2` | TX/RX | Burn flash |
| BURNT_CHECKSUM | `0xB3` | TX/RX | Burn checksum |
| BURNT_RESET | `0xB4` | TX/RX | Burn and reset controller |
| FLASH_INFO_VERSION | `0xFA` | TX/RX | Read flash info version |

### Monitoring

| Command | Code | Direction | Description |
|---|---|---|---|
| CODE_VERSION | `0x11` | TX/RX | Read firmware version |
| MONITOR | `0x33` | TX/RX | Monitor (standard) |
| MONITOR1 | `0x34` | TX/RX | Monitor variant 1 |
| USER_MONITOR1 | `0x3A` | TX/RX | Monitor command 1 -- bytes 0..15 |
| USER_MONITOR2 | `0x3B` | TX/RX | Monitor command 2 -- bytes 16..31 |
| USER_MONITOR3 | `0x3C` | TX/RX | Monitor command 3 -- bytes 32..47 |

### Diagnostics & Identification

| Command | Code | Direction | Description |
|---|---|---|---|
| WATCHDOG_TEST | `0x10` | TX/RX | Watchdog/test |
| A2D_BATCH_READ | `0x1B` | TX/RX | Analog-to-digital batch read |
| GPIO_PORT_INPUT | `0x1E` | TX/RX | GPIO port input |
| GPIO_PIN_INPUT | `0x1F` | TX/RX | GPIO pin input |
| GET_PHASE_I_AD | `0x35` | TX/RX | Phase current A/D zero values |
| ENTRY_IDENTIFY | `0x43` | TX/RX | Enter motor identification mode |
| CHECK_IDENTIFY_STATUS | `0x44` | TX/RX | Check identification status |
| QUIT_IDENTIFY | `0x42` | TX/RX | Exit motor identification mode |
| GET_PMSM_PARM | `0x4B` | TX/RX | Get PMSM motor parameters |
| WRITE_PMSM_PARM | `0x4C` | TX/RX | Write PMSM motor parameters |
| GET_RESOLVER_INIT_ANGLE | `0x4D` | TX/RX | Get resolver initial angle |
| GET_HALL_SEQUENCE | `0x4E` | TX/RX | Get Hall sensor sequence |

## Flash Memory (512 Bytes)

The controller stores all calibration parameters in a 512-byte flash region. Reading and writing requires an open flash session.

### Session Lifecycle

```
1. FLASH_OPEN (0xF1)     -- open session
2. FLASH_READ (0xF2) x32  -- read all 512 bytes    (OR)
   FLASH_WRITE (0xF3) x40 -- write all 512 bytes
3. FLASH_CLOSE (0xF4)     -- commit (burn) and close
```

### Flash Read (512 Bytes)

32 sequential read commands, each returning 16 bytes:

```
Block 0:  addr=0x0000  ->  bytes 0..15
Block 1:  addr=0x0010  ->  bytes 16..31
...
Block 31: addr=0x01F0  ->  bytes 496..511
```

**TX packet** (DATA = 3 bytes):

```
[0xF2] [0x03] [addr_lo] [0x10] [addr_hi] [checksum]
```

| Field | Description |
|---|---|
| `addr_lo` | `address & 0xFF` |
| `0x10` | Block size (16) |
| `addr_hi` | `(address >> 8) & 0xFF` |

**RX packet** (DATA = 16 bytes):

```
[0xF2] [0x10] [data_0 .. data_15] [checksum]
```

### Flash Write (512 Bytes)

40 sequential write commands: 39 chunks of 13 bytes + 1 final chunk of 5 bytes:

```
Chunk 0:  addr=0    -> bytes 0..12     (13 bytes)
Chunk 1:  addr=13   -> bytes 13..25    (13 bytes)
...
Chunk 38: addr=494  -> bytes 494..506  (13 bytes)
Chunk 39: addr=507  -> bytes 507..511  (5 bytes)
```

**TX packet for chunks 0--38** (DATA = 16 bytes):

```
[0xF3] [0x10] [addr_lo] [0x0D] [addr_hi] [data_0 .. data_12] [checksum]
```

**TX packet for chunk 39** (DATA = 8 bytes):

```
[0xF3] [0x08] [0xFB] [0x05] [0x01] [data_507 .. data_511] [checksum]
```

| Field | Description |
|---|---|
| `addr_lo` | `address & 0xFF` |
| `0x0D` / `0x05` | Chunk size (13 or 5) |
| `addr_hi` | `(address >> 8) & 0xFF` |

### Flash Burn (Commit)

After writing, `FLASH_CLOSE` (`0xF4`) commits data to persistent flash. This operation can take up to **9 seconds** -- the controller may not respond immediately. The host should retry up to 30 times.

## Real-Time Monitoring

Three commands return 48 bytes of live data:

| Command | Code | Returns | Monitor bytes |
|---|---|---|---|
| USER_MONITOR1 | `0x3A` | 16 bytes | 0..15 |
| USER_MONITOR2 | `0x3B` | 16 bytes | 16..31 |
| USER_MONITOR3 | `0x3C` | 16 bytes | 32..47 |

Each TX packet is a zero-data command: `[CMD, 0x00, CMD]`.

Each RX packet returns 16 data bytes: `[CMD, 0x10, data_0..data_15, checksum]`.

### Monitor Data Map

| Offset | Size | Name | Range | Unit | Description |
|---|---|---|---|---|---|
| 0 | 1 | TPS Pedal | 0--255 | | Throttle A/D (0--255 = 0--5V) |
| 1 | 1 | Brake Pedal | 0--255 | | Brake A/D (0--255 = 0--5V) |
| 2 | 1 | Brake Switch | 0--2 | | Brake switch status |
| 3 | 1 | Foot Switch | 0--2 | | Throttle safety switch |
| 4 | 1 | Forward Switch | 0--2 | | Forward direction switch |
| 5 | 1 | Reverse Switch | 0--2 | | Reverse direction switch |
| 6 | 1 | Hall A | 0--2 | | Hall sensor A |
| 7 | 1 | Hall B | 0--2 | | Hall sensor B |
| 8 | 1 | Hall C | 0--2 | | Hall sensor C |
| 9 | 1 | B+ Voltage | 0--200 | V | Battery voltage |
| 10 | 1 | Motor Temp | 0--150 | C | Motor temperature |
| 11 | 1 | Controller Temp | 0--150 | C | Controller temperature |
| 12 | 1 | Set Direction | 0--2 | | Direction setting (0=fwd) |
| 13 | 1 | Actual Direction | 0--2 | | Current direction (0=fwd) |
| 14 | 1 | Brake Switch 2 | 0--2 | | Secondary brake switch |
| 15 | 1 | Low Speed | 0--2 | | Low speed mode |
| 16--17 | 2 | Error Status | 0--65535 | | 16-bit error bitmask (hex) |
| 18--19 | 2 | Motor Speed | 0--10000 | RPM | Motor speed (big-endian) |
| 20--21 | 2 | Phase Current | 0--800 | A | Phase current RMS (big-endian) |

Multi-byte values (WORD) are stored **big-endian**: `value = byte[offset] * 256 + byte[offset + 1]`.

## Error Codes

The Error Status field (offset 16--17) is a 16-bit bitmask:

| Bit | Mask | Name |
|---|---|---|
| 0 | `0x0001` | Identify Error |
| 1 | `0x0002` | Over Voltage |
| 2 | `0x0004` | Low Voltage |
| 3 | `0x0008` | (Reserved) |
| 4 | `0x0010` | Locking |
| 5 | `0x0020` | V+ Error |
| 6 | `0x0040` | Over Temperature |
| 7 | `0x0080` | High Pedal |
| 8 | `0x0100` | (Reserved) |
| 9 | `0x0200` | Reset Error |
| 10 | `0x0400` | Pedal Error |
| 11 | `0x0800` | Hall Sensor Error |
| 12 | `0x1000` | (Reserved) |
| 13 | `0x2000` | Emergency Reverse Error |
| 14 | `0x4000` | Motor Over Temp Error |
| 15 | `0x8000` | Current Meter Error |

Multiple errors can be active simultaneously. Example: `0x0006` = Over Voltage + Low Voltage.

## Controller Detection

On connection the host reads flash and checks bytes 0--7 (module name, ASCII) and bytes 16--17 (firmware version, big-endian uint16):

```
Module name substring [1..4]:
  "BLS", "BSS", or [1..3] == "LS"  ->  KLS series (supported)
  anything else                     ->  unsupported

Firmware version:
  >= 265  ->  KBLS_0109 (91 calibration parameters)
  262-264 ->  KBLS_0106 (69 calibration parameters)
  < 262   ->  unsupported
```

## Communication Pattern

The host follows a strict drain-send-receive pattern for reliability:

```
1. Drain    -- discard any stale bytes in the receive buffer
2. Send     -- transmit TX packet
3. Receive  -- wait for RX packet (timeout: 100ms BLE, 300ms BT Classic)
4. Validate -- check command echo + checksum
5. Retry    -- on failure, repeat from step 1
```

Retry counts vary by operation:

| Operation | Max retries | Notes |
|---|---|---|
| Open flash | 2 | |
| Read flash (per block) | 2 | 32 blocks total |
| Write flash (per chunk) | 3 | 40 chunks total |
| Burn flash | 30 | Controller may take up to 9 seconds |
| Read monitor | 1 | Polling loop handles failures |
| Read version | 2 | |
| Read phase current A/D | 2 | |

## Calibration Parameters

Parameters are encoded in the 512-byte flash region using four data types:

| Type | Code | Encoding |
|---|---|---|
| UNSIGNED | `uo` | Unsigned integer (bit, byte, or multi-byte big-endian) |
| SIGNED | `so` | Signed byte (-128..127) |
| HEX | `h` | Hexadecimal string (2 chars per byte) |
| ASCII | `a` | ASCII characters |

And three size classes:

| Size | Encoding |
|---|---|
| BIT | Single bit at a given position within a byte. Read: `(byte / 2^pos) & 1`. Write: OR/AND mask. |
| BYTE | Single byte at offset |
| WORD | Multi-byte big-endian. `position` field = number of bytes - 1 (e.g. position=1 means 2 bytes) |

### Notable Parameters (KBLS_0109)

| Offset | Size | Name | Range | Category |
|---|---|---|---|---|
| 0 | 8 bytes ASCII | Module Name | -- | General |
| 16--17 | WORD | Software Version | -- | General |
| 18 | BYTE | Under Voltage | 0--255 | Protection |
| 19 | BYTE | Over Voltage | 0--255 | Protection |
| 24 | BYTE | Controller Over Temp | 0--150 | Protection |
| 25 | BYTE | Motor Over Temp | 0--150 | Protection |
| 32 | BYTE | Throttle Dead Zone High | 0--255 | Throttle |
| 33 | BYTE | Throttle Dead Zone Low | 0--255 | Throttle |
| 107--108 | WORD | Max Speed | 0--10000 | Speed |
| 268 | BYTE | Motor Poles | 1--100 | Motor |
| 343--352 | various | Hall Angles | -- | Motor |

Full definitions: 69 parameters for KBLS_0106, 91 for KBLS_0109. See `ParameterDefinitions.kt` for complete list with ranges, safety levels, and descriptions.

## Voltage Range Lookup

Controllers encode their voltage class as a code. Known mappings:

| Code | Min (V) | Max (V) |
|---|---|---|
| 24 | 8 | 35 |
| 36 | 18 | 45 |
| 48 | 18 | 62 |
| 60 | 18 | 80 |
| 72 | 18 | 90 |
| 84 | 18 | 105 |
| 96 | 18 | 120 |
| 11 | 18 | 132 |
| 12 | 18 | 136 |
| 14 | 18 | 180 |
| 16 | 18 | 200 |
| 32 | 18 | 380 |

Special code **80**: `max = controllerVoltage * 125 / 100`.

## Timing Reference

| Phase | Typical duration |
|---|---|
| Single TX/RX round-trip (BLE) | 10--50 ms |
| Single TX/RX round-trip (BT Classic) | 5--20 ms |
| Full flash read (32 blocks) | ~0.5--1.5 s |
| Full flash write (40 chunks) | ~1--3 s |
| Flash burn (commit) | up to 9 s |
| Monitor cycle (3 commands) | ~15--100 ms |
