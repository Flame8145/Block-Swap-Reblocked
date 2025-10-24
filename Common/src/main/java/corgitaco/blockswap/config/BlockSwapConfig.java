package corgitaco.blockswap.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.blockswap.BlockSwap;
import corgitaco.blockswap.swapper.Swapper;
import corgitaco.corgilib.serialization.codec.CodecUtil;
import corgitaco.corgilib.serialization.codec.CommentedCodec;
import corgitaco.corgilib.serialization.jankson.JanksonJsonOps;
import corgitaco.corgilib.serialization.jankson.JanksonUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public record BlockSwapConfig(Map<Block, Block> blockBlockMap, Map<BlockState, BlockState> blockStateBlockStateMap,
                              boolean retroGen, boolean generateAllKnownStates,
                              Map<Block, RuleFlags> rules) {

    public static final BlockSwapConfig DEFAULT = new BlockSwapConfig(new IdentityHashMap<>(), new IdentityHashMap<>(), false, true, new IdentityHashMap<>());

    public record RuleFlags(boolean retroGen, boolean replacePlayerPlaced) {
        public static final RuleFlags DEFAULT = new RuleFlags(false, false);
        public static final Codec<RuleFlags> CODEC = RecordCodecBuilder.create(b -> b.group(
                CommentedCodec.of(Codec.BOOL, "retro_gen", "When true, also replace in already-generated chunks (retrogen). Defaults to false.").xmap(Boolean::booleanValue, v -> v).forGetter(RuleFlags::retroGen),
                CommentedCodec.of(Codec.BOOL, "replace_player_placed", "When true, also replace when a player places the old block. Defaults to false.").xmap(Boolean::booleanValue, v -> v).forGetter(RuleFlags::replacePlayerPlaced)
        ).apply(b, RuleFlags::new));
    }

    private static final String SWAPPER_EXAMPLE = """
            	"swapper": {
            		"minecraft:coarse_dirt": "minecraft:dirt",
            		"minecraft:diamond_block": "minecraft:emerald_block"
            	}
            """;

    private static final String STATE_SWAPPER_EXAMPLE = """
             "state_swapper": [
             	{
             		"new": {
             			"Name": "minecraft:birch_log",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"axis": "x"
             			}
             		},
             		"old": {
             			"Name": "minecraft:oak_log",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"axis": "z"
             			}
             		}
             	},
             	{
             		"new": {
             			"Name": "minecraft:birch_leaves",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"distance": "7",
             				"persistent": "true"
             			}
             		},
             		"old": {
             			"Name": "minecraft:acacia_log",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"axis": "z"
             			}
             		}
             	},
             	{
             		"new": {
             			"Name": "minecraft:jungle_log",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"axis": "x"
             			}
             		},
             		"old": {
             			"Name": "minecraft:birch_log",
             			// Properties define the state of this block/fluid.
             			"Properties": {
             				"axis": "z"
             			}
             		}
             	},
             	{
             		"new": {
             			"Name": "minecraft:jungle_planks",
             		},
             		"old": {
             			"Name": "minecraft:acacia_planks",
             			}
             		}
             	}
             ]
            """;

    private static final Codec<Map<Block, RuleFlags>> RULES_CODEC = Codec.unboundedMap(CodecUtil.BLOCK_CODEC, RuleFlags.CODEC);

    private static final Codec<BlockSwapConfig> RAW_CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    CommentedCodec.of(Codec.unboundedMap(CodecUtil.BLOCK_CODEC, CodecUtil.BLOCK_CODEC), "swapper", "A map of blocks that specifies what the \"old\" block is and what its \"new\" block is.\nExample:\n" + SWAPPER_EXAMPLE).forGetter(BlockSwapConfig::blockBlockMap),
                    CommentedCodec.of(Swapper.KEYABLE_BLOCKSTATE_CODEC, "state_swapper", "A map of states that specifies what the \"old\" block state is and what its \"new\" block state is.\nSee \"known_states\" folder(\"generate_all_known_states\" must be set to true in this config) to see all known block states available for all blocks available in the registry.\nExample:\n" + STATE_SWAPPER_EXAMPLE).forGetter(BlockSwapConfig::blockStateBlockStateMap),
                    CommentedCodec.of(Codec.BOOL, "retro_gen", "Whether blocks are replaced in existing chunks (global). Per-block rules may override behavior.").forGetter(BlockSwapConfig::retroGen),
                    CommentedCodec.of(Codec.BOOL, "generate_all_known_states", "Generates all block states for all blocks in the registry.").forGetter(BlockSwapConfig::generateAllKnownStates),
                    CommentedCodec.of(RULES_CODEC, "rules", "Per-block rule flags: retro_gen and replace_player_placed.").forGetter(BlockSwapConfig::rules)
            ).apply(builder, BlockSwapConfig::new)
    );

    public static final Codec<BlockSwapConfig> CODEC = RAW_CODEC.flatXmap(verifyConfig(), verifyConfig());

    // Legacy codec without per-block rules to keep backward compatibility with existing configs
    private static final Codec<BlockSwapConfig> LEGACY_CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    CommentedCodec.of(Codec.unboundedMap(CodecUtil.BLOCK_CODEC, CodecUtil.BLOCK_CODEC), "swapper", "").forGetter(BlockSwapConfig::blockBlockMap),
                    CommentedCodec.of(Swapper.KEYABLE_BLOCKSTATE_CODEC, "state_swapper", "").forGetter(BlockSwapConfig::blockStateBlockStateMap),
                    CommentedCodec.of(Codec.BOOL, "retro_gen", "").forGetter(BlockSwapConfig::retroGen),
                    CommentedCodec.of(Codec.BOOL, "generate_all_known_states", "").forGetter(BlockSwapConfig::generateAllKnownStates)
            ).apply(builder, (map, stateMap, retro, known) -> new BlockSwapConfig(map, stateMap, retro, known, new IdentityHashMap<>()))
    );

    private static BlockSwapConfig CONFIG = null;


    public boolean contains(BlockState state) {
        return blockBlockMap.containsKey(state.getBlock()) || blockStateBlockStateMap.containsKey(state);
    }

    private static Function<BlockSwapConfig, DataResult<BlockSwapConfig>> verifyConfig() {
        return blockSwapConfig -> {
            StringBuilder blockSwapperErrors = new StringBuilder();
            for (Block block : blockSwapConfig.blockBlockMap.values()) {
                if (blockSwapConfig.blockBlockMap.containsKey(block)) {
                    blockSwapperErrors.append(BuiltInRegistries.BLOCK.getKey(block)).append("\n");
                }
            }

            StringBuilder stateSwapperErrors = new StringBuilder();

            for (BlockState value : blockSwapConfig.blockStateBlockStateMap.values()) {
                if (blockSwapConfig.blockStateBlockStateMap.containsKey(value)) {
                    stateSwapperErrors.append(value.toString()).append("\n");
                }
            }


            String errorMessage = "";

            if (!blockSwapperErrors.isEmpty()) {
                errorMessage = errorMessage + String.format("Detected circular BLOCK reference(s) in the \"swapper\"! Blocks being swapped cannot be used as a block to swap into. Circular references found:\n%s\n", blockSwapperErrors);
            }

            if (!stateSwapperErrors.isEmpty()) {
                errorMessage = errorMessage + String.format("Detected circular BLOCKSTATE reference(s) in the \"state_swapper\"! BlockStates being swapped cannot be used as a BlockState to swap into. Circular references found:\n%s", stateSwapperErrors);
            }

            if (!errorMessage.isEmpty()) {
                String finalErrorMessage = errorMessage;
                return DataResult.error(() -> finalErrorMessage);
            }

            return DataResult.success(blockSwapConfig);
        };
    }

    public static BlockSwapConfig getConfig(BlockSwapConfig server) {
        CONFIG = server;
        return CONFIG;
    }

    public boolean hasAnyRetroGenRule() {
        if (rules == null || rules.isEmpty()) return false;
        for (RuleFlags f : rules.values()) {
            if (f != null && f.retroGen()) return true;
        }
        return false;
    }

    public static BlockSwapConfig getConfig(boolean reload) {
        if (CONFIG == null || reload) {
            Path path = BlockSwap.CONFIG_PATH.resolve("block_swap.json5");
            File configFile = path.toFile();
            if (!configFile.exists()) {
                JanksonUtil.createConfig(path, BlockSwapConfig.CODEC, JanksonUtil.HEADER_CLOSED, new Object2ObjectOpenHashMap<>(), JanksonJsonOps.INSTANCE, BlockSwapConfig.DEFAULT);
            }
            try {
                CONFIG = JanksonUtil.readConfig(path, BlockSwapConfig.CODEC, JanksonJsonOps.INSTANCE);
            } catch (Throwable t) {
                // Backward compatibility: try legacy schema (without rules), then upgrade
                BlockSwapConfig legacy = JanksonUtil.readConfig(path, LEGACY_CODEC, JanksonJsonOps.INSTANCE);
                CONFIG = new BlockSwapConfig(new IdentityHashMap<>(legacy.blockBlockMap()), new IdentityHashMap<>(legacy.blockStateBlockStateMap()), legacy.retroGen(), legacy.generateAllKnownStates(), new IdentityHashMap<>());
                // Persist upgraded config with rules field present
                save(CONFIG);
            }
        }
        return CONFIG;
    }

    public static void save(BlockSwapConfig config) {
        Path path = BlockSwap.CONFIG_PATH.resolve("block_swap.json5");
        // Overwrite the existing config file with current values
        JanksonUtil.createConfig(path, BlockSwapConfig.CODEC, JanksonUtil.HEADER_CLOSED, new Object2ObjectOpenHashMap<>(), JanksonJsonOps.INSTANCE, config);
        CONFIG = config;
    }
}
