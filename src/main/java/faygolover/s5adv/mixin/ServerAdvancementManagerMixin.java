package faygolover.s5adv.mixin;

import faygolover.s5adv.AdvStore;
import faygolover.s5adv.ManagerAccess;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * После загрузки достижений датапаков (старт сервера, {@code /reload}) в готовый список добавляются наши из
 * {@code config/s5adv}. Белые списки других модов (flperf) их не трогают: те фильтруют датапаки раньше.
 */
@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin implements ManagerAccess {
    @Shadow private AdvancementList advancements;
    @Shadow @Final private LootDataManager lootData;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void s5adv$addOurs(CallbackInfo ci) {
        AdvStore.install(this.advancements, this.lootData);
    }

    @Override public AdvancementList s5adv$list() { return this.advancements; }
    @Override public LootDataManager s5adv$lootData() { return this.lootData; }
}
