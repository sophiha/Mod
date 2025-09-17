package com.sophia.easyforum.gui.handlers;

import com.sophia.easyforum.asm.event.ClientPreChatEvent;
import com.sophia.easyforum.command.ForumCommandLogic;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventHandler {
    @SubscribeEvent
    public void onPreChat(ClientPreChatEvent event) {
        String message = event.message;
        String lower = message.toLowerCase();

        if (lower.equals("/forum") || lower.startsWith("/forum ") || lower.equals("/f") || lower.startsWith("/f ")) {
            event.setCanceled(true);

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