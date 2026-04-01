# ITI0301-2024
## Orb it

### Game Description

"Orb it" is an extraction shooter game where the core objective is to collect orbs and weapons scattered across the map. Players must fight against other players and NPCs to survive and successfully extract with their loot. The game features a dynamic multiplayer environment where strategy, combat skills, and resource management are key to success.

### How to Play

#### Controls:

- **W, A, S, D** - Move character
- **Mouse** - Aim
- **Left mouse button** - Shoot

#### Gameplay Instructions:

1. **Join the Game**: Launch the client and wait for connection to the server.
2. **Register/Log In**: Create an account or log in to an existing one to start playing.
3. **Create/Join Lobby**: Create a new game lobby or join an existing one to start a match.
2. **Spawn In**: You will spawn at a random location on the map.
3. **Collect Resources**: Explore the map to find orbs and weapons.
4. **Combat**: Fight against NPCs and other players to survive.
5. **Extract**: Make your way to an extraction point to secure your loot and win.
6. **Survive**: Player that extracted gets loot and XP.

#### Running the Game Locally

This project is a **monorepo** containing both the Server and Client components. Follow these step-by-step instructions to run the game on your local machine.

#### Connection Configuration

**The game is configured by default to connect to the TalTech Remote Server.**

To switch between **LOCAL** and **REMOTE** servers, edit `NetworkConfig.java` and comment/uncomment the appropriate lines:

**File:** `core/src/main/java/taltech/orbitgame/network/NetworkConfig.java`

```java
// ===== SWITCH SERVER CONFIGURATION HERE =====
// Uncomment ONE of the following pairs:

// === REMOTE SERVER (TalTech) - DEFAULT ===
private static final String DEFAULT_HOST = "193.40.157.153";
private static final int DEFAULT_PORT = 8080;

// === LOCAL SERVER - Uncomment to use locally ===
// private static final String DEFAULT_HOST = "localhost";
// private static final int DEFAULT_PORT = 54555;

// ============================================
```

**To use Local Server:**
1. Comment out the REMOTE SERVER lines:
   ```java
   // private static final String DEFAULT_HOST = "193.40.157.153";
   // private static final int DEFAULT_PORT = 8080;
   ```

2. Uncomment the LOCAL SERVER lines:
   ```java
   private static final String DEFAULT_HOST = "localhost";
   private static final int DEFAULT_PORT = 54555;
   ```

3. Rebuild and run the project

**To return to Remote Server:**
1. Uncomment the REMOTE SERVER lines
2. Comment out the LOCAL SERVER lines
3. Rebuild and run the project

#### Prerequisites:
- **Java 21** (JDK 21 or higher)
- **Git** (to clone the repository)

### Step-by-Step Installation Guide:

#### 1. Clone the Repository:
```bash
git clone <https://gitlab.cs.ttu.ee/alekmu/iti0301-2026>
```
If cloning to IntelliJ IDEA:
```bash
cd IdeaProjects/iti0301-2026 (replace with your actual path if different)
```

#### 2. Start the Server (For Local Play only):
If you are running the game locally, start your own server instance.

Windows PowerShell:
```powershell
.\scripts\run-server.ps1
```

Linux/macOS:
```bash
chmod +x ./scripts/run-server.sh
./scripts/run-server.sh
```

**Note:** The server must be started and listening first before launching any clients for local play. The server will listen for client connections on the default port.

**Expected Output:** You should see console messages indicating the server has started and is listening for connections.

#### 3. Start the Client:
Open a **new separate terminal window** in the same project directory and run:

Windows PowerShell:
```powershell
.\scripts\run-client.ps1
```

Linux/macOS:
```bash
chmod +x ./scripts/run-client.sh
./scripts/run-client.sh
```

**Note:** The client will automatically attempt to connect to the configured server (localhost or remote).

**Expected Output:** The game window should open, and you should see a connection confirmation in the terminal.

#### 4. Running Multiple Clients (Multiplayer Testing):
To test multiplayer functionality, open additional terminal windows and repeat step 3. Each new client will connect to the same server instance, allowing you to test multiplayer interactions.

```powershell
# Terminal 1: Server
.\scripts\run-server.ps1

# Terminal 2: Client 1
.\scripts\run-client.ps1

# Terminal 3: Client 2
.\scripts\run-client.ps1

# Terminal 4: Client 3 (and so on...)
.\scripts\run-client.ps1
```

```bash
# Terminal 1: Server
./scripts/run-server.sh

# Terminal 2: Client 1
./scripts/run-client.sh

# Terminal 3: Client 2
./scripts/run-client.sh
```

### Troubleshooting:
- **Connection Failed**: Ensure the server is running before starting the client.
- **Port Already in Use**: Close any existing server instances before starting a new one.
- **Java Version**: Verify you're using Java 21 with `java -version`.
- **Gradle Issues**: If you encounter code related issues, try refreshing the Gradle project, rebuilding or syncing gradle project.

## Technologies Used

- **Java 21** - Programming language
- **LibGDX 1.14.0** - Game development framework
- **KryoNet 2.22.9** - Network communication library for client-server architecture
- **Gradle 8.x** - Build automation tool
- **LWJGL3** - Lightweight Java Game Library for desktop deployment

## Game Features

| Feature                       | Description                                                                 |
|-------------------------------|-----------------------------------------------------------------------------|
| Extraction Shooter Gameplay   | Collect orbs and weapons, then extract to win                               |
| Multiplayer Support           | Multiple players can connect and compete simultaneously                     |
| Client-Server Architecture    | Dedicated server handles game logic and synchronization                     |
| NPC Enemies                   | AI-controlled opponents that attack players on sight                        |
| Weapon System                 | Various weapons scattered across the map for players to collect             |
| Orb Collection                | Core objective - collect orbs and extract with your loot                    |
| Real-time Input Processing    | Player inputs (WASD, Mouse) transmitted and processed in real-time          |
| Map System                    | LDtk-based map with collision detection and interactive environment         |
| TCP/UDP Networking            | Efficient network communication using KryoNet                               |
| Multiple Concurrent Clients   | Server supports multiple simultaneous player connections                    |
 | Register/Login System         | Players can create accounts and log in to track progress and stats            |

## Authors

- **Aleksandr Muskevits**
- **Artemij Kuzin**
- **Artur Kuznetsov**

## Detailed Client and Server Communication

### 1. Client-Server Communication (Overview)

The game uses the **KryoNet** framework for object-based communication over **TCP port 54555** (local) or **8080** (remote).
Communication is event-driven (using Listeners). All transmittable objects (packets) are registered in the `taltech.orbitgame.network.Network` class.

### 2. Client -> Server (Data sent by the Client)

**A. Login**
- **Sent when:** The connection is established
- **Content:** Player name (String)
- **Purpose:** To notify the server of a new player and identify the user
- **Class:** `Network.Login`

**B. InputState**
- **Sent when:** Every time the player presses a key (W, A, S, D) or clicks the mouse
- **Content:**
  - `w, a, s, d` (boolean): State of the directional keys
  - `leftClick, rightClick` (boolean): State of the mouse buttons
  - `mouseX, mouseY` (int): Mouse coordinates on the screen
  - `clientId` (int): Unique ID of the client
- **Purpose:** To send player input to the server for processing and broadcasting to other clients
- **Class:** `Network.InputState`

**C. ReadyState**
- **Sent when:** Player toggles ready status in the lobby (presses join lobby)
- **Content:**
  - `clientId` (int): Unique ID of the client
  - `isReady` (boolean): Whether the player is ready to start
- **Purpose:** To notify the server that a player is ready to begin the game
- **Class:** `Network.ReadyState`

**D. DisconnectionNotification**
- **Sent when:** Client disconnects from the lobby
- **Content:** `clientId` (int): Unique ID of the disconnecting client
- **Purpose:** To notify the server of intentional disconnection
- **Class:** `Network.DisconnectionNotification`

### 3. Server -> Client (Data sent by the Server)

**A. Registration**
- **Sent when:** As a response to the Login packet
- **Content:**
  - `id` (int): Unique ID assigned to the player by the server
  - `name` (String): Final player name (the server may append a suffix)
- **Purpose:** To confirm the connection and inform the client of its network identity
- **Class:** `Network.Registration`

**B. KeyPressEvent (Input Echoing)**
- **Sent when:** When the server receives an InputState packet from any client
- **Content:**
  - `clientId` (int): ID of the player who sent the input
  - `playerName` (String): Name of the player
  - `keys` (String): Textual description of the pressed keys (e.g., "W, A")
- **Purpose:** To notify all clients about other players' actions (echoing) to synchronize game state
- **Class:** `Network.KeyPressEvent`

**C. LobbyUpdate**
- **Sent when:** A player joins, leaves, or changes ready status
- **Content:** `players` (List<LobbyPlayer>): List of all players in the lobby
  - Each LobbyPlayer contains:
    - `id` (int): Player's unique ID
    - `name` (String): Player's name
    - `isReady` (boolean): Ready status
- **Purpose:** To synchronize lobby state across all connected clients
- **Class:** `Network.LobbyUpdate`

**D. GameStart**
- **Sent when:** All players in the lobby are ready and the game begins
- **Content:** None (signal packet)
- **Purpose:** To transition all clients from lobby to game screen
- **Class:** `Network.GameStart`

**E. PlayerPositionUpdate**
- **Sent when:** Player moves or changes state
- **Content:**
  - `clientId` (int): ID of the player
  - `x, y` (float): Player position
  - `moving` (boolean): Whether player is moving
  - `aimX, aimY` (float): Aim direction
- **Purpose:** To synchronize player positions across all clients
- **Class:** `Network.PlayerPositionUpdate`

**F. PlayerShoot**
- **Sent when:** A player fires their weapon
- **Content:**
  - `clientId` (int): ID of the player shooting
  - `x, y` (float): Position of the shot
  - `aimX, aimY` (float): Direction of the shot
- **Purpose:** To notify all clients when a player shoots
- **Class:** `Network.PlayerShoot`

**G. PlayerStep**
- **Sent when:** A player takes a step
- **Content:**
  - `clientId` (int): ID of the player
  - `x, y` (float): Position where step occurred
- **Purpose:** To play footstep sound effects for other players
- **Class:** `Network.PlayerStep`

**H. NpcStateUpdate**
- **Sent when:** NPC changes state, position, or health
- **Content:** NPC state information (position, health, state)
- **Purpose:** To synchronize NPC state across all clients
- **Class:** `Network.NpcStateUpdate`

**I. NpcDeathSound**
- **Sent when:** An NPC dies
- **Content:**
  - `npcId` (int): ID of the NPC
  - `x, y` (float): Position of death
- **Purpose:** To play NPC death sound effects
- **Class:** `Network.NpcDeathSound`

**J. NpcHitSound**
- **Sent when:** An NPC hits a player
- **Content:**
  - `npcId` (int): ID of the NPC
  - `x, y` (float): Position of hit
- **Purpose:** To play NPC attack sound effects
- **Class:** `Network.NpcHitSound`

## Connection Information

### Default Configuration (Remote TalTech Server)

The game is configured by default to connect to the **TalTech Remote Server**:
- **Host:** `193.40.157.153`
- **Port:** `8080`

Simply run the client without any configuration changes.

### Switching Server Configuration

**File:** `core/src/main/java/taltech/orbitgame/network/NetworkConfig.java` (lines 12-22)

**For Local Server Testing:**
```java
// Comment out these lines:
// private static final String DEFAULT_HOST = "193.40.157.153";
// private static final int DEFAULT_PORT = 8080;

// Uncomment these lines:
private static final String DEFAULT_HOST = "localhost";
private static final int DEFAULT_PORT = 54555;
```

**To Return to Remote Server:**
```java
// Uncomment these lines:
private static final String DEFAULT_HOST = "193.40.157.153";
private static final int DEFAULT_PORT = 8080;

// Comment out these lines:
// private static final String DEFAULT_HOST = "localhost";
// private static final int DEFAULT_PORT = 54555;
```

After changing the configuration:
1. Save the file
2. Rebuild the project: `./gradlew clean build` (or in IDE)
3. Run the client/server scripts

