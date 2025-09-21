package com.sophia.easyforum;

import com.sophia.easyforum.gui.handlers.ChatEventHandler;
import com.sophia.easyforum.gui.handlers.GuiHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = EasyForum.MODID, version = EasyForum.VERSION, name = EasyForum.NAME, clientSideOnly = true)
@SideOnly(Side.CLIENT)
public final class EasyForum {
    public static final String MODID = "easyforum";
    public static final String VERSION = "2.0";
    public static final String NAME = "EasyForum";

    @Mod.EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new ChatEventHandler());
        MinecraftForge.EVENT_BUS.register(new GuiHandler());
    }
}
