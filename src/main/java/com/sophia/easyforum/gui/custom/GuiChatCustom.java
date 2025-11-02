package com.sophia.easyforum.gui.custom;

import com.sophia.easyforum.util.ScreenshotManager;
import net.minecraft.client.gui.GuiChat;

public final class GuiChatCustom extends GuiChat {

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 28) {
            handleEnterKey();
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void handleEnterKey() {
        String message = this.inputField.getText();

        if (isForumCommand(message)) {
            if (hasCommandArgs(message)) {
                ScreenshotManager.captureAndStore();
            } else {
                ScreenshotManager.clearLastScreenshot();
            }
        }
    }

    private boolean isForumCommand(String message) {
        String lower = message.toLowerCase();

        return lower.equals("/forum") || lower.startsWith("/forum ") || lower.equals("/f") || lower.startsWith("/f ");
    }

    private boolean hasCommandArgs(String message) {
        String lower = message.toLowerCase();

        if (lower.startsWith("/forum ")) return message.length() > "/forum ".length();
        if (lower.startsWith("/f ")) return message.length() > "/f ".length();

        return false;
    }
}
