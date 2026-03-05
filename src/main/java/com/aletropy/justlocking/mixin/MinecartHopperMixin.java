package com.aletropy.justlocking.mixin;

import com.aletropy.justlocking.data.LockDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartHopper.class)
public abstract class MinecartHopperMixin {

    /**
     * Prevents Minecart Hopper from sucking items from a locked container above.
     */
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private void onSuckInItems(CallbackInfoReturnable<Boolean> cir) {
        MinecartHopper cart = (MinecartHopper) (Object) this;
        Level level = cart.level();
        if (level == null)
            return;

        // The item source is always the block directly above the hopper minecart
        BlockPos sourcePos = BlockPos.containing(cart.getX(), cart.getY() + 1.0D, cart.getZ());
        BlockState sourceState = level.getBlockState(sourcePos);
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);

        // Blistering fast early-exit check
        if (!LockDataManager.INSTANCE.mightBeLockable(sourceState))
            return;

        // If locked (meaning there's an owner), cancel item suck
        if (LockDataManager.INSTANCE.isLocked(level, sourcePos, sourceState, sourceBe)) {
            cir.setReturnValue(false);
        }
    }
}
