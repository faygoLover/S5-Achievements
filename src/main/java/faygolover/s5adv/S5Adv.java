package faygolover.s5adv;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * S5 Achievements — достижения сервера «Департамент S5» в одной вкладке. Файлы — {@code config/s5adv}, формат —
 * docs/format.md. Нужен только на сервере: клиент получает достижения ванильным пакетом, наши строки — готовым текстом.
 * Другим модам — {@link S5Advancements}.
 */
@Mod(S5Adv.MODID)
public class S5Adv {
    public static final String MODID = "s5adv";
    public static final Logger LOG = LogUtils.getLogger();

    public S5Adv() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(S5Adv::onCommands);
        bus.addListener(S5Adv::onEarn);
    }

    private static void onCommands(RegisterCommandsEvent e) {
        S5AdvCommand.register(e.getDispatcher());
        SexCommand.register(e.getDispatcher());
    }

    /** Флажок «в общий чат»: ванильное сообщение о достижении всем игрокам (gamerule announceAdvancements не нужен). */
    private static void onEarn(AdvancementEvent.AdvancementEarnEvent e) {
        Advancement adv = e.getAdvancement();
        AdvStore.Flags f = AdvStore.flags(adv.getId());
        if (f == null || !f.chat() || adv.getDisplay() == null || !(e.getEntity() instanceof ServerPlayer p)) return;
        p.server.getPlayerList().broadcastSystemMessage(Component.translatable(
                "chat.type.advancement." + adv.getDisplay().getFrame().getName(), p.getDisplayName(), adv.getChatComponent()), false);
    }
}
