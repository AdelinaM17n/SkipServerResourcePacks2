package io.github.adelinam17n.skippacks.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.adelinam17n.skippacks.client.ducks.SkippedRequiredPackGetter;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow
    @Final
    public Connection connection;

    @Shadow
    @Final
    @Nullable
    public ServerData serverData;

    @Inject(
            method = "handleResourcePackPush",
            at = @At(
                    target = "net/minecraft/client/multiplayer/ClientCommonPacketListenerImpl.parseResourcePackUrl (Ljava/lang/String;)Ljava/net/URL;",
                    shift = At.Shift.AFTER,
                    value = "INVOKE"
            ),
            cancellable = true
    )
    private void injectAfterParseUrl$handleResourcePackPush(ClientboundResourcePackPushPacket clientboundResourcePackPushPacket, CallbackInfo ci, @Local UUID uUID){
        if(this.serverData != null && ((SkippedRequiredPackGetter) this.serverData).getRequiredPackSkipped$skipserverpacks()){
            this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.ACCEPTED));
            this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.DOWNLOADED));
            this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            ci.cancel();
        }
    }
}
