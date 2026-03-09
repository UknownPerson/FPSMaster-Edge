package top.fpsmaster.utils.imaging;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.modules.logger.ClientLogger;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class AWTUtils {
    private static final HashMap<Integer, ResourceLocation[]> generated = new HashMap<>();
    private static final HashMap<String, ResourceLocation> generatedFull = new HashMap<>();

    public static ResourceLocation generateRoundImage(int width, int height, int radius) {
        return generateRoundImage(width, height, radius, 1.0f);
    }

    public static ResourceLocation generateRoundImage(int width, int height, int radius, float pixelScale) {
        if (width <= 0 || height <= 0 || radius < 0) {
            throw new IllegalArgumentException("Width, height must be positive and radius must be non-negative");
        }
        float density = Math.max(1.0f, pixelScale);
        int pixelWidth = Math.max(1, Math.round(width * density));
        int pixelHeight = Math.max(1, Math.round(height * density));
        int pixelRadius = Math.max(0, Math.round(radius * density));
        String cacheKey = pixelWidth + "/" + pixelHeight + "/" + pixelRadius;
        return generatedFull.computeIfAbsent(cacheKey, r -> {
            BufferedImage bufferedImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(new Color(0, 0, 0, 0));
                graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

                graphics2D.setComposite(AlphaComposite.SrcOver);
                graphics2D.setColor(Color.WHITE);
                RoundRectangle2D roundRectangle = new RoundRectangle2D.Float(0, 0, pixelWidth, pixelHeight, pixelRadius * 2f, pixelRadius * 2f);
                graphics2D.fill(roundRectangle);

                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.getTextureManager() == null) {
                    return null;
                }

                return mc.getTextureManager()
                        .getDynamicTextureLocation(r + "_full", new DynamicTexture(bufferedImage));
            } catch (Exception e) {
                ClientLogger.error("An error occurred while generating round texture: " + r);
                e.printStackTrace();
                return null;
            } finally {
                graphics2D.dispose();
            }
        });
    }

    public static ResourceLocation[] generateRound(int radius) {
        return generateRound(radius, 1.0f);
    }

    public static ResourceLocation[] generateRound(int radius, float pixelScale) {
        int pixelRadius = Math.max(1, Math.round(radius * Math.max(1.0f, pixelScale)));
        if (generated.get(pixelRadius) != null) {
            return generated.get(pixelRadius);
        }
        int radius2 = pixelRadius * 2;

        BufferedImage bufferedImage = new BufferedImage(radius2, radius2, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            String[] fileNames = {"lt.png", "rt.png", "lb.png", "rb.png"}; // 存储文件名
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(Color.decode("#00000000"));
            graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            RoundRectangle2D roundRectangle;

            int[] coordinates = {0, -radius2, 0, -radius2};
            int[] coordinates2 = {0, 0, -radius2, -radius2};
            ResourceLocation[] locations = new ResourceLocation[4];
            for (int i = 0; i < 4; i++) {
                graphics2D.setComposite(AlphaComposite.Clear);
                graphics2D.fillRect(0, 0, radius2, radius2);
                graphics2D.setComposite(AlphaComposite.SrcOver);
                graphics2D.setColor(Color.WHITE);
                roundRectangle = new RoundRectangle2D.Float(
                        coordinates[i],
                        coordinates2[i],
                        (radius2 * 2),
                        (radius2 * 2),
                        radius2,
                        radius2
                );
                graphics2D.fill(roundRectangle);

                locations[i] = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(pixelRadius + "_" + fileNames[i], new DynamicTexture(bufferedImage));
            }

            generated.put(pixelRadius, locations);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            graphics2D.dispose();
        }
        return generated.get(pixelRadius);
    }
}



