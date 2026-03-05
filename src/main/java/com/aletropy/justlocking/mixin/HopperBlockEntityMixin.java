package com.aletropy.justlocking.mixin;

import com.aletropy.justlocking.data.LockDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends BlockEntity {

    public HopperBlockEntityMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
            BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Prevents Hopper from pushing items into a locked container.
     */
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void onEjectItems(Level level, BlockPos pos,
            HopperBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (level == null)
            return;

        BlockState state = level.getBlockState(pos);
        BlockPos targetPos = pos.relative(state.getValue(HopperBlock.FACING));
        BlockState targetState = level.getBlockState(targetPos);
        BlockEntity targetBe = level.getBlockEntity(targetPos);

        // Blistering fast early-exit check
        if (!LockDataManager.INSTANCE.mightBeLockable(targetState))
            return;

        // If locked (meaning there's an owner), cancel item eject
        if (LockDataManager.INSTANCE.isLocked(level, targetPos, targetState, targetBe)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Prevents Hopper from sucking items from a locked container above.
     */
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void onSuckInItems(Level level, net.minecraft.world.level.block.entity.Hopper hopper,
            CallbackInfoReturnable<Boolean> cir) {
        if (level == null || hopper == null)
            return;

        // The item source is always the block directly above the hopper
        BlockPos sourcePos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0D, hopper.getLevelZ());
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
