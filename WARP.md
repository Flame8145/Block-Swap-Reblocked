# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.
## High-level Architecture
This project is a Minecraft mod that supports both the Forge and Fabric mod loaders. The codebase is structured into three main sub-projects:
*   **`Common`**: This project contains the majority of the mod's code, including blocks, items, entities, and game logic. This code is platform-agnostic.
*   **`Forge`**: This project contains the Forge-specific code, which acts as a wrapper around the `Common` code. It handles Forge-specific APIs and event handling.
*   **`Fabric`**: This project contains the Fabric-specific code, which acts as a wrapper around the `Common` code. It handles Fabric-specific APIs and event handling.
This multi-loader architecture allows the mod to be built for both Forge and Fabric from a single codebase.
## Common Development Tasks
This is a Gradle project. The Gradle wrapper (`gradlew` or `gradlew.bat`) should be used for all builds.
### Building the mod
To build the mod for both Forge and Fabric, run the following command from the root directory:
```sh
./gradlew build
```
The resulting JAR files will be located in the `build/libs` directory of each sub-project (`Forge/build/libs` and `Fabric/build/libs`).
### Running the game for development
**For Forge:**
*   To run the Minecraft client with the mod loaded:
    ```sh
    ./gradlew :Forge:runClient
    ```
*   To run the Minecraft server with the mod loaded:
    ```sh
    ./gradlew :Forge:runServer
    ```
**For Fabric:**
*   To run the Minecraft client with the mod loaded:
    ```sh
    ./gradlew :Fabric:runClient
    ```
*   To run the Minecraft server with the mod loaded:
    ```sh
    ./gradlew :Fabric:runServer
    ```