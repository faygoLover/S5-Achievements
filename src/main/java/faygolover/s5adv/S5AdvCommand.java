package faygolover.s5adv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** {@code /s5adv reload | list [игрок] | give <игроки> <id> | revoke <игроки> <id>} — права 2. Ответы — готовым текстом (на клиенте мода нет). */
public final class S5AdvCommand {
    private S5AdvCommand() {}

    private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, b) ->
            SharedSuggestionProvider.suggest(AdvStore.all().keySet().stream().map(ResourceLocation::toString), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("s5adv").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload").executes(S5AdvCommand::reload))
                .then(Commands.literal("list").executes(c -> list(c, null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> list(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("give").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(IDS).executes(c -> change(c, true)))))
                .then(Commands.literal("revoke").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(IDS).executes(c -> change(c, false))))));
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        AdvStore.Result r = AdvStore.reload(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.literal("[s5adv] Перечитано: " + r.loaded() + " достижений"
                + (r.errors().isEmpty() ? ", ошибок нет" : ", ошибок: " + r.errors().size())), true);
        for (String err : r.errors()) c.getSource().sendFailure(Component.literal(" • " + err));
        return r.loaded();
    }

    private static int list(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        Map<ResourceLocation, AdvStore.Flags> all = new TreeMap<>(AdvStore.all());
        c.getSource().sendSuccess(() -> Component.literal("[s5adv] Достижений: " + all.size()
                + (player != null ? " — у " + player.getDisplayName().getString() : "")), false);
        for (Map.Entry<ResourceLocation, AdvStore.Flags> e : all.entrySet()) {
            Advancement adv = c.getSource().getServer().getAdvancements().getAdvancement(e.getKey());
            if (adv == null) continue;
            AdvStore.Flags f = e.getValue();
            MutableComponent line = Component.literal(" ");
            if (player != null) {
                boolean done = player.getAdvancements().getOrStartProgress(adv).isDone();
                line.append(Component.literal(done ? "✔ " : "✘ ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal(e.getKey().toString()).withStyle(ChatFormatting.YELLOW));
            if (adv.getDisplay() != null) line.append(" — ").append(adv.getDisplay().getTitle().copy());
            String flags = (f.toast() ? " всплывашка" : "") + (f.chat() ? " чат" : "") + (f.hidden() ? " скрытое" : "");
            if (!flags.isEmpty()) line.append(Component.literal(" [" + flags.trim() + "]").withStyle(ChatFormatting.GRAY));
            c.getSource().sendSuccess(() -> line, false);
        }
        return all.size();
    }

    private static int change(CommandContext<CommandSourceStack> c, boolean give) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "players");
        ResourceLocation raw = ResourceLocationArgument.getId(c, "id");
        // «spinning» без пространства имён Brigadier читает как minecraft:spinning — это наше
        String id = raw.getNamespace().equals("minecraft") ? raw.getPath() : raw.toString();
        ResourceLocation full = S5Advancements.id(id);
        if (!full.getNamespace().equals(AdvStore.NS) || AdvStore.flags(full) == null) {
            c.getSource().sendFailure(Component.literal("[s5adv] Нет такого достижения: " + full));
            return 0;
        }
        int n = 0;
        for (ServerPlayer p : players) {
            if (give ? S5Advancements.grant(p, id) : S5Advancements.revoke(p, id)) n++;
        }
        int done = n;
        List<String> names = players.stream().map(p -> p.getDisplayName().getString()).toList();
        c.getSource().sendSuccess(() -> Component.literal("[s5adv] " + full + (give ? " выдано: " : " отозвано: ") + done
                + " из " + players.size() + " (" + String.join(", ", names) + ")"), true);
        return done;
    }
}
