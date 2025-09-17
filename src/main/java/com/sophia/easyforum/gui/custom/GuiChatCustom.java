package com.sophia.easyforum.gui.custom;

import com.sophia.easyforum.util.ScreenshotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Base64;

public final class GuiChatCustom extends GuiChat {
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 28) {
            handleEnterKey();
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void handleEnterKey() {
        String message = this.inputField.getText();

        if (isForumCommand(message)) {
            if (hasCommandArgs(message)) {
                captureScreenshot();
            } else {
                clearScreenshot();
            }
        }
    }

    private boolean isForumCommand(String message) {
        String lower = message.toLowerCase();

        return lower.equals("/forum") || lower.startsWith("/forum ") || lower.equals("/f") || lower.startsWith("/f ");
    }

    private boolean hasCommandArgs(String message) {
        String lower = message.toLowerCase();

        if (lower.startsWith("/forum ")) {
            return message.length() > "/forum ".length();
        } else if (lower.startsWith("/f ")) {
            return message.length() > "/f ".length();
        }

        return false;
    }

    private void captureScreenshot() {
        try {
            BufferedImage screenshot = createScreenshot();

            String base64Image = convertImageToBase64(screenshot);

            ScreenshotManager.setLastScreenshotBase64(base64Image);
        } catch (Exception e) {
            ScreenshotManager.clearLastScreenshot();
        }
    }

    private void clearScreenshot() {
        ScreenshotManager.clearLastScreenshot();
    }

    private String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageIO.write(image, "png", outputStream);

        byte[] imageBytes = outputStream.toByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private BufferedImage createScreenshot() {
        Minecraft minecraft = Minecraft.getMinecraft();

        Framebuffer framebuffer = minecraft.getFramebuffer();

        ScreenshotDimensions dimensions = calculateScreenshotDimensions(minecraft, framebuffer);

        preparePixelBuffers(dimensions.pixelCount);

        capturePixels(framebuffer, dimensions);

        return createBufferedImage(framebuffer, dimensions);
    }

    private ScreenshotDimensions calculateScreenshotDimensions(Minecraft minecraft, Framebuffer framebuffer) {
        int width = minecraft.displayWidth;
        int height = minecraft.displayHeight;

        if (OpenGlHelper.isFramebufferEnabled()) {
            width = framebuffer.framebufferTextureWidth;
            height = framebuffer.framebufferTextureHeight;
        }

        return new ScreenshotDimensions(width, height, width * height);
    }

    private void preparePixelBuffers(int pixelCount) {
        if (pixelBuffer == null || pixelBuffer.capacity() < pixelCount) {
            pixelBuffer = BufferUtils.createIntBuffer(pixelCount);
            pixelValues = new int[pixelCount];
        }

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        pixelBuffer.clear();
    }

    private void capturePixels(Framebuffer framebuffer, ScreenshotDimensions dimensions) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        } else {
            GL11.glReadPixels(0, 0, dimensions.width, dimensions.height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        }

        pixelBuffer.get(pixelValues);

        TextureUtil.processPixelValues(pixelValues, dimensions.width, dimensions.height);
    }

    private BufferedImage createBufferedImage(Framebuffer framebuffer, ScreenshotDimensions dimensions) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            return createFramebufferImage(framebuffer);
        } else {
            return createDirectImage(dimensions);
        }
    }

    private BufferedImage createFramebufferImage(Framebuffer framebuffer) {
        BufferedImage image = new BufferedImage(framebuffer.framebufferWidth, framebuffer.framebufferHeight, BufferedImage.TYPE_INT_RGB);

        int offset = framebuffer.framebufferTextureHeight - framebuffer.framebufferHeight;

        for (int y = offset; y < framebuffer.framebufferTextureHeight; ++y) {
            for (int x = 0; x < framebuffer.framebufferWidth; ++x) {
                int pixelIndex = y * framebuffer.framebufferTextureWidth + x;
                image.setRGB(x, y - offset, pixelValues[pixelIndex]);
            }

        }

        return image;
    }

    private BufferedImage createDirectImage(ScreenshotDimensions dimensions) {
        BufferedImage image = new BufferedImage(dimensions.width, dimensions.height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, dimensions.width, dimensions.height, pixelValues, 0, dimensions.width);

        return image;
    }

    private static class ScreenshotDimensions {
        final int width;
        final int height;
        final int pixelCount;

        ScreenshotDimensions(int width, int height, int pixelCount) {
            this.width = width;
            this.height = height;
            this.pixelCount = pixelCount;
        }
    }
}