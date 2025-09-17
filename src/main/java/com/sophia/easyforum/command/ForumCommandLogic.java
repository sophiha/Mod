package com.sophia.easyforum.command;

import com.sophia.easyforum.api.ForumApiService;
import com.sophia.easyforum.util.ScreenshotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;

public final class ForumCommandLogic {
    public static void execute(String[] args) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        if (!validateEnvironment(player)) return;
        if (!validateArguments(player, args)) return;
        if (!validateScreenshot(player)) return;

        String n = args[0].trim();
        String m = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

        String p = ScreenshotManager.getLastScreenshotBase64();
        ScreenshotManager.clearLastScreenshot();

        player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Sincronizando dados com o EasyForum..."));
        ForumApiService.sendReport(player, n, m, p);
    }

    private static boolean validateEnvironment(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.isSingleplayer()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Este comando só pode ser usado em servidor multiplayer."));
            return false;
        }

        ServerData serverData = minecraft.getCurrentServerData();

        if (serverData == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Falha ao obter informações do servidor."));
            return false;
        }

        if (!isMushDomain(serverData.serverIP)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Este comando não pode ser executado neste servidor."));
            return false;
        }

        return true;
    }

    private static boolean validateArguments(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].trim().isEmpty()) {
            showUsage(sender);
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Você precisa informar o(s) motivo(s)!"));
            return false;
        }

        return true;
    }

    private static boolean validateScreenshot(ICommandSender sender) {
        if (!ScreenshotManager.hasScreenshot()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Não foi possível capturar a imagem. Execute o comando novamente."));
            return false;
        }

        return true;
    }

    private static boolean isMushDomain(String serverIp) {
        if (serverIp == null || serverIp.trim().isEmpty()) {
            return false;
        }

        String[] domainParts = serverIp.toLowerCase().split("\\.");

        if (domainParts.length < 2 || domainParts.length > 3) {
            return false;
        }

        if (domainParts.length == 3 && "br".equals(domainParts[2]) && "com".equals(domainParts[1])) {
            return "mush".equals(domainParts[0]);
        }

        if (domainParts.length >= 3 && "com".equals(domainParts[domainParts.length - 1])) {
            return false;
        }

        return "mush".equals(domainParts[0]);
    }

    private static void showUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso incorreto!"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Ex: /forum Fulano ofensa"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Ex: /forum Beltrano,Ciclano ofensa"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Atalho disponível: /f"));
    }
}