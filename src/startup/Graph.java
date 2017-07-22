package startup;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Graph {

	final BufferedImage img;

	public Graph(int width, int height) {
		this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
	}

	public void draw(int ch, double[] func, double min, double max) {
		if (Double.isNaN(min) || Double.isNaN(max)) {
			min = Double.POSITIVE_INFINITY;
			max = Double.NEGATIVE_INFINITY;
			for (double d : func) {
				if (d < min) min = d;
				if (d > max) max = d;
			}
		}
		int h = img.getHeight(), w = img.getWidth();
		Graphics2D g = img.createGraphics();
		g.setColor(Color.decode(ch == 0 ? "0xc00000" : ch == 1 ? "0x00c000" : "0x0000c0"));
		g.drawString(String.format("%.3g", max), ch * 80, g.getFontMetrics().getAscent() + 1);
		g.drawString(String.format("%.3g", min), ch * 80, h - g.getFontMetrics().getDescent() - 1);
		g.dispose();
		WritableRaster raster = img.getRaster();
		double scale = (double)h / (max - min);
		if (Double.isInfinite(scale)) return;
		for (int i = 0; i < w; i++) {
			int y = h - (int)Math.ceil((func[i] - min) * scale);
			if (y >= 0 && y < h) raster.setSample(i, y, ch, 255);
		}
	}

	public void save(File file) {
		try {
			ImageIO.write(img, "PNG", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
