package io.github.adelinam17n.skippacks.client.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import io.github.adelinam17n.skippacks.client.ducks.SkippedRequiredPackGetter;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public class ServerDataMixin implements SkippedRequiredPackGetter {
    @Unique
    private boolean requiredPackSkipped = false;

    @Inject(
            method = "read",
            at = @At(
                    "TAIL"
            )
    )
    private static void injectTail$read$skipserverpacks(CompoundTag nbtCompound, CallbackInfoReturnable<ServerData> cir, @Local ServerData serverData) {
        if (nbtCompound.contains("requiredPackSkipped")) {
            ((SkippedRequiredPackGetter) serverData).setRequiredPackSkipped$skipserverpacks(
                    nbtCompound.getBoolean("requiredPackSkipped")
            );
        }
    }

    @Inject(
            method = "write",
            at = @At(
                    "TAIL"
            )
    )
    private void injectTail$write$skipserverpacks(CallbackInfoReturnable<CompoundTag> cir, @Local CompoundTag compoundTag) {
        compoundTag.putBoolean("requiredPackSkipped", requiredPackSkipped);
    }

    @Override
    public boolean getRequiredPackSkipped$skipserverpacks() {
        return requiredPackSkipped;
    }

    @Override
    public void setRequiredPackSkipped$skipserverpacks(boolean value) {
        this.requiredPackSkipped = value;
    }
}