package faygolover.s5adv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Наши достижения: файлы {@code config/s5adv/**.json} → id {@code s5:<путь без .json>}. Формат — docs/format.md.
 * Всё в одной вкладке: у кого нет родителя (или родитель не из s5) — родитель {@code s5:root}.
 */
public final class AdvStore {
    private AdvStore() {}

    public static final String NS = "s5";
    public static final ResourceLocation ROOT = new ResourceLocation(NS, "root");
    private static final String ROOT_BACKGROUND = "minecraft:textures/block/gray_concrete.png";
    private static final String[] DEFAULTS = {"root", "attempt", "spinning"};

    /** Флажки из блока {@code "s5"}: всплывашка (только себе), сообщение в общий чат, скрытое. */
    public record Flags(boolean toast, boolean chat, boolean hidden) {}

    /** Итог загрузки: сколько достижений в игре и что не так с файлами. */
    public record Result(int loaded, List<String> errors) {}

    private static volatile Map<ResourceLocation, Flags> flags = Map.of();
    /** Последняя удачная версия каждого файла: если файл сломали, достижение (и прогресс игроков) не пропадает. */
    private static final Map<ResourceLocation, JsonObject> lastGood = new HashMap<>();

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("s5adv");
    }

    public static Flags flags(ResourceLocation id) {
        return flags.get(id);
    }

    public static Map<ResourceLocation, Flags> all() {
        return flags;
    }

    /** Перечитать только наши достижения и дослать всем, кто онлайн. Датапаки не перезагружаются. */
    public static Result reload(MinecraftServer server) {
        ServerAdvancementManager manager = server.getAdvancements();
        ManagerAccess access = (ManagerAccess) manager;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        // прогресс — на диск: reload игрока читает его из файла заново
        for (ServerPlayer p : players) p.getAdvancements().save();
        Result r = install(access.s5adv$list(), access.s5adv$lootData());
        // ваниль так же досылает после /reload: игроку уходит полный список заново
        for (ServerPlayer p : players) p.getAdvancements().reload(manager);
        return r;
    }

    /** Прочитать файлы и заменить наши достижения в списке сервера. */
    public static synchronized Result install(AdvancementList list, LootDataManager lootData) {
        ensureDefaults();
        List<String> errors = new ArrayList<>();
        Map<ResourceLocation, JsonObject> jsons = readFiles(errors);
        Map<ResourceLocation, Advancement.Builder> builders = new LinkedHashMap<>();
        Map<ResourceLocation, Flags> newFlags = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : jsons.entrySet()) {
            ResourceLocation id = e.getKey();
            JsonObject source = e.getValue();
            try {
                build(id, source.deepCopy(), lootData, builders, newFlags);
                lastGood.put(id, source);
            } catch (Exception ex) {
                JsonObject old = lastGood.get(id);
                if (old != null) {
                    errors.add(id + ": " + ex.getMessage() + " — оставлена прежняя версия");
                    try { build(id, old.deepCopy(), lootData, builders, newFlags); } catch (Exception ignored) {}
                } else {
                    errors.add(id + ": " + ex.getMessage());
                }
            }
        }
        lastGood.keySet().retainAll(jsons.keySet());
        if (!builders.containsKey(ROOT)) errors.add(ROOT + ": нет корневого достижения (root.json) — вкладка не появится");

        removeOurs(list);
        list.add(builders);
        Map<ResourceLocation, Flags> live = new HashMap<>();
        for (ResourceLocation id : builders.keySet()) {
            if (list.get(id) != null) live.put(id, newFlags.get(id));
            else errors.add(id + ": не загрузилось — родителя нет среди достижений s5");
        }
        Advancement root = list.get(ROOT);
        if (root != null && root.getDisplay() != null) TreeNodePosition.run(root);
        flags = Map.copyOf(live);
        S5Adv.LOG.info("[s5adv] Достижения: {} из {} файлов", live.size(), jsons.size());
        for (String err : errors) S5Adv.LOG.warn("[s5adv] {}", err);
        return new Result(live.size(), List.copyOf(errors));
    }

    private static void build(ResourceLocation id, JsonObject json, LootDataManager lootData,
                              Map<ResourceLocation, Advancement.Builder> builders, Map<ResourceLocation, Flags> newFlags) {
        JsonObject ours = json.has("s5") && json.get("s5").isJsonObject() ? json.getAsJsonObject("s5") : new JsonObject();
        json.remove("s5");
        Flags f = new Flags(GsonHelper.getAsBoolean(ours, "toast", true), GsonHelper.getAsBoolean(ours, "chat", false),
                GsonHelper.getAsBoolean(ours, "hidden", false));

        if (id.equals(ROOT)) {
            json.remove("parent");
        } else {
            ResourceLocation parent = json.has("parent") ? ResourceLocation.tryParse(GsonHelper.getAsString(json, "parent")) : null;
            if (parent == null || !parent.getNamespace().equals(NS)) json.addProperty("parent", ROOT.toString());
        }

        if (!json.has("display") || !json.get("display").isJsonObject()) throw new IllegalArgumentException("нет блока display");
        JsonObject display = json.getAsJsonObject("display");
        display.addProperty("show_toast", f.toast());
        display.addProperty("announce_to_chat", false); // в чат пишем сами (S5Adv.onEarn): без gamerule announceAdvancements
        display.addProperty("hidden", f.hidden());
        if (id.equals(ROOT)) {
            if (!display.has("background")) display.addProperty("background", ROOT_BACKGROUND);
        } else {
            display.remove("background");
        }

        boolean noCriteria = !json.has("criteria") || !json.get("criteria").isJsonObject() || json.getAsJsonObject("criteria").size() == 0;
        if (noCriteria) {
            // только выдача командой или из кода (S5Advancements.grant)
            JsonObject trigger = new JsonObject();
            trigger.addProperty("trigger", "minecraft:impossible");
            JsonObject criteria = new JsonObject();
            criteria.add("s5", trigger);
            json.add("criteria", criteria);
            json.remove("requirements");
        }

        Advancement.Builder b = Advancement.Builder.fromJson(json, new DeserializationContext(id, lootData), ICondition.IContext.EMPTY);
        if (b == null) return; // условия Forge не выполнены
        builders.put(id, b);
        newFlags.put(id, f);
    }

    private static Map<ResourceLocation, JsonObject> readFiles(List<String> errors) {
        Map<ResourceLocation, JsonObject> out = new LinkedHashMap<>();
        Path dir = dir();
        if (!Files.isDirectory(dir)) return out;
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            errors.add("папка " + dir + ": " + e.getMessage());
            return out;
        }
        for (Path p : files) {
            String rel = dir.relativize(p).toString().replace('\\', '/');
            String path = rel.substring(0, rel.length() - ".json".length());
            if (!ResourceLocation.isValidPath(path)) {
                errors.add(rel + ": имя файла — только a-z 0-9 _ - . /");
                continue;
            }
            ResourceLocation id = new ResourceLocation(NS, path);
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                JsonElement el = JsonParser.parseReader(r);
                if (!el.isJsonObject()) throw new IllegalArgumentException("не объект JSON");
                out.put(id, el.getAsJsonObject());
            } catch (Exception e) {
                JsonObject old = lastGood.get(id);
                if (old != null) {
                    errors.add(rel + ": " + e.getMessage() + " — оставлена прежняя версия");
                    out.put(id, old);
                } else {
                    errors.add(rel + ": " + e.getMessage());
                }
            }
        }
        return out;
    }

    /** Убрать наши достижения (с потомками), если они уже в списке. */
    private static void removeOurs(AdvancementList list) {
        List<ResourceLocation> ids = list.getAllAdvancements().stream().map(Advancement::getId)
                .filter(id -> id.getNamespace().equals(NS)).toList();
        for (ResourceLocation id : ids) {
            if (list.get(id) != null) list.remove(java.util.Set.of(id));
        }
    }

    /**
     * Встроенные достижения кладутся по одному разу (список — в {@code .defaults}): удалённое не возвращается.
     * Корень возвращается всегда — без него нет вкладки.
     */
    private static void ensureDefaults() {
        Path dir = dir();
        Path marker = dir.resolve(".defaults");
        try {
            Files.createDirectories(dir);
            List<String> written = Files.exists(marker) ? new ArrayList<>(Files.readAllLines(marker, StandardCharsets.UTF_8)) : new ArrayList<>();
            boolean changed = false;
            for (String name : DEFAULTS) {
                Path file = dir.resolve(name + ".json");
                if (!Files.exists(file) && (name.equals("root") || !written.contains(name))) {
                    try (InputStream in = AdvStore.class.getResourceAsStream("/s5adv_defaults/" + name + ".json")) {
                        if (in != null) Files.copy(in, file);
                    }
                }
                if (!written.contains(name)) {
                    written.add(name);
                    changed = true;
                }
            }
            if (changed) Files.write(marker, written, StandardCharsets.UTF_8);
        } catch (IOException e) {
            S5Adv.LOG.warn("[s5adv] Не удалось создать {}: {}", dir, e.toString());
        }
    }
}
