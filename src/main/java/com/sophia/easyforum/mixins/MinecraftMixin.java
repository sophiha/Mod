package com.sophia.easyforum.mixins;

import com.sophia.easyforum.gui.custom.GuiChatCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void onDisplayGuiScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (guiScreenIn instanceof GuiChat &&!(guiScreenIn instanceof GuiChatCustom)) {
            ci.cancel();

            Minecraft.getMinecraft().displayGuiScreen(new GuiChatCustom());
        }
    }
}