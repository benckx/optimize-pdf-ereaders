package be.enceladus.scan2ereader.core.utils;

import be.enceladus.scan2ereader.core.ocr.model.Page;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import static java.awt.Color.WHITE;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.RenderingHints.*;

public class ImageUtils {

    private final static Logger logger = Logger.getLogger(ImageUtils.class.getName());

    private static final boolean DUMP_DEBUG_IMAGES = true;

    public static void dumpToImageFile(String fileName, Page page) {
        if (DUMP_DEBUG_IMAGES) {
            new Printer(page, fileName).dumpToImageFile();
        }
    }

    public static BufferedImage rotate(BufferedImage image, double angle) {
        float radianAngle = (float) Math.toRadians(-angle);

        float sin = (float) Math.abs(Math.sin(radianAngle));
        float cos = (float) Math.abs(Math.cos(radianAngle));

        int w = image.getWidth();
        int h = image.getHeight();

        int newWidth = Math.round(w * cos + h * sin);
        int newHeight = Math.round(h * cos + w * sin);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();

        BufferedImage result = gc.createCompatibleImage(newWidth, newHeight, Transparency.OPAQUE);
        Graphics2D g = result.createGraphics();
        g.setColor(WHITE);
        g.fillRect(0, 0, result.getWidth(), result.getHeight());

        //-----------------------MODIFIED--------------------------------------
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        // g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);

        AffineTransform at = AffineTransform.getTranslateInstance((newWidth - w) / 2, (newHeight - h) / 2);
        at.rotate(radianAngle, w / 2, h / 2);
        //---------------------------------------------------------------------

        g.drawRenderedImage(image, at);
        g.dispose();

        return result;
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static BufferedImage resize(BufferedImage bufferedImage, double ratio, int bufferedImageType) {
        return resize(bufferedImage, (int) (ratio * bufferedImage.getWidth()), (int) (ratio * bufferedImage.getHeight()), bufferedImageType);
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH, int bufferedImageType) {
//        if (newW <= 0 || newH <= 0) {
//            logger.severe("illegal values [" + newW + ", " + newH + "]");
//            return img;
//        }

        Image tmp = img.getScaledInstance(newW, newH, SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, bufferedImageType);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    // https://stackoverflow.com/questions/28439136/java-image-compression-for-any-image-formatjpg-png-gif
    public static BufferedImage compress(BufferedImage image, float quality) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();

            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            // Check if canWriteCompressed is true
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            // End of check
            writer.write(null, new IIOImage(image, null, null), param);

            byte[] bytes = os.toByteArray();
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            logger.warning(e.toString());
            return image;
        }
    }
}
