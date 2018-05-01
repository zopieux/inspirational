import org.apache.commons.cli.*;
import org.imgscalr.Scalr;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Render {
    private static final int Y_MARGIN = 16;

    public static void main(String[] args) throws IOException, FontFormatException, ParseException {
        System.setProperty("java.awt.headless", "true");
        assert GraphicsEnvironment.isHeadless();

        Options options = new Options();
        options.addOption(Option.builder("w").longOpt("width").hasArg().required().desc("Width").type(Number.class).build());
        options.addOption(Option.builder("h").longOpt("height").hasArg().required().desc("Height").type(Number.class).build());
        options.addOption(Option.builder("q").longOpt("quality").hasArg().required().desc("Quality").type(Number.class).build());
        options.addOption(Option.builder("f").longOpt("font").hasArg().required().desc("Font").build());
        options.addOption(Option.builder("i").longOpt("input").hasArg().required().desc("Background image").build());
        options.addOption(Option.builder("t").longOpt("text").hasArg().required().desc("Text").build());

        CommandLine cmdline = new DefaultParser().parse(options, args);

        final int width = ((Number) cmdline.getParsedOptionValue("w")).intValue();
        final int height = ((Number) cmdline.getParsedOptionValue("h")).intValue();
        final float quality = ((Number) cmdline.getParsedOptionValue("q")).floatValue();
        final String fontName = cmdline.getOptionValue("f");
        final String inputImage = cmdline.getOptionValue("i");
        final String text = cmdline.getOptionValue("t");

        Canvas canvas = new Canvas();
        canvas.setSize(width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setBackground(Color.WHITE);
        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

        BufferedImage bgRaw = ImageIO.read(new File(inputImage));
        // TODO: this is not equivalent to Qt KeepAspectRatioByExpanding
        g.drawImage(Scalr.resize(bgRaw, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, width, height), 0, 0, null);

        FontRenderContext frc = g.getFontMetrics().getFontRenderContext();
        Font inputFont = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(new File(fontName)));

        final int x = width / 2;
        int y = 128;

        for (String line : text.split("\n")) {
            TextLayout layout = fitTextInWidth(frc, line, inputFont, width / 3);
            Rectangle bounds = layout.getPixelBounds(frc, 0, 0);
            drawStringShadow(layout, g, -bounds.x + x - bounds.width / 2, -bounds.y + y, 0, 3);
            y += bounds.height + Y_MARGIN;
        }

        // JPEG writer to stdout
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        writer.setOutput(ImageIO.createImageOutputStream(System.out));

        // adjust JPEG quality
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);

        writer.write(null, new IIOImage(image, null, null), params);
        writer.dispose();
    }

    private static TextLayout fitTextInWidth(FontRenderContext frc, final String text, final Font font, final int width) {
        TextLayout layout = null;
        float min = 1f, max = 1000f;
        while (max - min > 1f) {
            float size = (min + max) / 2f;
            layout = new TextLayout(text, font.deriveFont(size), frc);
            int fontWidth = layout.getPixelBounds(frc, 0, 0).width;
            if (Math.abs(fontWidth - width) <= 1) {
                return layout;
            } else if (fontWidth < width) {
                min = size;
            } else {
                max = size;
            }
        }
        return layout;
    }

    private static void drawStringShadow(TextLayout layout, Graphics2D g, int x, int y, int dx, int dy) {
        g.setColor(new Color(0, 0, 0, .5f));
        layout.draw(g, x + dx, y + dy);
        g.setColor(Color.WHITE);
        layout.draw(g, x, y);
    }
}
