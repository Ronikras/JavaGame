package GUI;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to load and cache SVG images as BufferedImage.
 */
public class ImageLoader {
    private static final SVGUniverse svgUniverse = new SVGUniverse();
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ImageLoader.class);

    /**
     * Loads an SVG file and converts it into a BufferedImage.
     *
     * @param resourcePath path inside resources folder, e.g., "/figures/rabbit_gold.svg"
     * @param width        target width
     * @param height       target height
     * @return BufferedImage or null if failed
     */
    public static BufferedImage loadImage(String resourcePath, int width, int height) {
        //Return cached image if already loaded
        if (imageCache.containsKey(resourcePath)) {
            logger.debug("Image cache hit for resource: {}", resourcePath);
            return imageCache.get(resourcePath);
        }

        logger.debug("Loading SVG resource {} with size {}x{}", resourcePath, width, height);
        try {
            InputStream in = ImageLoader.class.getResourceAsStream(resourcePath);
            if (in == null) {
                //Resource not found on classpath
                logger.error("SVG not found: {}", resourcePath);
                return null;
            }

            //Load and parse the SVG document
            URI uri = svgUniverse.loadSVG(in, resourcePath);
            SVGDiagram diagram = svgUniverse.getDiagram(uri);

            if (diagram == null) {
                //Parsing failed
                logger.error("Failed to parse SVG: {}", resourcePath);
                return null;
            }

            //Prepare an img buffer with ARGB for transparency
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            //Configure the SVG diagram's viewport and clipping
            diagram.setIgnoringClipHeuristic(true);
            diagram.setDeviceViewport(new Rectangle(width, height));
            //Render the SVG into our graphics context
            diagram.render(g2d);
            g2d.dispose();

            //Cache and return the rendered image
            imageCache.put(resourcePath, image);
            logger.info("SVG loaded and cached: {}", resourcePath);
            return image;

        } catch (Exception e) {
            logger.error("Exception while loading SVG: {}", resourcePath, e);
            return null;
        }
    }
}
