package faygolover.s5adv;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Публичное API для других модов. id — {@code "spinning"} или {@code "s5:spinning"} (имя файла в config/s5adv).
 * Вызывать в серверном потоке. Если такого достижения нет (файл удалили) — ничего не происходит, вернётся false.
 */
public final class S5Advancements {
    private S5Advancements() {}

    /** Выдать достижение целиком (все условия). true — выдано сейчас; false — уже было или нет такого. */
    public static boolean grant(ServerPlayer player, String id) {
        Advancement adv = get(player, id);
        if (adv == null) return false;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return false;
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(adv, criterion);
        return true;
    }

    /** Отозвать достижение. true — было и отозвано. */
    public static boolean revoke(ServerPlayer player, String id) {
        Advancement adv = get(player, id);
        if (adv == null) return false;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (!progress.hasProgress()) return false;
        for (String criterion : progress.getCompletedCriteria()) player.getAdvancements().revoke(adv, criterion);
        return true;
    }

    public static boolean has(ServerPlayer player, String id) {
        Advancement adv = get(player, id);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** {@code "spinning"} → {@code s5:spinning}; {@code "s5:spinning"} как есть. */
    public static ResourceLocation id(String id) {
        return id.indexOf(':') >= 0 ? new ResourceLocation(id) : new ResourceLocation(AdvStore.NS, id);
    }

    private static Advancement get(ServerPlayer player, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.indexOf(':') >= 0 ? id : AdvStore.NS + ":" + id);
        return rl == null ? null : player.server.getAdvancements().getAdvancement(rl);
    }
}
