package net.abhinav.dws;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public final class DynamicWeatherSystem extends JavaPlugin {

    private FileConfiguration config;
    private Random random = new Random();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();

        startWeatherCycle();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void startWeatherCycle() {
        int interval = config.getInt("change-interval", 10) * 20 * 60; // Convert minutes to ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                changeWeather();
            }
        }.runTaskTimer(this, 0L, interval);
    }

    private void changeWeather() {
        List<String> weatherOptions = config.getStringList("weather-options");
        String newWeather = weatherOptions.get(random.nextInt(weatherOptions.size()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            switch (newWeather) {
                case "rain":
                    player.getWorld().setWeatherDuration(config.getInt("weather-duration.rain", 15) * 20 * 60);
                    player.getWorld().setStorm(true);
                    player.getWorld().setThundering(false);
                    break;
                case "thunderstorm":
                    player.getWorld().setWeatherDuration(config.getInt("weather-duration.thunderstorm", 5) * 20 * 60);
                    player.getWorld().setStorm(true);
                    player.getWorld().setThundering(true);
                    startLightningStrikes(player.getWorld());
                    darkenSky(player.getWorld());
                    sendStormWarning(player);
                    break;
                case "clear":
                default:
                    player.getWorld().setWeatherDuration(config.getInt("weather-duration.clear", 10) * 20 * 60);
                    player.getWorld().setStorm(false);
                    player.getWorld().setThundering(false);
                    break;
            }
        }
    }

    private void startLightningStrikes(org.bukkit.World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (world.hasStorm() && world.isThundering()) {
                    Location randomLocation = getRandomLocation(world);
                    world.strikeLightning(randomLocation);
                    if (config.getBoolean("play-thunder-sound", true)) {
                        playThunderSound(randomLocation);
                    }
                    if (config.getBoolean("falling-debris", false)) {
                        spawnFallingDebris(randomLocation);
                    }
                    applyLightningDamage(world, randomLocation);
                    chainLightning(world, randomLocation);
                }
            }
        }.runTaskTimer(this, 0L, config.getBoolean("lightning-strike-every-tick", true) ? 1L : 20L); // 1 tick or configurable
    }

    private Location getRandomLocation(org.bukkit.World world) {
        int x = (int) (random.nextInt((int) world.getWorldBorder().getSize()) - world.getWorldBorder().getSize() / 2);
        int z = (int) (random.nextInt((int) world.getWorldBorder().getSize()) - world.getWorldBorder().getSize() / 2);
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private void playThunderSound(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    private void applyLightningDamage(org.bukkit.World world, Location location) {
        for (Entity entity : world.getNearbyEntities(location, 5, 5, 5)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.damage(config.getDouble("lightning-damage", 5.0)); // Example damage amount
            }
        }
    }

    private void darkenSky(org.bukkit.World world) {
        if (config.getBoolean("darken-sky", true)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                world.spawnParticle(Particle.CLOUD, world.getSpawnLocation(), 100, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.SMOKE, world.getSpawnLocation(), 100, 0.5, 0.5, 0.5, 0.1);
            }, 0L);
        }
    }

    private void sendStormWarning(Player player) {
        if (config.getBoolean("storm-warning", true)) {
            player.sendMessage("A thunderstorm is approaching! Seek shelter!");
        }
    }

    private void spawnFallingDebris(Location location) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (int i = 0; i < 20; i++) {
                double xOffset = random.nextDouble() * 2 - 1;
                double yOffset = random.nextDouble() * 2;
                double zOffset = random.nextDouble() * 2 - 1;
                Location particleLocation = location.clone().add(xOffset, yOffset, zOffset);
                particleLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLocation, 10, 0.5, 0.5, 0.5, 0.1);
                // Example collision effect
                particleLocation.getWorld().playSound(particleLocation, Sound.BLOCK_GRAVEL_PLACE, 1.0f, 1.0f);
            }
        }, 0L);
    }


    private void chainLightning(org.bukkit.World world, Location location) {
        if (config.getBoolean("chain-lightning", false)) {
            for (int i = 0; i < 3; i++) {
                Location chainLocation = getRandomLocation(world).add(0, 20, 0); // Slightly offset
                world.strikeLightning(chainLocation);
                if (config.getBoolean("play-thunder-sound", true)) {
                    playThunderSound(chainLocation);
                }
                applyLightningDamage(world, chainLocation);
                spawnFallingDebris(chainLocation);
            }
        }
    }
}
