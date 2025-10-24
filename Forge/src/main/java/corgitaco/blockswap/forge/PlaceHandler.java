package corgitaco.blockswap.forge;

import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = corgitaco.blockswap.BlockSwap.MOD_ID)
public class PlaceHandler {

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState placed = event.getPlacedBlock();
        BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
        // Must have mapping and per-block flag to replace player placed
        if (cfg.blockBlockMap().containsKey(placed.getBlock())) {
            BlockSwapConfig.RuleFlags flags = cfg.rules().get(placed.getBlock());
            if (flags != null && flags.replacePlayerPlaced()) {
                BlockPos pos = event.getPos();
                level.setBlock(pos, Swapper.remapState(placed), 2);
            }
        }
    }
}
