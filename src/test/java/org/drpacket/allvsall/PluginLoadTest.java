package org.drpacket.allvsall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginLoadTest {
    @Test
    void pluginLoads() {
        assertNotNull(new InventoryLayout());
    }
}
