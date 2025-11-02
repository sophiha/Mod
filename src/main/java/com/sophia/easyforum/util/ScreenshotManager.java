package com.sophia.easyforum.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public final class ScreenshotManager {
    private static final AtomicReference < String > LAST_SCREENSHOT_BASE64 = new AtomicReference < > ();

    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    private ScreenshotManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void captureAndStore() {
        try {
            BufferedImage img = createScreenshot();

            String b64 = convertImageToBase64(img);

            LAST_SCREENSHOT_BASE64.set(b64);
        } catch (Throwable t) {
            LAST_SCREENSHOT_BASE64.set(null);

        }
    }

    public static String getLastScreenshotBase64() {
        return LAST_SCREENSHOT_BASE64.get();
    }

    @Deprecated // use captureAndStore() p capturar do frame atual
    public static void setLastScreenshotBase64(String base64) {
        LAST_SCREENSHOT_BASE64.set(base64);
    }

    public static void clearLastScreenshot() {
        LAST_SCREENSHOT_BASE64.set(null);
    }

    public static boolean hasScreenshot() {
        return LAST_SCREENSHOT_BASE64.get() != null;
    }

    private static String convertImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ImageIO.write(image, "png", os);

        return Base64.getEncoder().encodeToString(os.toByteArray());
    }

    private static BufferedImage createScreenshot() {
        Minecraft mc = Minecraft.getMinecraft();

        Framebuffer fb = mc.getFramebuffer();

        ScreenshotDimensions dims = calculateScreenshotDimensions(mc, fb);

        preparePixelBuffers(dims.pixelCount);

        capturePixels(fb, dims);

        return createBufferedImage(fb, dims);
    }

    private static ScreenshotDimensions calculateScreenshotDimensions(Minecraft mc, Framebuffer fb) {
        int w = mc.displayWidth;
        int h = mc.displayHeight;

        if (OpenGlHelper.isFramebufferEnabled()) {
            w = fb.framebufferTextureWidth;
            h = fb.framebufferTextureHeight;
        }

        return new ScreenshotDimensions(w, h, w * h);
    }

    private static void preparePixelBuffers(int pixelCount) {
        if (pixelBuffer == null || pixelBuffer.capacity() < pixelCount) {
            pixelBuffer = BufferUtils.createIntBuffer(pixelCount);
            pixelValues = new int[pixelCount];
        }

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        pixelBuffer.clear();
    }

    private static void capturePixels(Framebuffer fb, ScreenshotDimensions dims) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fb.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        } else {
            GL11.glReadPixels(0, 0, dims.width, dims.height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        }

        pixelBuffer.get(pixelValues);

        TextureUtil.processPixelValues(pixelValues, dims.width, dims.height);
    }

    private static BufferedImage createBufferedImage(Framebuffer fb, ScreenshotDimensions dims) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            return createFramebufferImage(fb);
        } else {
            return createDirectImage(dims);
        }
    }

    private static BufferedImage createFramebufferImage(Framebuffer fb) {
        BufferedImage image = new BufferedImage(fb.framebufferWidth, fb.framebufferHeight, BufferedImage.TYPE_INT_RGB);

        int offset = fb.framebufferTextureHeight - fb.framebufferHeight;

        for (int y = offset; y < fb.framebufferTextureHeight; ++y) {
            for (int x = 0; x < fb.framebufferWidth; ++x) {
                int idx = y * fb.framebufferTextureWidth + x;
                image.setRGB(x, y - offset, pixelValues[idx]);
            }
        }

        return image;
    }

    private static BufferedImage createDirectImage(ScreenshotDimensions dims) {
        BufferedImage image = new BufferedImage(dims.width, dims.height, BufferedImage.TYPE_INT_RGB);
        
        image.setRGB(0, 0, dims.width, dims.height, pixelValues, 0, dims.width);

        return image;
    }

    private static final class ScreenshotDimensions {
        final int width, height, pixelCount;

        ScreenshotDimensions(int w, int h, int pc) {
            this.width = w;
            this.height = h;
            this.pixelCount = pc;
        }
    }
}
