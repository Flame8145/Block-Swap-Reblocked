package corgitaco.blockswap.mixin;

import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Inject(method = "place", at = @At("TAIL"))
    private void blockswap_replaceOnPlace(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = ctx.getClickedPos();
        BlockState placed = level.getBlockState(pos);
        BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
        if (cfg.blockBlockMap().containsKey(placed.getBlock())) {
            BlockSwapConfig.RuleFlags flags = cfg.rules().get(placed.getBlock());
            if (flags != null && flags.replacePlayerPlaced()) {
                level.setBlock(pos, Swapper.remapState(placed), 2);
            }
        }
    }
}
