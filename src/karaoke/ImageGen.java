package karaoke;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageGen {
	public static void main(String args[]) throws Exception {
		try {
			int width = 1920, height = 1080;

			// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
			// into integer pixels
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D ig2 = bi.createGraphics();
			RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			ig2.setRenderingHints(rh);

			Font font = new Font("TimesRoman", Font.BOLD, 72);
			ig2.setFont(font);
			
			ig2.setStroke(new BasicStroke(10f));
			
			String message = "These are the lyrics";
			FontMetrics fontMetrics = ig2.getFontMetrics();
			int stringWidth = fontMetrics.stringWidth(message);
			int stringHeight = fontMetrics.getAscent();
			ig2.setPaint(Color.black);
			ig2.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

			BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D ig3 = bi2.createGraphics();
			ig3.setRenderingHints(rh);

			ig3.setFont(font);
			ig3.setPaint(Color.BLUE);
			ig3.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

			BufferedImage dest = bi2.getSubimage(0, 0, 900, 1080);

			BufferedImage c = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);

			Graphics g = c.getGraphics();
			g.drawImage(bi, 0, 0, null);
			g.drawImage(dest, 0, 0, null);

			ImageIO.write(c, "PNG", new File("/Users/waylonh/Downloads/abcdef.png"));
		} catch (IOException ie) {
			ie.printStackTrace();
		}

	}
}
