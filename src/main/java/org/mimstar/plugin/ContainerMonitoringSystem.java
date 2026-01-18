package org.mimstar.plugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ContainerMonitoringSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, OpenedContainerComponent> containerComponentType;

    public ContainerMonitoringSystem(ComponentType<EntityStore, OpenedContainerComponent> type) {
        this.containerComponentType = type;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        // 1. Get data
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        OpenedContainerComponent monitor = archetypeChunk.getComponent(index, containerComponentType);
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());

        // 2. Get the World and Block State
        World world = player.getWorld();
        // 'true' in getState usually ensures we get the latest read/calculated state
        BlockState blockState = world.getState(monitor.getX(), monitor.getY(), monitor.getZ(), true);

        // 3. Check logic
        boolean stillOpen = false;

        // Ensure block is still a container (hasn't been broken)
        if (blockState instanceof ItemContainerState containerState) {
            // If windows is NOT empty, it is still being viewed by someone
            if (!containerState.getWindows().isEmpty()) {
                stillOpen = true;
                // Optional: You can do logic here "While Opened"
            }
        }

        // 4. If it's closed (or broken), remove the component to stop watching
        if (!stillOpen) {
            Loot4Everyone.LOGGER.atInfo().log("Container at " + monitor.getX() + " closed or broken. Stop watching.");
            PlayerLoot playerLoot = store.getComponent(playerRef,Loot4Everyone.get().getPlayerLootcomponentType());
            if (blockState instanceof ItemContainerState itemContainerState){
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++){
                    items.add(itemContainerState.getItemContainer().getItemStack((short) i));
                }
                playerLoot.setInventory(monitor.getX(), monitor.getY(), monitor.getZ(), items);
                commandBuffer.replaceComponent(playerRef,Loot4Everyone.get().getPlayerLootcomponentType(), playerLoot);
                itemContainerState.getItemContainer().clear();
            }
            commandBuffer.removeComponent(playerRef, containerComponentType);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Only run for entities that are Players AND have our monitor component
        return Query.and(Player.getComponentType(), containerComponentType);
    }
}
