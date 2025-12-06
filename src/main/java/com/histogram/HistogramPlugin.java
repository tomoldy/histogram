package com.histogram;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.*;
import net.runelite.client.game.WorldService;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(ublic class Histog	@Inject
	private Client client; 
	@Inject
	private HistogramConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldService worldService;

	private HistogramOverlay histogramOverlay;
	private ScheduledExecutorService pingThreads;

	// track last time we saw combat so we can hide the overlay when idle
	private long lastCombatMillis;

	
	private int ping = -1;

	
	private static final int HOP_GAMESTATE = 45;

	@Override p
		
		histogramOverlay = new HistogramOverlay(config);
		// start with overlay showing and timer  	
		
		overlayManager.add(histogramOverlay);
		
		pingThreads = Executors.newScheduledT

		Override
	protected void shutDown()
	{
		overlayManager.remove(hi // kill ping threads to avoid leaks in dev runs
		if (pingThreads != null) 	{
			pingThreads.shutdownNow();
		}
	} Subscribe
	public void onGameTick(GameTick tick)
	{
		histogramOverlay.addEvent(EventType.TICK);

		if (config.useIdealTicks()) {
			histogramOverlay.addEvent(EventType 	}

		// simple combat check: if we have a target, keep the overlay alive long now = System.currentTimeMillis();
		if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() != null)
		{ 	markCombatActivity(now); }
		else if (config.overlayTimeoutEnabled())
		{ long elapsed = now - lastCombatMillis;
			if (elapsed > config.overlayTimeoutSeconds() * 1000L)
			{
			 	} } 	lse if (!config.overlayTimeoutEnabled() && !overlayVisib
			
			  }

		pingThreads.schedule(this::updatePing, 0, TimeUnit.SEC 

	@Subscribe
	pu String menuOption = removeFormatting(e.getMenuOption()); S
				
			
		i

			return;
		}

		if (menuOption.equals("Eat") || menuOption.equals("Drink")) {
			histogramOverlay.addEvent(EventType.EAT, getInputDelay(EventType.EAT), getServerDelay(EventType.EAT));
			return;
		}

		if (menuOption.equals("Walk here")) {
				
			histogramOverlay.addEvent(EventType.MOVE, getInputDelay(EventType.MOVE), getServerDelay(EventType.MOVE));
			return;
		}

		if (menuOption.equals("Use")) {
						
			if (menuTarget.equals("Special Attack")) {
				histogramOverlay.addEvent(EventType.SPECIAL_ATTACK, getInputDelay(EventType.SPECIAL_ATTACK), getServerDelay(EventType.SPECIAL_ATTACK));
				// special attack click counts as combat, wake overlay
				 	return;
			}
			else {
				histogramOverlay.addEvent(EventType.USE, getInputDelay(EventType.USE), getServerDelay(EventType.USE));
				return;
			}
		}

						
					
		if (removeFormatting(menuOption).equals("Use Special Attack")) {
			histogramOverlay.addEvent(EventType.SPECIAL_ATTACK, getInputDelay(EventType.SPECIAL_ATTACK), getServerDelay(EventType.SPECIAL_ATTACK));
			m return;
		}

					
		if (menuOption.equals("Attack")) {
			histogramOverlay.addEvent(EventType.ATTACK, getInputDelay(EventType.ATTACK), getServerDelay(EventType.ATTACK));
			markCombatActivity(System.currentTimeMillis());
			return;
					
		}

					
		if (menuOption.equals("Activate") || menuOption.equals("Deactivate")) {
			// 
			histogramOverlay.addEvent(EventType.PRAYER, getInputDelay(EventType.PRAYER),
					getServerDelay(EventType.PRAYER));
			// prayer toggles are a strong hint the player is “doing stuff”, so wake the overlay too
			markCombatActivity(System.currentTimeMillis());
			return;
		}
				

					
		if (handleCustomConfig(menuOption, menuTarget, config.custom1Interaction(), c
			// nfig.custom1Target(),
				EventType.CUSTOM_1))
			return;
		if (handleCustomConfig(menuOption, menuTarget, config.custom2Interaction(), config.custom2Target(),
				EventType.CUSTOM_2))
			return;
				
		if (handleCustomConfig(menuOption, menuTarget, config.custom3Interaction(), config.custom3Target(),
				EventType.CUSTOM_3))
				
			return;
		if (handleCustomConfig(menuOption, menuTarget, config.custom4Interaction(), config.custom4Target(),
				
				EventType.CUSTOM_4))
			return;
				
		if (handleCustomConfig(menuOption, menuTarget, config.custom5Interaction(), config.custom5Target(), EventType.CUSTOM_5))
			return;
				
	}
 @Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) 
		if (gameStateChanged.getGameState().getState() == HOP_GAMESTATE)
		{ 		checksTilPing = 0;
			// reset combat timer on hop/login so we don't instantly hide 	lastCombatMillis = System.currentTimeMillis();
			showOverlay();
		}
	}
 @Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{ // if we get hit, we are in combat, so keep the overlay awake
		if (client.getLocalPlayer() != null && event.getActor() == client.getLocalPlayer())
		{
			// even if timeout is disabled or we were hidden,  		markCombatActivity(System.currentTimeMillis());
		}
	} Provides
	HistogramConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HistogramConfig.class);
	} 
	private float getInputDelay(EventType event)
	{
		float delay = (ping / 1000f); 	return Math.min(delay, (config.pingMax() / 1000f));
	} 
	private void updatePing( 
		if (checksTilPing == 0)
		{ 		int currentping = send 
			if (currentping != -1)
			{
				ping = currentping;
			 	} }
		else 	{
			checksTilPing--; }
	} 
	private int sendPing()
	{ eturn Ping.ping(worldService.getWorlds().findWorld(client.getWorld()));
	} 
	private int getPlayerCount()
	{
		r   rivate String removeFormatting(String raw)
	{
		return raw.replaceAll("<[^>]*>", "");
	} 
	private float getServerDelay(EventType type)
	{ 	int playercou 
		witch (type
			
		case EQUI
			return config.equipConst() 	case EAT:
			return config.eatConst() / 1000f + (config.eatMult() / 1000f * playercount / 1000f);
		case MOVE
			return config.moveConst() / 1000f + (config.moveMult() / 1000f * playercount / 1000f
		case USE: 		return config.useConst() / 1000f + (config.useMult() / 1000f * playercount / 1000f);
		case ATTACK:
			return config.attackConst() / 1000f + (config.attackMult() / 1000f * playercount / 1000f);
		case SPECIAL
			return config.specialattackConst() / 1000f 	case PRAYER:
			return config.prayerConst() / 1000f + (config.prayerMult() / 1000f * playercount / 1000f);
		case CUSTOM_1:
			return confi c
		return conf
			se CUSTOM_3:
		return co
			se CUSTOM_4:
		return con
			se CUS
		return co
			efault:
			return 0;
			
		
			
		ivate boolea
			
		f (option.isEm
			eturn false; 
		f (option.isEm
			f (target.isEmpty() || target.equals(menuTarget))
		{
			histogramOverlay.addEvent(type, getInputDelay(type), getServerDelay(type));
			return true;
			
		
			
		eturn fa
			
 // helper to refresh combat timer and wake overlay if needed
	private void markCombatActivity(long now)
	{
		lastCombatMillis = now;
			 	showOverlay();
	} 
		rivate void showOverlay() 
			f (!overlayVisible)
		{ 
  }
		 r
			
		i
		
	
			histogramOverlay.setVisible(false);
		}
	}
}
  
		 
			
		
	

	 
		 
			
		
	

