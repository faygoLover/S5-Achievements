package faygolover.s5adv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Set;

/**
 * Шутка: {@code /sex anasvaldigor | archibald | Арчибальд}, без прав. Первый раз — скрытое достижение {@code s5:attempt},
 * дальше по строке в чат только этому игроку, потом команда молчит. Счётчик — в данных игрока (переживает смерть).
 * Кириллицу {@code word()} не пропускает, поэтому {@code greedyString} и сравнение.
 */
public final class SexCommand {
    private SexCommand() {}

    private static final Set<String> TARGETS = Set.of("anasvaldigor", "archibald", "арчибальд");
    private static final String COUNTER = "s5adv_sex";
    private static final String[] REPLIES = {"Нет", "Просто нет", "Хватит", "Я этого не одобряю", "Со дна постучали", "Арчибальд запомнит это"};
    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.translatable("command.unknown.argument"));

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sex")
                .then(Commands.argument("target", StringArgumentType.greedyString()).executes(SexCommand::run)));
    }

    private static int run(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String who = StringArgumentType.getString(c, "target").trim().toLowerCase(Locale.ROOT);
        if (!TARGETS.contains(who)) throw UNKNOWN.create();
        ServerPlayer p = c.getSource().getPlayerOrException();

        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        int n = persisted.getInt(COUNTER);
        if (n == 0) {
            S5Advancements.grant(p, "attempt");
        } else if (n <= REPLIES.length) {
            p.sendSystemMessage(Component.literal(REPLIES[n - 1]));
        } else if (n == REPLIES.length + 1) {
            p.sendSystemMessage(Component.literal("/ban " + p.getDisplayName().getString()));
        }
        if (n <= REPLIES.length + 1) {
            persisted.putInt(COUNTER, n + 1);
            p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        return 1;
    }
}
