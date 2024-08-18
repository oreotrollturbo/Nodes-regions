package org.oreo.nodesregions;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.oreo.nodesregions.commands.RegionsCommand;
import org.oreo.nodesregions.listeners.PlayerMoveIntoNewTown;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Resident;
import phonon.nodes.objects.Town;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Nodes_regions extends JavaPlugin {

    Nodes nodes;

    private final Gson gson = new Gson();

    public List<String> europe = new ArrayList<>();
    public List<String> pacific = new ArrayList<>();

    public List<HashMap<String,List<String>>> whiteList = new ArrayList<>();

    public List<Player> playerOutOfRegion = new ArrayList<>();

    public boolean regionsDebuff = false;


    private final File saveWhiteList;
    private final File saveEurope;
    private final File savePacific;

    public Nodes_regions() {
        this.saveWhiteList = new File(getDataFolder(), "regionsWhitelist.json");
        this.saveEurope = new File(getDataFolder(), "regionsEurope.json");
        this.savePacific = new File(getDataFolder(), "regionsPacific.json");
    }

    @Override
    public void onEnable() {

        saveDefaultConfig();

        nodes = Nodes.INSTANCE;

        getServer().getPluginManager().registerEvents(new PlayerMoveIntoNewTown(this), this);
        getCommand("regions").setExecutor(new RegionsCommand(this));

        loadSaveFiles();
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Saving all files");
        saveAllFiles();
    }

    public boolean isSameGroup(Resident resident , Town townEntered){
        return europe.contains(resident.getTown()) && europe.contains(townEntered) || pacific.contains(resident.getTown()) && pacific.contains(townEntered);
    }

    public void playerWrongRegion (Player player){
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 6 * 20,3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 6 * 20,87));
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }


    public void addPotionEffect() {
        new BukkitRunnable() {
            @Override
            public void run() {
                getServer().getScheduler().runTask(Nodes_regions.this, () -> {
                    if (!regionsDebuff){
                        cancel();
                    }else {
                        for (Player player : playerOutOfRegion){
                            playerWrongRegion(player);
                        }
                    }

                });
            }
        }.runTaskTimerAsynchronously(Nodes_regions.this, 0L, 20L * 5); // Applies every 5 secs
    }




    private void loadSaveFiles() {
        // Load the whitelist
        if (saveWhiteList.exists()) {
            try (FileReader reader = new FileReader(saveWhiteList)) {
                Type whiteListType = new TypeToken<List<HashMap<String, List<String>>>>() {}.getType();
                whiteList = gson.fromJson(reader, whiteListType);
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
        }

        // Load the Europe region
        if (saveEurope.exists()) {
            try (FileReader reader = new FileReader(saveEurope)) {
                Type townListType = new TypeToken<List<String>>() {}.getType();
                europe = gson.fromJson(reader, townListType);
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
        }

        // Load the Pacific region
        if (savePacific.exists()) {
            try (FileReader reader = new FileReader(savePacific)) {
                Type townListType = new TypeToken<List<String>>() {}.getType();
                pacific = gson.fromJson(reader, townListType);
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
        }

        // If files do not exist, initialize them
        initializeSaveFiles();
    }

    private void initializeSaveFiles() {
        if (!saveWhiteList.exists()) {
            try {
                if (saveWhiteList.createNewFile()) {
                    this.getLogger().info("Created new whitelist file at: " + saveWhiteList.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(saveWhiteList)) {
                        writer.write("[]"); // Write an empty JSON array to the file
                    }
                }
            } catch (IOException e) {
                this.getLogger().info("Unable to create whitelist save file.");
                e.printStackTrace();
            }
        } else {
            this.getLogger().info("Whitelist file found.");
        }

        if (!saveEurope.exists()) {
            try {
                if (saveEurope.createNewFile()) {
                    this.getLogger().info("Created new Europe file at: " + saveEurope.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(saveEurope)) {
                        writer.write("[]"); // Write an empty JSON array to the file
                    }
                }
            } catch (IOException e) {
                this.getLogger().info("Unable to create Europe save file.");
                e.printStackTrace();
            }
        } else {
            this.getLogger().info("Europe file found.");
        }

        if (!savePacific.exists()) {
            try {
                if (savePacific.createNewFile()) {
                    this.getLogger().info("Created new Pacific file at: " + savePacific.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(savePacific)) {
                        writer.write("[]"); // Write an empty JSON array to the file
                    }
                }
            } catch (IOException e) {
                this.getLogger().info("Unable to create Pacific save file.");
                e.printStackTrace();
            }
        } else {
            this.getLogger().info("Pacific file found.");
        }
    }


    public void saveAllFiles() {
        try {
            // Save the whitelist
            try (FileWriter writer = new FileWriter(saveWhiteList)) {
                gson.toJson(whiteList, writer);
            }

            // Save the Europe region
            try (FileWriter writer = new FileWriter(saveEurope)) {
                gson.toJson(europe, writer);
            }

            // Save the Pacific region
            try (FileWriter writer = new FileWriter(savePacific)) {
                gson.toJson(pacific, writer);
            }

            getLogger().info("Successfully saved all region data.");

        } catch (IOException e) {
            getLogger().severe("Error saving region data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveWhiteList() {
        try (FileWriter writer = new FileWriter(saveWhiteList)) {
            gson.toJson(whiteList, writer);
            getLogger().info("Successfully saved the whitelist data.");
        } catch (IOException e) {
            getLogger().severe("Error saving whitelist data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveEurope() {
        try (FileWriter writer = new FileWriter(saveEurope)) {
            gson.toJson(europe, writer);
            getLogger().info("Successfully saved the Europe region data.");
        } catch (IOException e) {
            getLogger().severe("Error saving Europe region data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void savePacific() {
        try (FileWriter writer = new FileWriter(savePacific)) {
            gson.toJson(pacific, writer);
            getLogger().info("Successfully saved the Pacific region data.");
        } catch (IOException e) {
            getLogger().severe("Error saving Pacific region data: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
