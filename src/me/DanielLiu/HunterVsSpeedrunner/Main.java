package me.DanielLiu.HunterVsSpeedrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

//ctrl+shift+o to automatically import
//make runner to hunter deaths do something

public class Main extends JavaPlugin implements Listener{
	//Hardcore: True if runner cannot be killed by environment, false if runner can be killed by the environment (not killed by player does not reset run)
	private boolean hardcore = false;
	//list of all the hunters
	private ArrayList<Player> hunters = new ArrayList<Player>();
	//list of all the runner
	private ArrayList<Player> runners = new ArrayList<Player>();
	//map containing player and the number of times no enderpearl dropped from enderman, should be either 1 or 0
	private Map<Player, Integer> missedPearls = new HashMap<>();
	//map containing player and the number of times no rods dropped from blaze, should be either 1 or 0
	private Map<Player, Integer> missedRods = new HashMap<>();
	//map containing player and number of times no flint dropped from gravel, should be between 0 and 4 inclusive
	private Map<Player, Integer> missedFlint = new HashMap<>();
	//integer containing global missed trades (trade event does not give the player that trades with piglin)
	private Map<Player, Integer> missedTrades = new HashMap<>();
	
	//random function for some drop loot randomization
	Random random = new Random();
	
	@Override
	public void onEnable()
	{
		//server startup, reloads, plugin reloads
		getServer().getPluginManager().registerEvents(this, this);
	}
	@Override
	public void onDisable()
	{
		//shutdown, reloads, plugin reloads
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) //resets all of the player missed items on join, just to prevent any glitches/bugs
	{
		Player player = (Player)event.getPlayer();
		//remove player pearl information
		if (!missedPearls.containsKey(player))
			missedPearls.put(player, 0);
		else
			missedPearls.replace(player, 0);
		if (!missedTrades.containsKey(player))
			missedTrades.put(player, 0);
		else
			missedTrades.replace(player, 0);
		//remove player rod information
		if (!missedRods.containsKey(player))
			missedRods.put(player, 0);
		else
			missedRods.replace(player, 0);
		//remove player flint information
		if (!missedFlint.containsKey(player))
			missedFlint.put(player, 0);
		else
			missedFlint.replace(player, 0);
	}
	
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) //when the player quits, remove player from all missed items and hunter/runner teams to prevent any glitches/bugs
	{
		Player player = event.getPlayer();
		if (player!=null)
		{
			//remove players from item drop data
			missedPearls.remove(player);
			missedRods.remove(player);
			missedFlint.remove(player);
			
			//remove player from hunters/runner
			for (int i=0; i<hunters.size(); i++)
			{
				if (player.equals(hunters.get(i)))
				{
					hunters.remove(i);
					i--;
				}
			}
			for (int i=0; i<runners.size(); i++)
			{
				if (player.equals(runners.get(i)))
				{
					runners.remove(i);
					i--;
				}
			}
		}
		//debug message, should be removed (maybe)
		getServer().broadcastMessage(player.getName() + " has been removed from the hunters and/or runners team");
	}

	@EventHandler
	public void onMobDeath(EntityDeathEvent event)
	{
		Entity killer = event.getEntity().getKiller();
		Entity mob = event.getEntity();
		
		//handle endermen killed by players
		if (mob instanceof Enderman)
		{
			if (killer instanceof Player)
			{
				
				List<ItemStack> drops = event.getDrops();
				ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL, 1+random.nextInt(2));
				boolean droppedPearl = false;
				//check drops to see if pearl drops
				for (int i=0; i<drops.size(); i++)
				{
					if (drops.get(i).isSimilar(enderPearl))
					{
						//gets enderpearl drop from enderman, reset the missedPearls to 0
						missedPearls.replace((Player)killer, 0);
						droppedPearl = true;
						break;
					}
				}
				//if a pearl is not dropped, the missed pearls counter increases.
				if (!droppedPearl)
				{
					missedPearls.replace((Player)killer, missedPearls.get((Player)killer) + 1);
					//if the counter equals 2, the enderman is guaranteed to drop 1-2 pearls
					if (missedPearls.get((Player)killer) >= 2)
					{
						missedPearls.replace((Player)killer, 0);
						event.getDrops().clear();
						event.getDrops().add(enderPearl);
					}
				}
			}
		}
		//handle blaze killed by players
		else if (mob instanceof Blaze)
		{
			List<ItemStack> drops = event.getDrops();
			ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD, 1);
			boolean droppedRod = false;
			//check drops to see if blaze rod drops
			for (int i=0; i<drops.size(); i++)
			{
				if (drops.get(i).isSimilar(blazeRod))
				{
					//gets blaze rod drop from blaze, reset the missedRods to 0
					missedRods.replace((Player)killer, 0);
					droppedRod = true;
					break;
				}
			}
			//if a rod is not dropped, the missed rods counter increases.
			if (!droppedRod)
			{
				missedRods.replace((Player)killer, missedRods.get((Player)killer) + 1);
				//if the counter equals 2, the blaze is guaranteed to drop 1-2 rods
				if (missedRods.get((Player)killer) >= 3)
				{
					missedRods.replace((Player)killer, 0);
					event.getDrops().clear();
					event.getDrops().add(blazeRod);
				}
			}
		}
		else if (mob instanceof Spider)
		{
			List<ItemStack> drops = event.getDrops();
			ItemStack string = new ItemStack(Material.STRING, 1+random.nextInt(2));
			boolean droppedString = false;
			for (int i=0; i<drops.size(); i++)
			{
				if (drops.get(i).isSimilar(string))
				{
					droppedString = true;
				}
			}
			if (!droppedString)
			{
				drops.add(string);
			}
		}
	}
	
	//Handle piglin trading (again, no player in event, so it is a global thing not per player like flint/pearls/rods)

	@EventHandler
	public void onPiglinTrade(PiglinBarterEvent event)
	{
		List<ItemStack> drops = event.getOutcome();
		boolean droppedGood = false;
		boolean droppedString = false;
		//check if the piglin either traded obsidian or enderpearls (the only good items tbh)
		ItemStack obsidian = new ItemStack(Material.OBSIDIAN, 1+random.nextInt(3));
		ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(5));
		ItemStack string = new ItemStack(Material.STRING, 1+random.nextInt(5));
		Piglin piglin = event.getEntity();
		Player closestPlayer = null;
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (player.getWorld().getName().endsWith("_nether"))
			{
				if (closestPlayer == null)
				{
					closestPlayer = player;
				}
				else if (closestPlayer.getLocation().distance(piglin.getLocation()) > player.getLocation().distance(piglin.getLocation()))
				{
					closestPlayer = player;
				}
			}
		}
		if (closestPlayer == null)
		{
			return;
		}
		for (int i=0; i<drops.size(); i++)
		{
			if (drops.get(i).isSimilar(obsidian) || drops.get(i).isSimilar(enderPearl))
			{
				missedTrades.replace(closestPlayer, 0);
				droppedGood = true;
				break;
			}
			else if (drops.get(i).isSimilar(string))
			{
				droppedString = true;
			}
		}
		if (!droppedGood)
		{
			missedTrades.replace(closestPlayer, missedTrades.get(closestPlayer) + 1);
			if (missedTrades.get(closestPlayer) >= 10)
			{
				event.getOutcome().clear();
				if (random.nextInt(2) == 1)
					event.getOutcome().add(enderPearl);
				else
					event.getOutcome().add(obsidian);
				missedTrades.replace(closestPlayer, 0);
			}
			else if (!droppedString)
			{
				int chance = random.nextInt(10);
				if (chance == 0)
				{
					event.getOutcome().clear();
					event.getOutcome().add(string);
				}
			}
		}
		else
		{
			missedTrades.replace(closestPlayer, 0);
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event)
	{
		ItemStack flint = new ItemStack(Material.FLINT);
		ItemStack gravel = new ItemStack(Material.GRAVEL);
		if (event.getBlock().getType().equals(Material.NETHER_GOLD_ORE)) 
		{
            event.getBlock().setType(Material.AIR);
            int numDrops = 5+random.nextInt(5);
            ItemStack goldNuggets = new ItemStack(Material.GOLD_NUGGET, numDrops);
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), goldNuggets); 
        }
		if (event.getBlock().getType().equals(Material.GRAVEL))
		{
			event.getBlock().setType(Material.AIR);
			int chance = random.nextInt(10);
			if (chance == 0)
			{
				missedFlint.replace(event.getPlayer(), 0);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), flint); 
			}
			else
			{
				missedFlint.replace(event.getPlayer(), missedFlint.get(event.getPlayer()) + 1);
				if (missedFlint.get(event.getPlayer().getPlayer()) >= 5)
				{
					missedFlint.replace(event.getPlayer(), 0);
					event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), flint); 
				}
				else
				{
					event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), gravel); 
				}
			}
		}
	}
	
	@EventHandler
	public void onEnderDragonDeath(EntityDeathEvent event) //Checks if runners kills the enderdragon
	{
		if (event.getEntity() instanceof EnderDragon) //If the entity that died was an enderdragon
		{
			if (event.getEntity().getKiller()!=null) //If a player killed the enderdragon (probably)
			{
				if (runners.indexOf(event.getEntity().getKiller())!=(-1)) //If the player who killed the enderdragon was a runner (may change later)
				{
					getServer().broadcastMessage("Runner " + event.getEntity().getKiller().getName() + " has killed the Enderdragon!"); //Broadcast message that runner has killed the enderdragon
				}
				else if (runners.size() > 0) //check if there are still runners left in the game
				{
					getServer().broadcastMessage("The EnderDragon has been slain. The Runners win!");
				}
			}
		}
	}
	
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		//Check if hunter was killed, if so give compass again
		if (hunters.indexOf(event.getPlayer())!=(-1))
		{
			ItemStack compass = new ItemStack(Material.COMPASS); //ItemStack of Hunter Compass
			ItemMeta compassMeta = compass.getItemMeta(); //ItemMeta of Hunter Compass
			compassMeta.setDisplayName(ChatColor.DARK_RED + "Hunter Compass");
			List<String> compassLore = new ArrayList<String>();
			compassLore.add("Right click to use");
			compassMeta.setLore(compassLore);
			compass.setItemMeta(compassMeta); //This just names the compass to Hunter Compass
			boolean containsCompass = false;
			if (event.getPlayer().getInventory() != null)
			{
				for (ItemStack i : event.getPlayer().getInventory())
				{
					if (i!=null)
					{
						if (i.getType().equals(Material.COMPASS) && i.getItemMeta().getDisplayName().equals(compassMeta.getDisplayName()))
						{
							containsCompass = true;
							break;
						}
					}
				}
			}
			if (!containsCompass)
				event.getPlayer().getInventory().addItem(compass); //Adds compass to Player inventory.
			return;
		}
	}
	
	@EventHandler
	public void onPlayerDeath (PlayerDeathEvent event)
	{
		//Checks if runner was killed
		if (hardcore==false)
		{
			if (runners.indexOf(event.getEntity())!=(-1) && event.getEntity().getKiller()!=null) //If the runner was killed and the killer was a player
			{
				if (hunters.indexOf(event.getEntity().getKiller())!=(-1)) //If the killer was a hunter
				{
					event.setDeathMessage("Runner " + event.getEntity().getName() + " was killed by " + event.getEntity().getKiller().getName()); //Print out that the runner was killed by a hunter
					for (int i=0; i<runners.size(); i++) //remove the player from the runner team
					{
						if (runners.get(i).equals(event.getEntity()))
						{
							runners.get(i).sendMessage("You are no longer a runner.");
							runners.remove(i);
							i--;
						}
					}
					if (runners.size()==0) //If there are no more runners, output this (if this happens, probably hunters have won the game)
					{
						getServer().broadcastMessage("There are no more runners in this game. Hunters win!");
					}
				}
			}
		}
		else if (hardcore==true) //If the runners cannot die at all
		{
			if (runners.indexOf(event.getEntity())!=(-1))
			{
				event.setDeathMessage("Runner " + event.getEntity().getName() + " was killed."); //Print out that the runner was killed
				for (int i=0; i<runners.size(); i++) //remove the player from the runner team
				{
					if (runners.get(i).equals(event.getEntity()))
					{
						runners.get(i).sendMessage("You are no longer a runner."); //message dead player that he is not a runner anymore
						runners.remove(i);
						i--;
					}
				}
				if (runners.size()==0) //If there are no more runners, output this (if this happens, probably hunters have won
				{
					getServer().broadcastMessage("There are no more runners in this game. Hunters win!");
				}
			}
		}
	}
	@EventHandler
	public void onCompassRightClick(PlayerInteractEvent event) //If the player clicked (I think, not sure but it works)
	{
		ItemStack compass = new ItemStack(Material.COMPASS); //ItemStack of Hunter Compass
		ItemMeta compassMeta = compass.getItemMeta(); //ItemMeta of Hunter Compass
		compassMeta.setDisplayName(ChatColor.DARK_RED + "Hunter Compass");
		List<String> compassLore = new ArrayList<String>();
		compassLore.add("Right click to use");
		compassMeta.setLore(compassLore);
		compass.setItemMeta(compassMeta);
		Player player = event.getPlayer();
		Action action = event.getAction();
		if ((action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK))) //If the player right clicked
		{
			if (event.getItem().getType().equals(Material.COMPASS) && event.getItem().getItemMeta().getDisplayName().equals(compassMeta.getDisplayName())) //checks if the item in hand while clicking is the compass named "Hunter Compass"
			{
				int closestRunnerIndex = -1; //Originally -1 so we know if there is no runner
				double distance=Integer.MAX_VALUE; //Largest number possible so that if there is a runner it will always be closer than this
				for (int i=0; i<runners.size(); i++) //Loop through all of the runners
				{
					//Finds the distance (no need to square root if we are just comparing)
					double curDistance = Math.pow(Math.abs(runners.get(i).getLocation().getX() - player.getLocation().getX()), 2) + Math.pow(Math.abs(runners.get(i).getLocation().getY() - player.getLocation().getY()), 2) + Math.pow(Math.abs(runners.get(i).getLocation().getZ() - player.getLocation().getZ()), 2);
					if (distance>curDistance && runners.get(i).getWorld().equals(player.getWorld())) //If the distance of the runner checked is closer than the distance than the previously closer runner
					{ //The runner has to be in the same dimension as the hunter (need to check still if it works)
						distance = curDistance; //updates the distance to the closest runner
						closestRunnerIndex = i; //updates the index of the closest runner (to access the closest runner, we do runners.get(closestRunnerIndex))
					}
				}
				if (closestRunnerIndex == -1) //If the closest runner index was never updated
				{
					player.sendMessage("No Player in this Dimension Found.");
				}
				else //If everything worked and a player was found
				{
					player.setCompassTarget(runners.get(closestRunnerIndex).getLocation()); //set the player compass target to the runner location
					player.sendMessage("Tracking Player " + runners.get(closestRunnerIndex).getName()); //These are just debug statements to show the runner's position and name (won't be there later)
					if (player.getWorld().getName().endsWith("_nether") || player.getWorld().getName().endsWith("_end"))
					{
						//changed to work with lodestone stuff
						CompassMeta compassLodestoneMeta = (CompassMeta)event.getItem().getItemMeta();
						compassLodestoneMeta.setDisplayName(ChatColor.DARK_RED + "Hunter Compass");
						compassLodestoneMeta.setLore(compassLore);
						compassLodestoneMeta.setLodestoneTracked(false);
						compassLodestoneMeta.setLodestone(runners.get(closestRunnerIndex).getLocation());
						event.getItem().setItemMeta(compassLodestoneMeta);
//						double runX = runners.get(closestRunnerIndex).getLocation().getX();
//						double runZ = runners.get(closestRunnerIndex).getLocation().getZ();
//						double huntX = player.getLocation().getX();
//						double huntZ = player.getLocation().getZ();
//						String X = "";
//						String Z = "";
//						if (runX<huntX)
//						{
//							X = "W";
//						}
//						else
//						{
//							X = "E";
//						}
//						if (runZ<huntZ)
//						{
//							Z = "N";
//						}
//						else
//						{
//							Z = "S";
//						}
//						player.sendMessage("Direction: " + Z + X);
					}
				}
			}
			
		}
	}
	//where all of the commands are
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player sendPlayer = (Player)sender; //sendPlayer is the player who sent the command
		if (!(sender instanceof Player)) //commands only work for players
		{
			sender.sendMessage("Error: Sender is not a Player");
			return false;
		}
		if (label.equalsIgnoreCase("hunter")) //Player types in /hunter
		{
			if(args.length == 0)
			{
				if (hunters.indexOf(sendPlayer)==(-1)) //Makes sure that the player is not already included as a hunter
				{
					hunters.add(sendPlayer); //Adds the player to the list of hunters
					ItemStack compass = new ItemStack(Material.COMPASS); //ItemStack of Hunter Compass
					ItemMeta compassMeta = compass.getItemMeta(); //ItemMeta of Hunter Compass
					compassMeta.setDisplayName(ChatColor.DARK_RED + "Hunter Compass");
					List<String> compassLore = new ArrayList<String>();
					compassLore.add("Right click to use");
					compassMeta.setLore(compassLore);
					compass.setItemMeta(compassMeta);
					boolean containsCompass = false;
					if (sendPlayer.getInventory() != null)
					{
						for (ItemStack i : sendPlayer.getInventory())
						{
							if (i!= null)
							{
								if (i.getType().equals(Material.COMPASS) && i.getItemMeta().getDisplayName().equals(compassMeta.getDisplayName()))
								{
									containsCompass = true;
								}
							}
						}
					}
					if (!containsCompass)
					{
						sendPlayer.getInventory().addItem(compass); //Adds compass to Player inventory.
						sendPlayer.sendMessage("You are now a hunter. You should have recieved a compass.");
					}
					sendPlayer.sendMessage((String)("There are now " + hunters.size() + " hunters.")); //Probably debug statements
				}
				else //The player has already been registered as a hunter
				{
					sendPlayer.sendMessage("You are already a hunter!");
				}
				return true;
			}
			else
			{
				if (getServer().getPlayer(args[0]) == null)
				{
					sendPlayer.sendMessage("Player not found.");
					return false;
				}
				else
				{
					for (int i=0; i<hunters.size(); i++)
					{
						if (hunters.get(i).getName() == args[0])
						{
							sendPlayer.sendMessage("Player is already a hunter.");
							return false;
						}
					}
					hunters.add(getServer().getPlayer(args[0]));
					return true;
				}
			}
		}
		if (label.equalsIgnoreCase("runner")) //Player types in /runner
		{
			if(args.length == 0)
			{
				if (runners.indexOf(sendPlayer)==(-1)) //Checks that player is never a runner before
				{
					runners.add(sendPlayer); //Adds player to list of runners
					sendPlayer.sendMessage("You are now a runner."); //Debug statement probably
					sendPlayer.sendMessage((String)("There are now " + runners.size() + " runners.")); //Probably debug statement
				}
				else //Player has already been a runner before
				{
					sendPlayer.sendMessage("You are already a runner!");
				}
				return true;
			}
			else
			{
				if (getServer().getPlayer(args[0]) == null)
				{
					sendPlayer.sendMessage("Player not found.");
					return false;
				}
				else
				{
					for (int i=0; i<runners.size(); i++)
					{
						if (runners.get(i).getName() == args[0])
						{
							sendPlayer.sendMessage("Player is already a runner.");
							return false;
						}
					}
					runners.add(getServer().getPlayer(args[0]));
					return true;
				}
			}
		}
		if (label.equalsIgnoreCase("clearRoles")) //Player types in /clearRoles
		{
			if (args.length == 0)
			{
				for (int i=0; i<hunters.size(); i++) //checks if the player is in the hunters list, deletes it if it is
				{
					if (hunters.get(i).equals(sendPlayer))
					{
						hunters.remove(i);
						i--;
						sendPlayer.sendMessage("You are no longer a hunter");
					}
				}
				for (int i=0; i<runners.size(); i++) //checks if player is in the runners list, deletes it if it is
				{
					if (runners.get(i).equals(sendPlayer))
					{
						runners.remove(i);
						i--;
						sendPlayer.sendMessage("You are no longer a runner");
					}
				}
				sendPlayer.sendMessage((String)("There are now " + runners.size() + " runners.")); //Probably debug statements too
				sendPlayer.sendMessage((String)("There are now " + hunters.size() + " hunters."));
				if (runners.size()==0) //If there are no more runners, output this
				{
					getServer().broadcastMessage("There are no more runners in this game.");
				}
				return true;
			}
			else
			{
				if (getServer().getPlayer(args[0])==null)
				{
					sendPlayer.sendMessage("Player not found.");
				}
				else
				{
					for (int i=0; i<runners.size(); i++)
					{
						if (runners.get(i).equals(getServer().getPlayer(args[0])))
						{
							runners.get(i).sendMessage("You were removed from being a runner by " + sendPlayer.getName());
							runners.remove(i);
							i--;
						}
					}
					for (int i=0; i<hunters.size(); i++)
					{
						if (hunters.get(i).equals(getServer().getPlayer(args[0])))
						{
							hunters.get(i).sendMessage("You were removed from being a hunter by " + sendPlayer.getName());
							hunters.remove(i);
							i--;
						}
					}
				}
			}
		}
		if (label.equalsIgnoreCase("removeRunner")) //debug command to remove runner for bug issuese
		{
			if (args.length==0)
			{
				sendPlayer.sendMessage("Please specify a player!");
			}
			else
			{
				if (getServer().getPlayer(args[0])==null)
				{
					sendPlayer.sendMessage("Player not found.");
				}
				else
				{
					for (int i=0; i<runners.size(); i++)
					{
						if (runners.get(i).equals(getServer().getPlayer(args[0])))
						{
							runners.get(i).sendMessage("You were removed from being a runner by " + sendPlayer.getName());
							runners.remove(i);
							i--;
						}
					}
				}
			}
			return true;
		}
		if (label.equalsIgnoreCase("removeHunter")) //debug command to remove runner for bug issuese
		{
			if (args.length==0)
			{
				sendPlayer.sendMessage("Please specify a player!");
			}
			else
			{
				if (getServer().getPlayer(args[0])==null)
				{
					sendPlayer.sendMessage("Player not found.");
				}
				else
				{
					for (int i=0; i<hunters.size(); i++)
					{
						if (hunters.get(i).equals(getServer().getPlayer(args[0])))
						{
							hunters.get(i).sendMessage("You were removed from being a runner by " + sendPlayer.getName());
							hunters.remove(i);
							i--;
						}
					}
				}
			}
			return true;
		}
		if (label.equalsIgnoreCase("compass"))
		{
			ItemStack compass = new ItemStack(Material.COMPASS); //ItemStack of Hunter Compass
			ItemMeta compassMeta = compass.getItemMeta(); //ItemMeta of Hunter Compass
			compassMeta.setDisplayName(ChatColor.DARK_RED + "Hunter Compass");
			List<String> compassLore = new ArrayList<String>();
			compassLore.add("Right click to use");
			compassMeta.setLore(compassLore);
			compass.setItemMeta(compassMeta);
			boolean containsCompass = false;
			if (sendPlayer.getInventory() != null)
			{
				for (ItemStack i : sendPlayer.getInventory())
				{
					if (i!=null)
					{
						if (i.getType().equals(Material.COMPASS) && i.getItemMeta().getDisplayName().equals(compassMeta.getDisplayName()))
						{
							containsCompass = true;
							break;
						}
					}
				}
			}
			if (!containsCompass)
			{
				sendPlayer.getInventory().addItem(compass); //Adds compass to Player inventory.
				sendPlayer.sendMessage("You have been given a compass.");
			}
			return true;
		}
		if (label.equalsIgnoreCase("resetrun")) //reset the run
		{
			runners.clear();
			hunters.clear();
			getServer().broadcastMessage("Please select either a hunter (/hunter) or runner (/runner)");
			getServer().dispatchCommand(getServer().getConsoleSender(), "gamerule doDaylightCycle false");
			getServer().dispatchCommand(getServer().getConsoleSender(), "time set day");
			getServer().dispatchCommand(getServer().getConsoleSender(), "weather clear");
			for (Player player : getServer().getOnlinePlayers())
			{
				player.getInventory().clear();
				player.setGameMode(GameMode.SURVIVAL);
				player.setHealth(20);
				player.setFoodLevel(20);
				player.setSaturation(20);
				player.setLevel(0);
				player.setExp(0);
				player.teleport(getServer().getWorld("world").getSpawnLocation());
			}
			return true;
		}
		if (label.equalsIgnoreCase("startrun")) //starts the run
		{
			if (args.length == 0)
			{
				sendPlayer.sendMessage("Please specify a headstart time!");
			}
			else
			{
				getServer().dispatchCommand(getServer().getConsoleSender(), "/gamerule doDaylightCycle true");
				getServer().broadcastMessage("The hunters will now be slowed down for " + Integer.valueOf(args[0]) + " seconds.");
				int time = Integer.valueOf(args[0]) * 20;
				for (Player player : hunters)
				{
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, time, 255));
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, time, 255));
					player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, time, 128));
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, time, 255));
					player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, time, 255));
				}
			}
			return true;
		}
		if (label.equalsIgnoreCase("hardcoremode")) //set hardcoremode, true if runners cannot die to environment, false if runners can
		{
			if (args.length == 0)
			{
				if (hardcore==false)
				{
					sendPlayer.sendMessage("False");
				}
				else
				{
					sendPlayer.sendMessage("True");
				}
			}
			else
			{
				if (args[0].equalsIgnoreCase("true"))
				{
					hardcore = true;
				}
				else if (args[0].equalsIgnoreCase("false"))
				{
					hardcore = false;
				}
				else
				{
					sendPlayer.sendMessage("Your input was not valid. Please enter either true or false");
				}
			}
			return true;
		}
		if (label.equalsIgnoreCase("listroles"))
		{
			sendPlayer.sendMessage("There are " + hunters.size() + " hunters.");
			sendPlayer.sendMessage("There are " + runners.size() + " runners.");
			String totalHunters = "Hunters: \n";
			for (Player player : hunters)
			{
				totalHunters += player.getName();
				totalHunters += "\n";
			}
			sendPlayer.sendMessage(totalHunters);
			String totalRunners = "Runners: \n";
			for (Player player : runners)
			{
				totalRunners += player.getName();
				totalRunners += "\n";
			}
			sendPlayer.sendMessage(totalRunners);
		}
		if (label.equalsIgnoreCase("removeallrunners")) //debug command to reset runners
		{
			runners.clear();
			sendPlayer.sendMessage("There are " + runners.size() + " runners now.");
			return true;
		}
		if (label.equalsIgnoreCase("removeallhunters")) //debug command to reset hunters
		{
			hunters.clear();
			sendPlayer.sendMessage("There are " + hunters.size() + " hunters now.");
			return true;
		}
		if (label.equalsIgnoreCase("missedPearls")) //debug command to check pearl luck
		{
			sendPlayer.sendMessage("You have missed " + missedPearls.get(sendPlayer) + " ender pearls.");
			return true;
		}
		if (label.equalsIgnoreCase("missedRods")) //debug command to check rod luck
		{
			sendPlayer.sendMessage("You have missed " + missedRods.get(sendPlayer) + " blaze rods.");
			return true;
		}
		if (label.equalsIgnoreCase("missedTrades")) //debug command to check trade luck
		{
			sendPlayer.sendMessage("You have missed " + missedTrades.get(sendPlayer) + " obsidian/pearl trades.");
			return true;
		}
		if (label.equalsIgnoreCase("missedFlint")) //debug command to check flint luck
		{
			sendPlayer.sendMessage("You have missed " + missedFlint.get(sendPlayer) + " flint drops.");
			return true;
		}
		if (label.equalsIgnoreCase("resetMisses"))
		{
			missedPearls.clear();
			missedFlint.clear();
			missedTrades.clear();
			missedRods.clear();
			for (Player player : Bukkit.getOnlinePlayers())
			{
				missedPearls.put(player, 0);
				missedFlint.put(player, 0);
				missedTrades.put(player, 0);
				missedRods.put(player, 0);
			}
			getServer().broadcastMessage(sendPlayer.getName() + " reset misses for everyone");
			return true;
		}
	 	return false;
	}
}

