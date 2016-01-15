package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 1/14/16.
 */
public abstract class DeltaBansCommand implements CommandExecutor
{
    private final String commandName;
    private String permission;
    private DeltaBansPlugin plugin;

    public DeltaBansCommand(String commandName, DeltaBansPlugin plugin)
    {
        this.commandName = commandName;
        this.plugin = plugin;
        this.plugin.getCommand(commandName).setExecutor(this);
    }

    public String getCommandName()
    {
        return commandName;
    }

    public String getPermission()
    {
        return permission;
    }

    public boolean register()
    {
        if(plugin != null)
        {
            plugin.getCommand(commandName).setExecutor(this);
            return true;
        }
        return false;
    }

    public boolean unregister()
    {
        if(plugin != null)
        {
            plugin.getCommand(commandName).setExecutor(null);
            return true;
        }
        return false;
    }

    public void shutdown()
    {
        onShutdown();
        unregister();
        this.permission = null;
        this.plugin = null;
    }

    public abstract void runCommand(CommandSender sender, Command command, String label, String[] args);

    public abstract void onShutdown();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!sender.hasPermission(permission))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
        }
        else
        {
            runCommand(sender, command, label, args);
        }
        return true;
    }
}
