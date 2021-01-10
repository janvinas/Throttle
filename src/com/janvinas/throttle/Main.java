package com.janvinas.throttle;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.properties.CartProperties;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.boss.*;

import java.util.HashMap;

public class Main extends JavaPlugin{

    Float accelerationPerIncrement = 0.001F;    //acceleration per throttle increment in blocks/tick^2
    Integer maxSearchDistance = 300;

    //stores acceleration only for players that have executed the command
    HashMap<Player, Integer> acceleration = new HashMap<Player, Integer>();
    //stores speed calculated every tick from acceleration and previous speed
    HashMap<Player, Float> speed = new HashMap<Player, Float>();
    //stores a bossbar for every player that has throttle turned on
    HashMap<Player, BossBar> speedBar = new HashMap<Player, BossBar>();
    //stores another bossbar to show distance from train ahead
    HashMap<Player, BossBar> distanceBar = new HashMap<Player, BossBar>();

    @Override
    public void onEnable(){

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
                        speedBar.get(p).setProgress(currentSpeed);
                        speedBar.get(p).setTitle("Speed: " + String.format("%.02f", currentSpeed));
                    }
                }
            }
        }, 0, 1);     //will run every tick


        scheduler.scheduleSyncRepeatingTask(this, new Runnable(){
            @Override
            public void run() {
                for(Player p : distanceBar.keySet()){
                    CartProperties cprop = CartProperties.getEditing(p);
                    if(cprop != null){
                        distanceBar.get(p).setProgress(getDistanceAhead(p, maxSearchDistance) / maxSearchDistance);
                        distanceBar.get(p).setTitle("Train ahead: " + String.format( "%.02f", getDistanceAhead(p, maxSearchDistance) ) + " blocks");
                    }
                }
            }
        },0, 10); //will run every 10 ticks

    }

    @Override
    public void onDisable(){}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(command.getName().equalsIgnoreCase("throttle")){
            //if argument is "off" remove all player information to turn throttle off
            if(args.length == 1 && args[0].equalsIgnoreCase("off")) {
                //but do nothing if throttle was already off
                if (acceleration.remove((Player) sender) == null) {
                    sender.sendMessage(ChatColor.AQUA + "Throttle was already off. Set it to anything to turn it on.");
                } else {
                    //remove speed and acceleration hasmap entry
                    speed.remove((Player) sender);
                    acceleration.remove((Player) sender);
                    //remove player from bossbar
                    speedBar.get((Player) sender).removeAll();
                    //remove bossbar
                    speedBar.remove((Player) sender);

                    distanceBar.get(sender).removeAll();
                    distanceBar.remove(sender);

                    sender.sendMessage(ChatColor.AQUA + "Throttle turned off");
                }
                return true; //command executed successfully

            //if argument is "reverse"
            }else if(args.length == 1 && args[0].equalsIgnoreCase("reverse")){
                if(CartProperties.getEditing((Player) sender).getGroup() == null) {
                    sender.sendMessage(ChatColor.AQUA + "You are not editing any train!");
                    return true;
                }
                if(speed.get(sender) == null){
                    sender.sendMessage(ChatColor.AQUA + "Turn throttle on to perform this command");
                    return true;
                }
                if(speed.get(sender) != 0){
                    sender.sendMessage(ChatColor.AQUA + "Stop the train before reversing!");
                    return true;
                }
                CartProperties.getEditing((Player) sender).getGroup().reverse();
                sender.sendMessage(ChatColor.AQUA + "Train reversed successfully");
                return true;

            //if argument is an integer between 2 and 8
            }else if(args.length == 1 && Integer.parseInt(args[0]) <= 8 && Integer.parseInt(args[0]) >= 2) {
                //argument = 5 is throttle = 0
                Integer currentAcceleration = Integer.parseInt(args[0]) - 5;
                //if player entry doesn't exist in hashmap, add it and create bossbar
                if (speed.get((Player) sender) == null) {
                    speed.put((Player) sender, 0F);
                    //puts a new bossbar in hashmap:
                    speedBar.put((Player) sender, Bukkit.createBossBar("Speed", BarColor.YELLOW, BarStyle.SEGMENTED_10));
                    speedBar.get((Player) sender).addPlayer((Player) sender);

                    distanceBar.put((Player) sender, Bukkit.createBossBar("Obstacle ahead: ? blocks", BarColor.RED, BarStyle.SOLID));
                    distanceBar.get((Player) sender).addPlayer((Player) sender);
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

    public double getDistanceAhead(Player p, int d){
        MinecartGroup group = CartProperties.getEditing(p).getGroup();
        if(group.findObstacleAhead(d) == null) return 0;
        if(group.findObstacleAhead(d).distance > maxSearchDistance) return 0;
        return group.findObstacleAhead(d).distance;
    }

}