package com.sophia.easyforum.mixins;

import com.sophia.easyforum.command.ForumCommandLogic;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class EntityPlayerSPMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        String lower = message.toLowerCase();

        if (lower.equals("/forum") || lower.startsWith("/forum ") || lower.equals("/f") || lower.startsWith("/f ")) {
            ci.cancel();

            String[] args;

            if (lower.startsWith("/forum")) {
                if (message.length() > "/forum".length()) {
                    args = message.substring("/forum ".length()).split(" ");
                } else {
                    args = new String[0];
                }
            } else {
                if (message.length() > "/f".length()) {
                    args = message.substring("/f ".length()).split(" ");
                } else {
                    args = new String[0];
                }
            }

            ForumCommandLogic.execute(args);
        }
    }
}