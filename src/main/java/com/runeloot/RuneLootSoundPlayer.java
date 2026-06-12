package com.runeloot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;

@Slf4j
class RuneLootSoundPlayer
{
	private final RuneLootConfig config;

	@Inject
	RuneLootSoundPlayer(RuneLootConfig config)
	{
		this.config = config;
	}

	void playAsync()
	{
		if (!config.soundEnabled()) return;

		String path = config.soundFilePath();
		File file = (path != null && !path.trim().isEmpty()) ? new File(path.trim()) : null;

		if (file != null && file.exists())
		{
			new Thread(() -> playFile(file), "rune-loot-sound").start();
		}
		else
		{
			if (file != null)
			{
				log.warn("Rune Loot: sound file not found at '{}', using built-in sound.", path);
			}
			new Thread(this::playBundled, "rune-loot-sound").start();
		}
	}

	private void playFile(File file)
	{
		try
		{
			new AudioPlayer().play(file, (float) config.soundVolume());
		}
		catch (Exception e)
		{
			log.warn("Rune Loot: failed to play sound file '{}'", file.getName(), e);
		}
	}

	private void playBundled()
	{
		try (InputStream is = RuneLootSoundPlayer.class.getResourceAsStream("/com/runeloot/notification.wav"))
		{
			if (is == null)
			{
				log.warn("Rune Loot: bundled notification.wav not found in resources.");
				return;
			}
			byte[] bytes = is.readAllBytes();
			new AudioPlayer().play(new ByteArrayInputStream(bytes), (float) config.soundVolume());
		}
		catch (Exception e)
		{
			log.warn("Rune Loot: failed to play bundled sound", e);
		}
	}
}