package org.oreo.nodesregions.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.oreo.nodesregions.Nodes_regions;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Resident;
import phonon.nodes.objects.Territory;
import phonon.nodes.objects.Town;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class PlayerMoveIntoNewTown implements Listener {

    private final Nodes_regions plugin;
    Nodes nodes;

    public PlayerMoveIntoNewTown(Nodes_regions plugin){
        this.plugin = plugin;
        nodes = Nodes.INSTANCE;
    }

    @EventHandler
    public void OnPlayerEnterTown (PlayerMoveEvent e){

        if (!plugin.regionsDebuff){
            return;
        }

        Player player = e.getPlayer();

        if (isRegionWhitelisted(player)){
            return;
        }

        World world = Objects.requireNonNull(e.getTo()).getWorld();

        int fromX = e.getFrom().getBlockX();
        int fromZ = e.getFrom().getBlockZ();

        int toX = e.getTo().getBlockX();
        int toZ = e.getTo().getBlockZ();



        // check if player chunk changed
        assert world != null;
        Territory fromTerritory = nodes.getTerritoryFromBlock(fromX,fromZ);

        Territory toTerritory = nodes.getTerritoryFromBlock(toX,toZ);


        if ( (toTerritory == null)) {
            plugin.playerOutOfRegion.remove(player);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            return;
        }


        if (fromTerritory == null || !fromTerritory.equals(toTerritory)) {
            Resident resident = nodes.getResident(player);
            Town town = toTerritory.getTown();

            assert resident != null;
            if (!plugin.isSameGroup(resident, town) && !plugin.playerOutOfRegion.contains(player)) {
                plugin.playerOutOfRegion.add(player);
                plugin.playerWrongRegion(player);

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                plugin.sendTitle(player, ChatColor.DARK_RED + "You can't fight here", ChatColor.YELLOW + "Return to your region", 10, 45, 10);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 6 * 20, 1));
            } else {
                plugin.playerOutOfRegion.remove(player);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }


    private boolean isRegionWhitelisted(Player player){
        String name = player.getName();

        boolean isInList = false;

        for (HashMap<String, List<String>> map : plugin.whiteList) {
            // Iterate through each entry in the HashMap
            for (List<String> list : map.values()) {
                // Iterate through each string in the list
                for (String value : list) {
                    if (value.equals(name)) {
                        isInList = true;
                        break;
                    }
                }
            }
        }

        return isInList;
    }
}
