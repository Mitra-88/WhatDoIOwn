package mitra88.whatdoiown;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@Mod(
        modid = "invdump",
        name = "Inventory Dump",
        version = "2.0",
        clientSideOnly = true
)
public class InventoryDumpMod {

    public static KeyBinding dumpKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        dumpKey = new KeyBinding(
                "Dump Inventory to JSON",
                Keyboard.KEY_P,
                "Inventory Dump"
        );
        ClientRegistry.registerKeyBinding(dumpKey);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new InventoryScanner());
    }
}
