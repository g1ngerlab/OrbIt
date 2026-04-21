# ITI0301-2024
## Orb it

### Mängu kirjeldus / Game Description
"Orb it" is a 2D extraction shooter game. The core objective is to collect valuable orbs scattered across the map. Players must fight against other players and AI-controlled NPCs, survive, and successfully reach an extraction zone to secure their loot. If a player dies, they lose their current non-secured loot. Survived loot can be used in the in-game market between matches.

### Mängimise juhend / How to Play

**Sisenemine mängu (Entering the Game):**
1. Launch the game client. (Ensure network settings are configured correctly for either local or TalTech server connection.)
2. At the login screen, enter your username to authenticate to the server.
3. In the character selection screen, choose your character.
4. From the Main Menu, open the Lobby system. You can create a new lobby or join an existing one. 
5. The game starts, you will spawn on the map.

**Nuppude funktsioonid (Controls):**
- **W, A, S, D** - Move character (Up, Left, Down, Right)
- **Tab** - Open/Close inventory
- **G** - Drop currently selected (in inventory) item
- **Mouse cursor** - Aiming
- **Left Mouse Button** - Shoot / Attack
- **Extraction:** Find an extraction zone on the map, stand within its borders, press E and survive the timer to extract successfully with your loot.

### Mängu käivitamine lokaalselt / Running the Game Locally
This project is a monorepo containing both the Client and Server components. 

**Step 1: Configure Local Connection**
Open `core/src/main/java/taltech/orbitgame/network/NetworkConfig.java` and switch to local settings:
```java
// Comment out remote server:
// public static final String DEFAULT_HOST = "193.40.157.153";
// public static final int DEFAULT_PORT = 8080;

// Uncomment local server:
public static final String DEFAULT_HOST = "localhost";
public static final int DEFAULT_PORT = 54555;
```

**Step 2: Start the Server**
Run the server locally using the provided script (Windows):
```powershell
.\scripts\run-server.ps1
```
Or (Linux/macOS):
```bash
./scripts/run-server.sh
```
Alternatively, run `server.ServerLauncher` directly from your IDE.

**Step 3: Start the Client**
Run the client using the script (Windows):
```powershell
.\scripts\run-client.ps1
```
Or (Linux/macOS):
```bash
./scripts/run-client.sh
```
Alternatively, run the `lwjgl3` run task from Gradle (`./gradlew lwjgl3:run`). Make sure the server is fully started before launching the client.

*(Another user can run this project by pulling the repository, following Java 21 setup, and using the exact steps above.)*

### Kasutatud tehnoloogiate loetelu / Technologies Used
- **Java** 21
- **LibGDX** 1.12.1 (Graphics, Input, Audio, UI)
- **KryoNet** 2.22.0-RC1 (TCP Client-Server object-based networking)
- **LDtk** (Map creation and parsing)
- **Gradle** 8.x (Build System)

### Mängu feature'id / Main Game Features
| Feature | Description | State |
| :--- | :--- | :--- |
| **Client-Server Architecture** | Authoritative server that validates movement, combat, and game state. | Done |
| **Map & Collision** | Custom parsed `.ldtk` maps with a server-side collision grid. | Done |
| **Movement System** | 2D player movement broadcasted to the server and synchronized to all clients. | Done |
| **Combat System** | Real-time shooting with server-validated hit registration and bullet traces. | Done |
| **AI (NPCs)** | Server-controlled non-playable enemies that wander and attack players. | Done |
| **Lobby & Matchmaking** | Pre-game session management. Players can create or join concurrent lobbies. | Done |
| **Extraction & Market System** | Dedicated extraction zones to escape with collected loot, and a market to trade items. | Done |
| **Dynamic Audio** | Ambient and combat tracks that dynamically switch depending on the client's current state (Menu -> Lobby -> Game). | Done |

### Info sellest, kuidas klient TalTechi serveriga ühendust saab / Connecting to TalTech Server
By default, the client is set up to connect to the TalTech school server automatically.
You just need to ensure `NetworkConfig.java` has the remote settings active:
- **IP:** `193.40.157.153`
- **Port:** `8080` (TCP)

*(If accessing from a separate network, make sure you are connected to the TalTech VPN.)*

### Autorite nimed / Authors
- Artur Kuznetsov, Aleksandr Muskevits, Artemij Kuzin
