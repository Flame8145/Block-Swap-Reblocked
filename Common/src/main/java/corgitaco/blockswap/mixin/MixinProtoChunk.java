package corgitaco.blockswap.mixin;

import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk {

    @Shadow
    public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean moved);

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void blockswap_worldgenOnly(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
        if (!cfg.retroGen() && cfg.contains(state)) {
            cir.setReturnValue(setBlockState(pos, Swapper.remapState(state), moved));
        }
    }
}
