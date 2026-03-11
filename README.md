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
2. **Spawn In**: You will spawn at a random location on the map.
3. **Collect Resources**: Explore the map to find orbs and weapons.
4. **Combat**: Fight against NPCs and other players to survive.
5. **Extract**: Make your way to an extraction point to secure your loot and win.
6. **Survive**: Player that extracted gets loot and XP.

#### Running the Game Locally

This project is a **monorepo** containing both the Server and Client components. Follow these step-by-step instructions to run the game on your local machine.

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

#### 2. Start the Server:
Open a terminal in the project root directory and run:
```bash
./gradlew Server:run
```

**Note:** The server must be started and listening first before launching any clients. The server will listen for client connections on the default port.

**Expected Output:** You should see console messages indicating the server has started and is listening for connections.

#### 3. Start the Client:
Open a **new separate terminal window** in the same project directory and run:
```bash
./gradlew lwjgl3:run
```

**Note:** The client will automatically attempt to connect to the server running on localhost.

**Expected Output:** The game window should open, and you should see a connection confirmation in both the server and client terminals.

#### 4. Running Multiple Clients (Multiplayer Testing):
To test multiplayer functionality, open additional terminal windows and repeat step 3. Each new client will connect to the same server instance, allowing you to test multiplayer interactions.

```bash
# Terminal 1: Server
./gradlew Server:run

# Terminal 2: Client 1
./gradlew lwjgl3:run

# Terminal 3: Client 2
./gradlew lwjgl3:run

# Terminal 4: Client 3 (and so on...)
./gradlew lwjgl3:run
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

| Feature                          | Description                                                                 |
|----------------------------------|-----------------------------------------------------------------------------|
| Extraction Shooter Gameplay      | Collect orbs and weapons, then extract to win                               |
| Multiplayer Support              | Multiple players can connect and compete simultaneously                     |
| Client-Server Architecture       | Dedicated server handles game logic and synchronization                     |
| NPC Enemies                      | AI-controlled opponents that attack players on sight                        |
| Weapon System                    | Various weapons scattered across the map for players to collect             |
| Orb Collection                   | Core objective - collect orbs and extract with your loot                    |
| Real-time Input Processing       | Player inputs (WASD, Mouse) transmitted and processed in real-time          |
| Map System                       | LDtk-based map with collision detection and interactive environment         |
| TCP/UDP Networking               | Efficient network communication using KryoNet                               |
| Multiple Concurrent Clients      | Server supports multiple simultaneous player connections                    |

## Authors

- **Aleksandr Muskevits**
- **Artemij Kuzin**
- **Artur Kuznetsov**

## Connection Information

### Connecting to TalTech Server:
TBA
