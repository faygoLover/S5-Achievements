package faygolover.s5adv;

import net.minecraft.advancements.AdvancementList;
import net.minecraft.world.level.storage.loot.LootDataManager;

/** Доступ к закрытым полям {@code ServerAdvancementManager} (реализует миксин). */
public interface ManagerAccess {
    AdvancementList s5adv$list();
    LootDataManager s5adv$lootData();
}
