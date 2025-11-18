package io.github.adelinam17n.skippacks.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.adelinam17n.skippacks.client.ducks.SkippedRequiredPackGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.util.UUID;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected Connection connection;

    @Shadow
    @Final
    @Nullable
    protected ServerData serverData;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected abstract Screen addOrUpdatePackPrompt(UUID uUID, URL uRL, String string, boolean bl, @Nullable Component component);

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
           sendLiesAndPrayers(uUID);
           ci.cancel();
        }
    }

    @Inject(
            method = "handleResourcePackPush",
            at = @At(
                    target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
                    value = "INVOKE"
            ),
            cancellable = true
    )
    private void injectBeforeScreen(ClientboundResourcePackPushPacket clientboundResourcePackPushPacket, CallbackInfo ci, @Local UUID uUID, @Local URL uRL, @Local String string, @Local boolean bl){
        if(bl){
            Screen oldScreen = this.minecraft.screen;
            this.minecraft.setScreen(
                    new ConfirmScreen(
                    accepted ->
                    {
                        minecraft.setScreen(oldScreen);
                        if(!accepted){
                            assert serverData != null;
                            this.sendLiesAndPrayers(uUID);
                            ((SkippedRequiredPackGetter) this.serverData).setRequiredPackSkipped$skipserverpacks(true);
                            ServerList.saveSingleServer(this.serverData);
                        }else {
                            this.minecraft.setScreen(this.addOrUpdatePackPrompt(uUID, uRL, string, bl, clientboundResourcePackPushPacket.prompt().orElse(null)));
                        }
                    },
                    Component.literal("To lie or to not lie?"),
                    Component.literal(
                            """
                                   This server requires you to use their custom resource-pack, with SkipServerResourcepacks mod, you can decline it and still join the server.
                                   
                                   If you want to join the server without downloading the resource-pack, click "Decline".
                                   Be Warned! The server intends you to have this resource-pack, things might not look or function correctly without it!
                                   
                                   Clicking on "Proceed" will redirect you to the vanilla prompt screen, where you can accept the pack or disconnect.
                                   """
                    ),
                    Component.literal("Proceed"),
                    Component.literal("Decline")
            ));
            ci.cancel();
        }
    }

    @Unique
    private void sendLiesAndPrayers(UUID uUID){
        this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.ACCEPTED));
        this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.DOWNLOADED));
        this.connection.send(new ServerboundResourcePackPacket(uUID, ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
    }
}
