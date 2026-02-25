# Orb it
## Description (Mängu kirjeldus)
"Orb it" is an extraction shooter game where the core objective is to collect orbs and weapons scattered across the map. Players must fight against other players and NPCs to survive and successfully extract with their loot.
## How to Play (Mängimise juhend)
### Starting the Game
1.  Start the Server: Ensure the server is running first.
2.  Start the Client: Launch the client application.
3.  Automatic Connection: The client will automatically attempt to connect to the server.
### Controls
Currently implemented controls for testing server communication:
*   W / A / S / D: Move character (Server echoes input)
*   Mouse: Aim / Click (Server echoes input)
## Running Locally (Mängu käivitamine lokaalselt)
This project is a monorepo containing both the Server and Client components. You can run them using Gradle wrapper commands from the project root directory.
### Prerequisites
*   Java 21
### Step-by-Step Guide
1.  Start the Server:
    Open a terminal in the project root and run:

    ./gradlew Server:run

    *(Note: This starts the server module which listens for client connections.)*
2.  Start the Client:
    Open a separate terminal window and run:

    ./gradlew lwjgl3:run

3.  Multiple Clients:
    To test multiplayer scenarios, open additional terminal windows and run the client command again. Every new client will connect to the same server instance.
## Technologies Used (Kasutatud tehnoloogiate loetelu)
*   Language: Java 21
*   Game Framework: LibGDX 1.14.0
*   Networking: KryoNet 2.22.9
*   Build Tool: Gradle
## Features (Mängu feature'id)
*   Client-Server Architecture: Distinct server and client applications communicating over TCP/UDP.
*   Multiplayer Support: The server can handle multiple simultaneous client connections.
*   Input Echoing: Real-time transmission of player inputs (WASD, Mouse) from the client to the server, where they are logged to the terminal.
## Authors (Autorite nimed)
*   Aleksandr Muskevits
*   Artemij Kuzin
*   Artur Kuznetsov
## Connection Info (Info ühenduse kohta)
*   (To be added) - Information on how the client connects to the TalTech server.
*   Repository Link: This project is a monorepo containing both Server and Client.
