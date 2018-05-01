import org.apache.commons.cli.*;
import org.imgscalr.Scalr;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Render {

    private static final int Y_MARGIN = 16;

    private static TextLayout fitWidth(FontRenderContext frc, final String text, final Font font, final int width) {
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

    public static void main(String[] args) throws IOException, FontFormatException, ParseException {
        System.setProperty("java.awt.headless", "true");
        assert GraphicsEnvironment.isHeadless();

        Options options = new Options();
        options.addOption(Option.builder("w").longOpt("width").hasArg().required().desc("Width").type(Number.class).build());
        options.addOption(Option.builder("h").longOpt("height").hasArg().required().desc("Height").type(Number.class).build());
        options.addOption(Option.builder("q").longOpt("quality").hasArg().required().desc("Quality").type(Number.class).build());
        options.addOption(Option.builder("f").longOpt("font").hasArg().required().desc("Font").build());
        options.addOption(Option.builder("i").longOpt("input").hasArg().required().desc("Input").build());
        options.addOption(Option.builder("t").longOpt("text").hasArg().required().desc("Text").build());

        CommandLine cmdline = new DefaultParser().parse(options, args);

        final float quality = ((Number) cmdline.getParsedOptionValue("q")).floatValue();

        System.err.println(System.currentTimeMillis() + " parse");
        Canvas canvas = new Canvas();
        canvas.setSize(
                ((Number) cmdline.getParsedOptionValue("w")).intValue(),
                ((Number) cmdline.getParsedOptionValue("h")).intValue());

        BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setBackground(Color.WHITE);
        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setColor(Color.BLACK);
        System.err.println(System.currentTimeMillis() + " canvasImageGraphics");

        FontRenderContext frc = g.getFontMetrics().getFontRenderContext();
        Font inputFont = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(new File(cmdline.getOptionValue("f"))));
        System.err.println(System.currentTimeMillis() + " fontLoaded");

        BufferedImage bgRaw = ImageIO.read(new File(cmdline.getOptionValue("i")));
        BufferedImage bg = Scalr.resize(bgRaw, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, canvas.getWidth(), canvas.getHeight());
        g.drawImage(bg, 0, 0, null);
        System.err.println(System.currentTimeMillis() + " bgLoaded");

        final int x = canvas.getWidth() / 2;
        int y = 128;

        for (String line : cmdline.getOptionValue("t").split("\n")) {
            TextLayout layout = fitWidth(frc, line, inputFont, canvas.getWidth() / 3);
            Rectangle bounds = layout.getPixelBounds(frc, 0, 0);
            drawShadow(layout, g, -bounds.x + x - bounds.width / 2, -bounds.y + y, 0, 3);
            y += bounds.height + Y_MARGIN;
        }
        System.err.println(System.currentTimeMillis() + " textDrawn");

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ImageIO.createImageOutputStream(System.out));
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);
        IIOImage outputImage = new IIOImage(image, null, null);
        writer.write(null, outputImage, params);
        writer.dispose();
        System.err.println(System.currentTimeMillis() + " image wrote");
    }

    private static void drawShadow(TextLayout layout, Graphics2D g, int x, int y, int dx, int dy) {
        g.setColor(new Color(0, 0, 0, .5f));
        layout.draw(g, x + dx, y + dy);
        g.setColor(Color.WHITE);
        layout.draw(g, x, y);
    }
}
