package com.sophia.easyforum.gui.handlers;

import com.sophia.easyforum.gui.custom.GuiChatCustom;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class GuiHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (shouldReplaceGui(event.gui)) {
            event.gui = new GuiChatCustom();
        }
    }

    private boolean shouldReplaceGui(Object gui) {
        return gui instanceof GuiChat && !(gui instanceof GuiChatCustom);
    }
}