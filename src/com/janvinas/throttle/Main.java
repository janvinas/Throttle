package com.janvinas.throttle;

import com.bergerkiller.bukkit.tc.properties.CartProperties;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.boss.*;

import java.util.HashMap;

public class Main extends JavaPlugin{

    Float accelerationPerIncrement = 0.001F;    //acceleration per throttle increment in blocks/tick^2
    //stores acceleration only for players that have executed the command
    HashMap<Player, Integer> acceleration = new HashMap<Player, Integer>();
    //stores speed calculated every tick from acceleration and previous speed
    HashMap<Player, Float> speed = new HashMap<Player, Float>();
    //stores a bossbar for every player that has throttle turned on
    HashMap<Player, BossBar> bar = new HashMap<Player, BossBar>();

    @Override
    public void onEnable(){

        //create scheduler that will run a task every tick
        BukkitScheduler scheduler = getServer().getScheduler();

        //schedule a task that will update the speed of trains every tick.
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                //run for every player with throttle turned on (exists in acceleration hashmap)
                for(Player p : acceleration.keySet()){
                    //get properties of the cart the player is currently editing
                    CartProperties cprop = CartProperties.getEditing(p);

                    //check if we got cart properties
                    if(cprop != null) {
                        //if we can, get properties from the train the cart is part of
                        TrainProperties prop = cprop.getTrainProperties();
                        //save current speed in a variable to make working with it easier
                        float currentSpeed = speed.get(p);
                        //update speed according to current acceleration and acceleration per throttle increment
                        currentSpeed += (acceleration.get(p) * accelerationPerIncrement);
                        //make sure speed is between 0 and 1
                        if(currentSpeed > 1) currentSpeed = 1;
                        if(currentSpeed < 0) currentSpeed = 0;
                        //update speed limit of the train
                        prop.setSpeedLimit(currentSpeed);
                        //update new current speed
                        speed.put(p, currentSpeed);

                        //update bar progress and title
                        bar.get(p).setProgress(currentSpeed);
                        bar.get(p).setTitle("Speed: " + String.format("%.02f", currentSpeed));
                    }

                }
            }
        }, 0, 1);     //will run every tick

    }

    @Override
    public void onDisable(){}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("throttle")){
            //if argument is "off" remove all player information to turn throttle off
            if(args.length == 1 && args[0].equalsIgnoreCase("off")) {
                //but do nothing if throttle was already off
                if (acceleration.remove((Player) sender) == null) {
                    sender.sendMessage(ChatColor.AQUA + "Throttle was already off. Set it to anything to turn it on.");
                }else{
                    //remove speed and acceleration hasmap entry
                    speed.remove((Player) sender);
                    acceleration.remove((Player) sender);
                    //remove player from bossbar
                    bar.get((Player) sender).removeAll();
                    //remove bossbar
                    bar.remove((Player) sender);
                    sender.sendMessage(ChatColor.AQUA + "Throttle turned off");
                }
                return true; //command executed successfully

            //if argument is an integer between 2 and 8
            }else if(args.length == 1 && Integer.parseInt(args[0]) <= 8 && Integer.parseInt(args[0]) >= 2){
                //argument = 5 is throttle = 0
                Integer currentAcceleration = Integer.parseInt(args[0]) - 5;
                //if player entry doesn't exist in hashmap, add it and create bossbar
                if(speed.get((Player) sender) == null) {
                    speed.put((Player) sender, 0F);
                    //puts a new bossbar in hashmap:
                    bar.put( (Player) sender, Bukkit.createBossBar("Speed", BarColor.YELLOW, BarStyle.SEGMENTED_10) );
                    bar.get((Player) sender).addPlayer((Player) sender);
                }
                float currentSpeed = speed.get((Player) sender);
                acceleration.put((Player) sender, currentAcceleration);
                sender.sendMessage(ChatColor.AQUA + "Throttle set to " + currentAcceleration.toString() + ". Speed is now " + String.format("%.02f", currentSpeed));
                return true;
            }else{
                sender.sendMessage(ChatColor.AQUA + "Incorrect argument provided. Provide an integer between 2 and 8. 5 is neutral.");
                return false;   //command failed to execute
            }

        }
        return false;
    }


}