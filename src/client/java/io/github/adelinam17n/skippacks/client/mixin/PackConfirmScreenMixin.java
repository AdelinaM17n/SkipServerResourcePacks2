package io.github.adelinam17n.skippacks.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.adelinam17n.skippacks.client.ducks.SkippedRequiredPackGetter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl$PackConfirmScreen")
public abstract class PackConfirmScreenMixin{

    @WrapWithCondition(
            method = "method_55612(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;ZLjava/util/List;Lnet/minecraft/client/multiplayer/ClientCommonPacketListenerImpl;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/chat/Component;)V")
    )
    private static boolean wrapDisconnectInvoke(Connection instance, Component component){
        return false;
    }

    @WrapWithCondition(
            method = "method_55612",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/server/DownloadedPackSource;rejectServerPacks()V")
    )
    private static boolean wrapRejectPack(DownloadedPackSource instance) {
        return false;
    }

    @Inject(
            method = "method_55612",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/chat/Component;)V"),
            cancellable = true
    )
    private static void openNewScreenByDisconnect(Minecraft minecraft, Screen screen, boolean bl, List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list, ClientCommonPacketListenerImpl clientCommonPacketListenerImpl, boolean bl2, CallbackInfo ci, @Local DownloadedPackSource downloadedPackSource){
        minecraft.setScreen(new ConfirmScreen(
                xBool -> {
                    if(!xBool){
                        if(clientCommonPacketListenerImpl.serverData != null){
                            ((SkippedRequiredPackGetter) clientCommonPacketListenerImpl.serverData).setRequiredPackSkipped$skipserverpacks(true);

                            for (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest pendingRequest : list) {
                                clientCommonPacketListenerImpl.connection.send(new ServerboundResourcePackPacket(pendingRequest.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
                                clientCommonPacketListenerImpl.connection.send(new ServerboundResourcePackPacket(pendingRequest.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
                                clientCommonPacketListenerImpl.connection.send(new ServerboundResourcePackPacket(pendingRequest.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
                            }

                            ServerList.saveSingleServer(clientCommonPacketListenerImpl.serverData);
                            minecraft.setScreen(screen);

                            ci.cancel();
                        }
                    }else {
                        downloadedPackSource.rejectServerPacks();
                        minecraft.setScreen(screen);
                    }
                },

                Component.literal("To lie or to not lie?"),
                Component.literal(
                        """
                               If you want to join the server without downloading the resource-pack, click "Decline and Lie".
                               Be Warned! The server intends you to have this resource-pack, things might not look or function correctly without it!
                               
                               If you decline and have the server added on the server list, on subsequent logins the resource-pack will be automatically skipped. Changing the "Server Resource Packs" option in the Server Info tab for the server will reset this
                               """
                ),
                Component.literal("Disconnect"),
                Component.literal("Decline and lie")
        ));
    }

    @Inject(
            method = "method_55612",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ServerData;setResourcePackStatus(Lnet/minecraft/client/multiplayer/ServerData$ServerPackStatus;)V", ordinal = 1)
    )
    private static void addRejectBeforeOptional(Minecraft minecraft, Screen screen, boolean bl, List<?> list, ClientCommonPacketListenerImpl clientCommonPacketListenerImpl, boolean bl2, CallbackInfo ci, @Local DownloadedPackSource downloadedPackSource){
        downloadedPackSource.rejectServerPacks();
    }

    @ModifyArgs(
            method = "<init>",
            at = @At(value = "INVOKE", target = "net/minecraft/client/gui/screens/ConfirmScreen.<init> (Lit/unimi/dsi/fastutil/booleans/BooleanConsumer;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V")

    )
    private static void modifySuperArgs(Args args){
        args.set(2, Component.literal(
                """
                        SkipServerResourcePacks mod allows you to decline the resource-pack without getting disconnected using an advanced technique called "lying" to server

                        Clicking "Accept" will download the resource pack
                        Click "Decline" to proceed without downloading the resource pack
                        """
        ).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        args.set(3, Component.literal("Accept"));
        args.set(4, Component.literal("Decline"));
    }
}
