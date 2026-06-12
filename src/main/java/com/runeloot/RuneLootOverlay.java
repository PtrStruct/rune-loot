package com.runeloot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

@Slf4j
class RuneLootOverlay extends Overlay
{
	private static final int PADDING = 8;
	private static final int ANIM_MS = 250;
	private static final int PINCH_W = 4;
	private static final int SLOT_SIZE = 36;

	private static final Color TEXT_COLOR = Color.WHITE;

	private static final DateTimeFormatter SCREENSHOT_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private enum AnimPhase { ENTER_V, ENTER_H, SHOWING, EXIT_H, EXIT_V }

	private final RuneLootConfig config;
	private final DrawManager drawManager;
	private final ConcurrentLinkedDeque<RuneLootNotification> queue = new ConcurrentLinkedDeque<>();
	private final BufferedImage holderImage;

	private RuneLootNotification current;
	private AnimPhase phase;
	private Instant phaseStart;

	@Inject
	RuneLootOverlay(RuneLootConfig config, DrawManager drawManager)
	{
		this.config = config;
		this.drawManager = drawManager;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		holderImage = loadHolder();
	}

	private static BufferedImage loadHolder()
	{
		try (InputStream is = RuneLootOverlay.class.getResourceAsStream("/com/runeloot/icon_holder.png"))
		{
			if (is == null) return null;
			BufferedImage raw = ImageIO.read(is);
			BufferedImage result = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int py = 0; py < raw.getHeight(); py++)
			{
				for (int px = 0; px < raw.getWidth(); px++)
				{
					int rgb = raw.getRGB(px, py);
					boolean isPink = ((rgb >> 16) & 0xFF) == 255 && ((rgb >> 8) & 0xFF) == 0 && (rgb & 0xFF) == 255;
					result.setRGB(px, py, isPink ? 0x00000000 : (0xFF000000 | (rgb & 0x00FFFFFF)));
				}
			}
			return result;
		}
		catch (IOException e)
		{
			log.warn("Could not load icon_holder.png", e);
			return null;
		}
	}

	void addNotification(RuneLootNotification notification)
	{
		queue.offer(notification);
	}

	void clearQueue()
	{
		queue.clear();
		current = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (current == null)
		{
			current = queue.poll();
			if (current == null) return null;
			startPhase(AnimPhase.ENTER_V);
		}

		long elapsed = Duration.between(phaseStart, Instant.now()).toMillis();
		switch (phase)
		{
			case ENTER_V: if (elapsed >= ANIM_MS) startPhase(AnimPhase.ENTER_H); break;
			case ENTER_H: if (elapsed >= ANIM_MS) startPhase(AnimPhase.SHOWING);  break;
			case SHOWING:  if (elapsed >= config.displayDuration() * 1000L) startPhase(AnimPhase.EXIT_H); break;
			case EXIT_H:  if (elapsed >= ANIM_MS) startPhase(AnimPhase.EXIT_V);  break;
			case EXIT_V:
				if (elapsed >= ANIM_MS)
				{
					current = queue.poll();
					if (current == null) return null;
					startPhase(AnimPhase.ENTER_V);
				}
				break;
		}

		return renderNotification(graphics, current);
	}

	private void startPhase(AnimPhase next)
	{
		phase = next;
		phaseStart = Instant.now();
		if (next == AnimPhase.SHOWING && config.screenshotEnabled() && current != null)
		{
			final String itemName = current.getItemName();
			drawManager.requestNextFrameListener(image -> saveScreenshot(itemName, image));
		}
	}

	private void saveScreenshot(String itemName, Image image)
	{
		BufferedImage screenshot = ImageUtil.bufferedImageFromImage(image);
		new Thread(() ->
		{
			try
			{
				File dir = new File(RuneLite.RUNELITE_DIR, "rune-loot/screenshots");
				dir.mkdirs();
				String safe = itemName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
				String ts = LocalDateTime.now().format(SCREENSHOT_TS);
				File out = new File(dir, safe + "_" + ts + ".png");
				ImageIO.write(screenshot, "PNG", out);
				log.debug("Rune Loot: screenshot saved to {}", out.getAbsolutePath());
			}
			catch (IOException e)
			{
				log.warn("Rune Loot: failed to save screenshot", e);
			}
		}, "rune-loot-screenshot").start();
	}

	private Dimension renderNotification(Graphics2D graphics, RuneLootNotification n)
	{
		Font originalFont = graphics.getFont();
		Font boldFont = originalFont.deriveFont(Font.BOLD);
		FontMetrics fm = graphics.getFontMetrics(originalFont);
		FontMetrics boldFm = graphics.getFontMetrics(boldFont);

		String title = n.isGeTradeable() ? config.tradeableTitle() : config.untradeableTitle();
		String name = n.getItemName();
		String valueText = config.showValue()
			? formatValue(n.getValue()) + (config.showGpSuffix() ? " gp" : "")
			: null;

		int textBlockW = valueText != null
			? Math.max(fm.stringWidth(name), fm.stringWidth(valueText))
			: fm.stringWidth(name);

		int titleBarH = boldFm.getHeight() + PADDING * 2;
		int contentW = PADDING + SLOT_SIZE + PADDING + textBlockW + PADDING;
		int fullW = Math.max(boldFm.stringWidth(title) + PADDING * 2, contentW);

		int textLines = 1 + (valueText != null ? 1 : 0);
		int textH = textLines * fm.getHeight();
		int contentH = Math.max(SLOT_SIZE, textH) + PADDING * 2;
		int fullH = titleBarH + contentH;

		long elapsed = Duration.between(phaseStart, Instant.now()).toMillis();
		double t = Math.min(1.0, elapsed / (double) ANIM_MS);
		double easeOut = 1.0 - Math.pow(1.0 - t, 3);
		double easeIn  = Math.pow(t, 3);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		switch (phase)
		{
			case ENTER_V:
			{
				int barH = Math.max(1, (int)(fullH * easeOut));
				graphics.setColor(config.borderColor());
				graphics.fillRect((fullW - PINCH_W) / 2, 0, PINCH_W, barH);
				break;
			}
			case ENTER_H:
			{
				int w = Math.max(1, PINCH_W + (int)((fullW - PINCH_W) * easeOut));
				Shape saved = graphics.getClip();
				graphics.clipRect((fullW - w) / 2, 0, w, fullH);
				drawContent(graphics, n, boldFont, fm, boldFm, title, name, valueText, fullW, fullH, titleBarH, contentH);
				drawBeveledBorder(graphics, config.borderColor(), fullW, fullH);
				graphics.setClip(saved);
				break;
			}
			case SHOWING:
			{
				drawContent(graphics, n, boldFont, fm, boldFm, title, name, valueText, fullW, fullH, titleBarH, contentH);
				drawBeveledBorder(graphics, config.borderColor(), fullW, fullH);
				break;
			}
			case EXIT_H:
			{
				int w = Math.max(PINCH_W, fullW - (int)((fullW - PINCH_W) * easeIn));
				Shape saved = graphics.getClip();
				graphics.clipRect((fullW - w) / 2, 0, w, fullH);
				drawContent(graphics, n, boldFont, fm, boldFm, title, name, valueText, fullW, fullH, titleBarH, contentH);
				drawBeveledBorder(graphics, config.borderColor(), fullW, fullH);
				graphics.setClip(saved);
				break;
			}
			case EXIT_V:
			{
				int barH = Math.max(1, fullH - (int)(fullH * easeIn));
				graphics.setColor(config.borderColor());
				graphics.fillRect((fullW - PINCH_W) / 2, 0, PINCH_W, barH);
				break;
			}
		}

		return new Dimension(fullW, fullH);
	}

	private void drawContent(
		Graphics2D g, RuneLootNotification n,
		Font boldFont, FontMetrics fm, FontMetrics boldFm,
		String title, String name, String valueText,
		int fullW, int fullH, int titleBarH, int contentH)
	{
		Font originalFont = g.getFont();

		g.setColor(config.backgroundColor());
		g.fillRect(0, 0, fullW, fullH);

		g.setColor(config.titleBarColor());
		g.fillRect(0, 0, fullW, titleBarH);

		applyVignette(g, 0, titleBarH, fullW, contentH);

		g.setColor(config.borderColor());
		g.drawLine(2, titleBarH, fullW - 3, titleBarH);

		g.setFont(boldFont);
		g.setColor(n.isGeTradeable() ? config.tradeableTitleColor() : config.untradeableTitleColor());
		int titleX = (fullW - boldFm.stringWidth(title)) / 2;
		int titleY = (titleBarH - boldFm.getHeight()) / 2 + boldFm.getAscent();
		g.drawString(title, titleX, titleY);
		g.setFont(originalFont);

		int slotY = titleBarH + (contentH - SLOT_SIZE) / 2;
		drawItemSlot(g, n.getIcon(), PADDING, slotY);

		int textLines = 1 + (valueText != null ? 1 : 0);
		int textH = textLines * fm.getHeight();
		int textX = PADDING + SLOT_SIZE + PADDING;
		int textY = titleBarH + (contentH - textH) / 2 + fm.getAscent();

		g.setColor(TEXT_COLOR);
		g.drawString(name, textX, textY);

		if (valueText != null)
		{
			g.setColor(getValueColor(n.getValue()));
			g.drawString(valueText, textX, textY + fm.getHeight());
		}
	}

	private void drawItemSlot(Graphics2D g, BufferedImage icon, int x, int y)
	{
		if (holderImage != null)
		{
			g.drawImage(holderImage, x, y, SLOT_SIZE, SLOT_SIZE, null);
		}
		else
		{
			g.setColor(new Color(61, 59, 54));
			g.fillRect(x, y, SLOT_SIZE, SLOT_SIZE);
			g.setColor(new Color(108, 101, 90));
			g.drawLine(x, y, x + SLOT_SIZE - 1, y);
			g.drawLine(x, y, x, y + SLOT_SIZE - 1);
			g.setColor(new Color(26, 24, 20));
			g.drawLine(x, y + SLOT_SIZE - 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1);
			g.drawLine(x + SLOT_SIZE - 1, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1);
		}

		if (icon != null)
		{
			int ix = x + (SLOT_SIZE - icon.getWidth()) / 2 + 2;
			int iy = y + (SLOT_SIZE - icon.getHeight()) / 2;
			g.drawImage(icon, ix, iy, null);
		}
	}

	private static void drawBeveledBorder(Graphics2D g, Color base, int w, int h)
	{
		g.setColor(darker(base, 2));
		g.drawRect(0, 0, w - 1, h - 1);

		g.setColor(base);
		g.drawRect(1, 1, w - 3, h - 3);
		g.drawRect(2, 2, w - 5, h - 5);

		g.setColor(lighter(base));
		g.drawLine(3, 3, w - 4, 3);
		g.drawLine(3, 4, 3, h - 4);

		g.setColor(darker(base, 1));
		g.drawLine(4, h - 4, w - 4, h - 4);
		g.drawLine(w - 4, 4, w - 4, h - 4);
	}

	private static void applyVignette(Graphics2D g, int x, int y, int w, int h)
	{
		float radius = Math.max(w, h) * 0.65f;
		RadialGradientPaint vignette = new RadialGradientPaint(
			x + w / 2f, y + h / 2f, radius,
			new float[]{ 0.2f, 1.0f },
			new Color[]{ new Color(0, 0, 0, 0), new Color(0, 0, 0, 110) }
		);
		Paint saved = g.getPaint();
		g.setPaint(vignette);
		g.fillRect(x, y, w, h);
		g.setPaint(saved);
	}

	private static Color lighter(Color c)
	{
		return new Color(
			Math.min(255, c.getRed() + 65),
			Math.min(255, c.getGreen() + 50),
			Math.min(255, c.getBlue() + 25),
			c.getAlpha()
		);
	}

	private static Color darker(Color c, int passes)
	{
		Color r = c;
		for (int i = 0; i < passes; i++)
		{
			r = new Color(
				Math.max(0, r.getRed() - 50),
				Math.max(0, r.getGreen() - 40),
				Math.max(0, r.getBlue() - 20),
				r.getAlpha()
			);
		}
		return r;
	}

	private String formatValue(long value)
	{
		if (!config.abbreviateValue())
		{
			return String.format("%,d", value);
		}
		if (value >= 1_000_000_000) return String.format("%.1fB", value / 1_000_000_000.0);
		if (value >= 1_000_000)     return String.format("%.1fM", value / 1_000_000.0);
		if (value >= 1_000)         return String.format("%.1fK", value / 1_000.0);
		return String.valueOf(value);
	}

	private Color getValueColor(long value)
	{
		if (value >= 1_000_000_000) return config.valueHighColor();
		if (value >= 10_000_000)    return config.valueMidColor();
		return config.valueLowColor();
	}
}