package com.lukeonuke;

import com.lukeonuke.event.SignEventListener;
import com.lukeonuke.service.ConfigurationService;
import com.lukeonuke.service.DatabaseService;
import com.lukeonuke.service.ShopCreationService;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignShop implements ModInitializer {
	public static final String MOD_ID = "signshop";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("signshop starting...");
		LOGGER.info("Signshop is still in its early alpha stage. Every release gets used on the beocraft server, " +
				"so its stable enough. But if you find any errors, report em to https://github.com/LukeOnuke/signshop");
		LOGGER.info("	- Starting services");
		LOGGER.info("		* ConfigurationService");
		ConfigurationService.getInstance();
		LOGGER.info("		* DatabaseService");
		DatabaseService.getInstance();
		LOGGER.info("		* ShopCreationService");
		ShopCreationService.getInstance();
		LOGGER.info("	- OK");
		LOGGER.info("	- Registering events");
		SignEventListener listener = new SignEventListener();
		AttackBlockCallback.EVENT.register(listener);
		UseBlockCallback.EVENT.register(listener);
		PlayerBlockBreakEvents.BEFORE.register(listener);
		LOGGER.info("	- OK");
		LOGGER.info("signshop started!");
	}
}