# Block-Swap Reblocked
A maintained continuation of Block Swap for Minecraft 1.20.1 on Fabric and Forge. Define rules that automatically replace one block with another during worldgen and/or gameplay, with fine-grained control and in‑game configuration.

## Why “Reblocked”?
- Modernized config flow with in‑game screens (Fabric via Mod Menu; Forge via ConfigScreenHandler)
- Per‑block flags for retrogen and player‑placed handling
- Server↔client config sync for multiplayer
- Safer worldgen‑only swapping and improved retrogen behavior

## Features
- Rule-based block replacement (by block, state, tags, and optional conditions)
- Per‑rule flags:
  - `retro_gen` — apply during retrogen
  - `replace_player_placed` — include blocks placed by players
- Worldgen‑only hook for performance-friendly generation-time swaps
- In‑game config UI and automatic upgrade of legacy configs

## Requirements
- Minecraft: 1.20.1
- Fabric: Fabric Loader ≥ 0.14, Fabric API, CorgiLib ≥ 4.0.0.0
- Forge: 47.2.x, CorgiLib ≥ 4.0.0.0
- Java 17+

## Download
Grab releases from the Releases page:
https://github.com/Flame8145/Block-Swap-Reblocked/releases

## Quick Start
1) Install the correct loader (Fabric or Forge) and required libraries.  
2) Drop the Reblocked JAR (and CorgiLib) into your `mods` folder.  
3) Launch the game:
   - Fabric: edit rules via Mod Menu → Block Swap
   - Forge: open the Block Swap config screen from Mods → Block Swap
