package org.drpacket.allvsall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitStorageTest {
    @Test
    void serializeAndDeserializeInventory() {
        InventoryLayout layout = new InventoryLayout();
        String serialized = layout.serialize();
        InventoryLayout restored = InventoryLayout.deserialize(serialized);

        assertNotNull(restored);
        assertEquals(4, restored.getArmorContents().length);
        assertEquals(36, restored.getContents().length);
    }
}
