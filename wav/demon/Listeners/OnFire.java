package wav.demon.Listeners;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import wav.demon.StatCraft;
import wav.demon.StatTypes;

import java.util.ArrayList;

public class OnFire extends StatListener implements CommandExecutor {

    public OnFire(StatCraft plugin) {
        super(plugin);
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void oniFire(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            final EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {

                final String name = ((Player) event.getEntity()).getName();
                addStat(StatTypes.ON_FIRE.id, name, getStat(name, StatTypes.ON_FIRE.id) + 1);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> names = getPlayers(sender, args);
        if (names == null)
            return false;

        //int fireTime;
        for (String name : names) {
            int stat = getStat(name, StatTypes.ON_FIRE.id);
            String timeOnFire = transformTime(stat);
            String message;
            if (timeOnFire.equals(""))
                message = name + " has not been on fire yet.";
            else
                message = name + " - On fire: " + timeOnFire;

            respondToCommand(message, args, sender);
        }

        return true;
    }
}
