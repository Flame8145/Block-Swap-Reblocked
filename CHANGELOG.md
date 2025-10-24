# 6.0.0.0
* Add in-game config screen: Forge via ConfigScreenHandler; Fabric via Mod Menu integration.
* Add per-block rule flags in config: `retro_gen` and `replace_player_placed` (legacy configs auto-upgrade).
* Add player placement handlers to replace player-placed blocks when the per-block flag is enabled (Forge + Fabric).
* Add server-to-client config sync on join (Forge SimpleChannel, Fabric global receiver).
* Add worldgen-only swap hook (ProtoChunk mixin) to apply replacements during chunk generation when global retrogen is off.
* Change retrogen to respect per-block `retro_gen` flags.
* Update mixins: add `MixinProtoChunk`; update `blockswap.mixins.json`.
* Remove `MixinLevel` and remove `known_states` generation on server init.

# 5.0.0.2
* Depend on CorgiLib
* Move Codecs/Serializers to CorgiLib
* 
# 5.0.0.1
* Fix Retrogen
* 
# 5.0.0.0
* Port to 1.20
* 
# 4.0.0.2
* Depend on CorgiLib
* Move Codecs/Serializers to CorgiLib

# 4.0.0.1
* Fix Retrogen

# 4.0.0.0
* Port to 1.19.4

# 3.0.0.0
* Port to 1.19.3

# 2.0.0.0
* Port to 1.19
* Add ability to write to section pallette directly to remap unknown/deleted blocks to a valid block in the registry.

# 1.0.1
* Add comments/docs to config & informative errors when configs fail to serialize.
* Print every known blockstate into the `config/block_swap/known_states`