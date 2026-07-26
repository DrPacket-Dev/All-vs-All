package org.drpacket.allvsall;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class AllVsAllPlugin extends JavaPlugin implements Listener, TabCompleter {
    private final Set<UUID> hostedPlayers = new HashSet<>();
    private final Map<UUID, String> playerRanks = new HashMap<>();
    private final Map<UUID, InventoryLayout> savedKits = new HashMap<>();
    private final Map<UUID, String> activeKitType = new HashMap<>();
    private final Map<UUID, Inventory> kitEditorMenus = new HashMap<>();
    private final Map<UUID, Boolean> menuIsEditor = new HashMap<>();
    private final Map<UUID, Integer> borderSize = new HashMap<>();
    private String selectedKit = "uhc";
    private final Map<UUID, Boolean> borderEnabled = new HashMap<>();
    private final Map<UUID, Integer> borderShrinkTicks = new HashMap<>();
    private final Map<UUID, Integer> borderShrinkAmount = new HashMap<>();
    private final Map<UUID, String> currentKitName = new HashMap<>();
    private File kitDatabaseFile;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String configuredPath = getConfig().getString("database.path", "kits.db");
        kitDatabaseFile = new File(configuredPath);
        if (!kitDatabaseFile.isAbsolute()) {
            kitDatabaseFile = new File(getDataFolder(), configuredPath);
        }
        if (kitDatabaseFile.getParentFile() != null && !kitDatabaseFile.getParentFile().exists() && !kitDatabaseFile.getParentFile().mkdirs()) {
            getLogger().warning("Could not create kit database directory");
        }
        databaseManager = new DatabaseManager(kitDatabaseFile);
        databaseManager.initialize();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("settings").setTabCompleter(this);
        getCommand("kit").setTabCompleter(this);
        getCommand("border").setTabCompleter(this);
        getCommand("b").setTabCompleter(this);
        getLogger().info("All vs All plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("All vs All plugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this plugin.");
            return true;
        }

        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "host" -> handleHost(player);
            case "settings" -> handleSettings(player);
            case "kit" -> handleKit(player, args);
            case "border", "b" -> handleBorder(player, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleHost(Player player) {
        hostedPlayers.add(player.getUniqueId());
        playerRanks.put(player.getUniqueId(), "Hoster");
        sendMessage(player, ChatColor.GREEN + getConfig().getString("messages.host", "You are now the host of the event."));
        sendMessage(player, ChatColor.YELLOW + getConfig().getString("messages.host_help", "Use /settings to configure the match and /kit to build kits."));
    }

    private void handleSettings(Player player) {
        if (!hasHostPermission(player)) {
            sendMessage(player, ChatColor.RED + getConfig().getString("messages.no_host", "You need host rights to use this feature."));
            return;
        }

        openSettingsMenu(player);
    }

    private void handleKit(Player player, String[] args) {
        String kitType = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : selectedKit;

        if (hasHostPermission(player)) {
            if (args.length > 0 && "list".equalsIgnoreCase(kitType)) {
                openKitSelectionMenu(player);
                return;
            }
            selectedKit = kitType;
            openKitMenu(player, kitType, true);
            return;
        }

        if (args.length > 0) {
            sendMessage(player, ChatColor.RED + "Only the host can choose kits. Opening the current kit builder.");
        }
        openKitMenu(player, selectedKit, false);
    }

    private void handleBorder(Player player, String[] args) {
        if (!hasHostPermission(player)) {
            sendMessage(player, ChatColor.RED + getConfig().getString("messages.no_host", "You need host rights to use this feature."));
            return;
        }

        if (args.length == 0) {
            sendMessage(player, ChatColor.YELLOW + "Use /border <size|add|subtract> or /border <number>");
            return;
        }

        try {
            int value = Integer.parseInt(args[0]);
            borderSize.put(player.getUniqueId(), value);
            sendMessage(player, ChatColor.GREEN + "Border size set to " + value + " blocks.");
            return;
        } catch (NumberFormatException ignored) {
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                int current = borderSize.getOrDefault(player.getUniqueId(), 1000);
                borderSize.put(player.getUniqueId(), current + 50);
                sendMessage(player, ChatColor.GREEN + "Border size increased to " + borderSize.get(player.getUniqueId()) + " blocks.");
            }
            case "subtract" -> {
                int current = borderSize.getOrDefault(player.getUniqueId(), 1000);
                borderSize.put(player.getUniqueId(), Math.max(50, current - 50));
                sendMessage(player, ChatColor.GREEN + "Border size decreased to " + borderSize.get(player.getUniqueId()) + " blocks.");
            }
            default -> sendMessage(player, ChatColor.RED + "Unknown border command. Use /border <size|add|subtract>.");
        }
    }

    private void openSettingsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, ChatColor.GOLD + "Event Settings");
        fillWithGlass(inventory);
        addItem(inventory, 10, createItem(Material.GRASS_BLOCK, "§aBorder: On"));
        addItem(inventory, 12, createItem(Material.BEDROCK, "§eShrink Border"));
        addItem(inventory, 14, createItem(Material.OAK_SIGN, "§6Set Size"));
        addItem(inventory, 16, createItem(Material.CLOCK, "§bSet Shrink Time"));
        addItem(inventory, 26, createItem(Material.BARRIER, "§cClose"));
        kitEditorMenus.put(player.getUniqueId(), inventory);
        menuIsEditor.put(player.getUniqueId(), false);
        player.openInventory(inventory);
    }

    private void openKitSelectionMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, ChatColor.DARK_AQUA + "Choose a Kit");
        fillWithGlass(inventory);
        addItem(inventory, 10, createItem(Material.DIAMOND_SWORD, "§aUHC"));
        addItem(inventory, 12, createItem(Material.IRON_AXE, "§aMace"));
        addItem(inventory, 14, createItem(Material.STONE_SWORD, "§aOnlySword"));
        addItem(inventory, 16, createItem(Material.CHEST, "§eLoad selected kit"));
        addItem(inventory, 26, createItem(Material.BARRIER, "§cClose"));
        kitEditorMenus.put(player.getUniqueId(), inventory);
        menuIsEditor.put(player.getUniqueId(), false);
        player.openInventory(inventory);
    }

    private void openKitMenu(Player player, String kitType, boolean editorMode) {
        Inventory inventory = Bukkit.createInventory(player, 54, editorMode ? ChatColor.DARK_PURPLE + "Kit Editor - " + kitType.toUpperCase(Locale.ROOT) : ChatColor.GOLD + "Kit - " + kitType.toUpperCase(Locale.ROOT));
        fillWithGlass(inventory);
        addItem(inventory, 0, createItem(Material.BLACK_STAINED_GLASS_PANE, "§8Armor"));
        addItem(inventory, 1, createItem(Material.IRON_HELMET, "§fHelmet"));
        addItem(inventory, 2, createItem(Material.IRON_CHESTPLATE, "§fChestplate"));
        addItem(inventory, 3, createItem(Material.IRON_LEGGINGS, "§fLeggings"));
        addItem(inventory, 4, createItem(Material.IRON_BOOTS, "§fBoots"));
        addItem(inventory, 5, createItem(Material.BLACK_STAINED_GLASS_PANE, "§8Offhand"));
        addItem(inventory, 6, createItem(Material.SHIELD, "§fOffhand"));

        addItem(inventory, 9, createItem(Material.BLACK_STAINED_GLASS_PANE, "§8Utility"));
        addItem(inventory, 10, createItem(Material.WATER_BUCKET, "§fWater"));
        addItem(inventory, 11, createItem(Material.LAVA_BUCKET, "§fLava"));
        addItem(inventory, 12, createItem(Material.COBWEB, "§fCobweb"));
        addItem(inventory, 13, createItem(Material.TORCH, "§fTorch"));
        addItem(inventory, 14, createItem(Material.SAND, "§fSand"));
        addItem(inventory, 15, createItem(Material.BLACK_STAINED_GLASS_PANE, "§8Combat"));
        addItem(inventory, 16, createItem(Material.IRON_SWORD, "§fSword"));
        addItem(inventory, 17, createItem(Material.IRON_AXE, "§fAxe"));

        if (editorMode) {
            addItem(inventory, 27, createItem(Material.GREEN_STAINED_GLASS_PANE, "§aSave Kit"));
            addItem(inventory, 35, createItem(Material.CHEST, "§fSave current layout"));
        } else {
            addItem(inventory, 27, createItem(Material.GREEN_STAINED_GLASS_PANE, "§aLoad kit"));
            addItem(inventory, 35, createItem(Material.CHEST, "§fLoad saved layout"));
        }
        addItem(inventory, 53, createItem(Material.BARRIER, "§cClose"));
        kitEditorMenus.put(player.getUniqueId(), inventory);
        menuIsEditor.put(player.getUniqueId(), editorMode);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(ChatColor.AQUA + getConfig().getString("messages.welcome", "Welcome to the Packet Community Server."));
        if (getConfig().getBoolean("scoreboard.enabled", true)) {
            updateScoreboard(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        hostedPlayers.remove(id);
        playerRanks.remove(id);
        activeKitType.remove(id);
        kitEditorMenus.remove(id);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!kitEditorMenus.containsKey(player.getUniqueId())) {
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !inventory.equals(kitEditorMenus.get(player.getUniqueId()))) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        int slot = event.getRawSlot();
        boolean editorMode = Boolean.TRUE.equals(menuIsEditor.get(player.getUniqueId()));
        String title = event.getView().getTitle();
        if (slot == 35) {
            if (editorMode) {
                saveKitLayout(player);
                sendMessage(player, ChatColor.GREEN + "Kit saved to the configured database.");
            } else {
                loadSavedKit(player, selectedKit);
                sendMessage(player, ChatColor.GREEN + "Loaded the saved kit into your inventory.");
            }
            return;
        }
        if (title.contains("Choose a Kit")) {
            if (!hasHostPermission(player)) {
                sendMessage(player, ChatColor.RED + "Only the host can choose kits.");
                player.closeInventory();
                return;
            }
            switch (slot) {
                case 10 -> {
                    selectedKit = "uhc";
                    openKitMenu(player, selectedKit, true);
                    return;
                }
                case 12 -> {
                    selectedKit = "mace";
                    openKitMenu(player, selectedKit, true);
                    return;
                }
                case 14 -> {
                    selectedKit = "onlysword";
                    openKitMenu(player, selectedKit, true);
                    return;
                }
                case 16 -> {
                    loadSavedKit(player, selectedKit);
                    sendMessage(player, ChatColor.GREEN + "Loaded the saved kit into your inventory.");
                    return;
                }
                case 26 -> {
                    player.closeInventory();
                    return;
                }
            }
        }
        if (slot == 10 && title.contains("Event Settings")) {
            borderEnabled.put(player.getUniqueId(), !Boolean.TRUE.equals(borderEnabled.get(player.getUniqueId())));
            sendMessage(player, ChatColor.GREEN + "Border " + (borderEnabled.get(player.getUniqueId()) ? "enabled" : "disabled") + ".");
            openSettingsMenu(player);
            return;
        }
        if (slot == 12 && inventory.getSize() == 27 && inventory.getItem(12) != null && inventory.getItem(12).getType() == Material.BEDROCK) {
            borderShrinkTicks.put(player.getUniqueId(), borderShrinkTicks.getOrDefault(player.getUniqueId(), 600) + 200);
            sendMessage(player, ChatColor.GREEN + "Border shrink interval set to " + borderShrinkTicks.get(player.getUniqueId()) + " ticks.");
            openSettingsMenu(player);
            return;
        }
        if (slot == 14 && inventory.getSize() == 27 && inventory.getItem(14) != null && inventory.getItem(14).getType() == Material.OAK_SIGN) {
            borderSize.put(player.getUniqueId(), borderSize.getOrDefault(player.getUniqueId(), 1000) + 100);
            sendMessage(player, ChatColor.GREEN + "Border size set to " + borderSize.get(player.getUniqueId()) + " blocks.");
            openSettingsMenu(player);
            return;
        }
        if (slot == 16 && inventory.getSize() == 27 && inventory.getItem(16) != null && inventory.getItem(16).getType() == Material.CLOCK) {
            borderShrinkAmount.put(player.getUniqueId(), borderShrinkAmount.getOrDefault(player.getUniqueId(), 25) + 25);
            sendMessage(player, ChatColor.GREEN + "Border shrink amount set to " + borderShrinkAmount.get(player.getUniqueId()) + " blocks.");
            openSettingsMenu(player);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot == 11 || slot == 13 || slot == 15) {
            if (!editorMode && hasHostPermission(player)) {
                String selectedKit = clicked.getItemMeta() != null ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT) : "uhc";
                openKitMenu(player, selectedKit, false);
                return;
            }
            if (!editorMode) {
                return;
            }
        }

        if (isSelectableButton(clicked)) {
            giveItemToPlayer(player, clicked);
        }
    }

    private void saveKitLayout(Player player) {
        Inventory inventory = kitEditorMenus.get(player.getUniqueId());
        if (inventory == null) {
            return;
        }

        InventoryLayout layout = new InventoryLayout();
        ItemStack[] content = new ItemStack[36];
        for (int index = 0; index < 36; index++) {
            content[index] = inventory.getItem(index) == null ? new ItemStack(Material.AIR) : inventory.getItem(index).clone();
        }
        layout.setContents(content);
        layout.setArmorContents(new ItemStack[]{
                inventory.getItem(1) == null ? new ItemStack(Material.AIR) : inventory.getItem(1).clone(),
                inventory.getItem(2) == null ? new ItemStack(Material.AIR) : inventory.getItem(2).clone(),
                inventory.getItem(3) == null ? new ItemStack(Material.AIR) : inventory.getItem(3).clone(),
                inventory.getItem(4) == null ? new ItemStack(Material.AIR) : inventory.getItem(4).clone()
        });
        layout.setOffhand(inventory.getItem(6) == null ? new ItemStack(Material.AIR) : inventory.getItem(6).clone());
        savedKits.put(player.getUniqueId(), layout);
        currentKitName.put(player.getUniqueId(), activeKitType.getOrDefault(player.getUniqueId(), "uhc"));
        savedKits.put(player.getUniqueId(), layout);
        String serializedLayout = layout.serialize();
        databaseManager.saveKit(currentKitName.getOrDefault(player.getUniqueId(), "uhc"), serializedLayout);
        sendMessage(player, ChatColor.GREEN + "Kit saved for " + currentKitName.get(player.getUniqueId()) + " in the configured database.");
    }

    private void giveItemToPlayer(Player player, ItemStack clicked) {
        ItemStack toGive = clicked.clone();
        toGive.setAmount(Math.max(1, toGive.getAmount()));
        player.getInventory().addItem(toGive);
        player.updateInventory();
    }

    private void loadSavedKit(Player player, String kitName) {
        String serialized = databaseManager.loadKit(kitName);
        if (serialized == null) {
            sendMessage(player, ChatColor.RED + "No saved kit found for " + kitName + ".");
            return;
        }

        InventoryLayout layout = InventoryLayout.deserialize(serialized);
        player.getInventory().setArmorContents(layout.getArmorContents());
        player.getInventory().setContents(new ItemStack[36]);
        player.getInventory().setItemInOffHand(layout.getOffhand());
        for (ItemStack item : layout.getContents()) {
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
        player.updateInventory();
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("allvsall", "dummy", ChatColor.GOLD + getConfig().getString("scoreboard.title", "Packet Community Server"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore(ChatColor.YELLOW + "Hoster:").setScore(3);
        objective.getScore(ChatColor.WHITE + (hostedPlayers.isEmpty() ? "-" : Bukkit.getOfflinePlayer(hostedPlayers.iterator().next()).getName())).setScore(2);
        objective.getScore(ChatColor.YELLOW + "Players:").setScore(1);
        objective.getScore(ChatColor.WHITE + String.valueOf(Bukkit.getOnlinePlayers().size())).setScore(0);
        player.setScoreboard(scoreboard);
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return java.util.Collections.emptyList();
        }
        List<String> suggestions = new ArrayList<>();
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "kit" -> {
                suggestions.addAll(List.of("uhc", "mace", "onlysword", "list"));
            }
            case "border", "b" -> {
                suggestions.addAll(List.of("1000", "add", "subtract", "200", "500"));
            }
            case "settings" -> {
                suggestions.addAll(List.of("border", "shrink", "size"));
            }
        }
        return suggestions;
    }

    private boolean hasHostPermission(Player player) {
        return hostedPlayers.contains(player.getUniqueId()) || player.hasPermission("allvsall.host");
    }

    private void sendMessage(Player player, String message) {
        String prefix = getConfig().getString("messages.prefix", "[Packet Serv]");
        player.sendMessage(prefix + " " + message);
    }

    private void addItem(Inventory inventory, int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    private void fillWithGlass(Inventory inventory) {
        ItemStack pane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int index = 0; index < inventory.getSize(); index++) {
            if (inventory.getItem(index) == null) {
                inventory.setItem(index, pane);
            }
        }
    }

    private boolean isSelectableButton(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Material type = item.getType();
        return type != Material.BARRIER && type != Material.CHEST && type != Material.GREEN_STAINED_GLASS_PANE && type != Material.BLUE_STAINED_GLASS_PANE && type != Material.RED_STAINED_GLASS_PANE;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
