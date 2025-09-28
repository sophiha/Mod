package com.sophia.easyforum;

import com.sophia.easyforum.gui.handler.GuiHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = EasyForum.MODID, version = EasyForum.VERSION, name = EasyForum.NAME, clientSideOnly = true)
@SideOnly(Side.CLIENT)
public final class EasyForum {
    public static final String MODID = "easyforum";
    public static final String VERSION = "3.0";
    public static final String NAME = "EasyForum";

    @Mod.EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new GuiHandler());
    }
}
