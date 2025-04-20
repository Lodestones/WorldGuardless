package gg.lode.worldguardless;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import to.lodestone.bookshelfapi.api.util.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class WorldGuardless extends JavaPlugin {

    @Override
    public void onEnable() {
        new Metrics(this, 25421);

        // Plugin startup logic
        patchWorldGuard();
    }

    private File findWorldGuardJar() {
        File pluginsFolder = getDataFolder().getParentFile();

        File[] files = pluginsFolder.listFiles((dir, name) ->
                name.toLowerCase().startsWith("worldguard") && name.toLowerCase().endsWith(".jar")
        );

        if (files == null || files.length == 0) {
            getLogger().warning("Could not find WorldGuard jar in /plugins/");
            getServer().getPluginManager().disablePlugin(this);
            return null;
        }

        // Pick the first match
        return files[0];
    }

    public void patchWorldGuard() {
        File wgJar = findWorldGuardJar();

        if (wgJar == null) {
            getLogger().warning("WorldGuard jar not found. Cannot patch.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            File tempJar = new File(wgJar.getParentFile(), "WorldGuard-patched.jar");

            try (JarFile jarFile = new JarFile(wgJar);
                 JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempJar.toPath()))) {

                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    // Skip the old GeneralCommands.class entirely
                    if (entry.getName().equals("com/sk89q/worldguard/commands/GeneralCommands.class")) {
                        Bukkit.getLogger().info("Deleting: " + entry.getName());
                        continue;
                    }

                    jos.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        is.transferTo(jos);
                    }
                    jos.closeEntry();
                }

                // Now add our patched GeneralCommands.class
                JarEntry newEntry = new JarEntry("com/sk89q/worldguard/commands/GeneralCommands.class");
                jos.putNextEntry(newEntry);
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("com/sk89q/worldguard/commands/GeneralCommands.class")) {
                    if (is != null) {
                        is.transferTo(jos);
                        Bukkit.getLogger().info("Patched: " + newEntry.getName());
                    } else {
                        Bukkit.getLogger().warning("Replacement GeneralCommands.class not found in plugin jar!");
                    }
                }
                jos.closeEntry();
            }

            if (wgJar.delete() && tempJar.renameTo(wgJar)) {
                getLogger().info("=========================");
                getLogger().info("WorldGuardless has been patched.");
                getLogger().info("If commands like \"/heal\" or \"/god\" still exists, restart the server one more time!");
                getLogger().info("You can install this plugin at https://modrinth.com/plugin/worldguardless");
                getLogger().info("=========================");
            } else {
                getLogger().info("=========================");
                Bukkit.getLogger().severe("Failed to replace the WorldGuard jar!");
                getLogger().info("Make sure you have WorldGuard installed or make sure your WorldGuard jar starts with \"worldguard\".");
                getLogger().info("=========================");
                getServer().getPluginManager().disablePlugin(this);
            }

        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }


    @Override
    public void onDisable() {

    }
}
