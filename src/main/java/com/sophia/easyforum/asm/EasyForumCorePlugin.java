package com.sophia.easyforum.asm;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("EasyForumCore")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class EasyForumCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                ChatTransformer.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map < String, Object > data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}