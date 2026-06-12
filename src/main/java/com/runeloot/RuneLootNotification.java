package com.runeloot;

import java.awt.image.BufferedImage;
import java.time.Instant;
import lombok.Getter;

@Getter
class RuneLootNotification
{
	private final String itemName;
	private final long value;
	private final BufferedImage icon;
	private final boolean geTradeable;
	private final Instant receivedAt;

	RuneLootNotification(String itemName, long value, BufferedImage icon, boolean geTradeable)
	{
		this.itemName = itemName;
		this.value = value;
		this.icon = icon;
		this.geTradeable = geTradeable;
		this.receivedAt = Instant.now();
	}
}