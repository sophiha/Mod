package com.sophia.easyforum.asm.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientPreChatEvent extends Event {
    public final String message;

    public ClientPreChatEvent(String message) {
        this.message = message;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}