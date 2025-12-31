package mitra88.whatdoiown;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class InventoryScanner {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private static final int BACKUP_INTERVAL_TICKS = 1200; // 60 seconds
    private static int tickCounter = 0;

    private static boolean dumping = false;
    private static int lastInventoryHash = 0;

    /* ================= MANUAL DUMP ================= */
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!InventoryDumpMod.dumpKey.isPressed() || dumping) return;
        dumping = true;
        try {
            dumpInventory(false);
        } finally {
            dumping = false;
        }
    }

    /* ================= AUTO BACKUP ================= */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        tickCounter++;
        if (tickCounter < BACKUP_INTERVAL_TICKS) return;
        tickCounter = 0;

        dumpInventory(true);
    }

    /* ================= CORE LOGIC ================= */
    private void dumpInventory(boolean backup) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || player.inventory == null) return;

        List<ItemEntry> items = new ArrayList<>();
        ItemStack[] inv = player.inventory.mainInventory;

        for (int slot = 0; slot < inv.length; slot++) {
            ItemStack stack = inv[slot];
            if (stack == null || stack.stackSize <= 0) continue;

            String rawName = resolveName(stack);
            String cleanName = cleanName(rawName);

            items.add(new ItemEntry(slot, rawName, cleanName, stack.stackSize));
        }

        int hash = items.hashCode();
        if (backup && hash == lastInventoryHash) return; // no changes
        lastInventoryHash = hash;

        writeJson(items, backup);

        if (!backup) {
            player.addChatMessage(new ChatComponentText(
                    "§aInventory dumped manually (" + items.size() + " items)"
            ));
        }
    }

    private String resolveName(ItemStack stack) {
        try {
            if (stack.hasDisplayName()) {
                return stack.getDisplayName();
            }
            return stack.getItem().getItemStackDisplayName(stack);
        } catch (Exception e) {
            return "Unknown Item";
        }
    }

    private String cleanName(String raw) {
        if (raw == null) return "Unknown Item";
        // Remove Minecraft color codes (§x)
        String cleaned = raw.replaceAll("§.", "");
        // Remove stars and extra symbols
        cleaned = cleaned.replaceAll("[✪✦⚚]", "");
        return cleaned.trim();
    }

    /* ================= FILE HANDLING ================= */
    private void writeJson(List<ItemEntry> items, boolean backup) {
        File baseDir = new File(
                Minecraft.getMinecraft().mcDataDir,
                backup ? "config/inventory_backups" : "config"
        );
        if (!baseDir.exists()) baseDir.mkdirs();

        String fileName = backup
                ? "inv_" + timestamp() + ".json"
                : "inventory_dump.json";

        File out = new File(baseDir, fileName);
        File tmp = new File(baseDir, fileName + ".tmp");

        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(tmp.toPath()),
                StandardCharsets.UTF_8
        )) {
            GSON.toJson(items, writer);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (out.exists()) out.delete();
        tmp.renameTo(out);
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                .format(new Date());
    }

    /* ================= DATA MODEL ================= */
    private static class ItemEntry {
        final int slot;
        final String name_raw;
        final String name_clean;
        final int count;

        ItemEntry(int slot, String raw, String clean, int count) {
            this.slot = slot;
            this.name_raw = raw;
            this.name_clean = clean;
            this.count = count;
        }

        @Override
        public int hashCode() {
            return Objects.hash(slot, name_raw, name_clean, count);
        }
    }
}
