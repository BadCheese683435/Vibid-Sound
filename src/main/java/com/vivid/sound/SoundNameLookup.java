package com.vivid.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the vanilla language file for the game's currently selected language
 * and builds a lookup table of {@code subtitles.*} translation keys to their
 * localized text, so the mod's sound captions follow the game language.
 * <p>
 * For sounds that have no vanilla subtitle key at all (e.g. block step
 * sounds), a fallback name is built: in Chinese a token dictionary is used,
 * while other languages fall back to a readable rendering of the sound path.
 */
public class SoundNameLookup {

    private static Map<String, String> currentSubtitles = null;
    /** Language code the cached subtitle map was loaded for, e.g. "en_us". */
    private static String loadedLanguage = null;

    /**
     * Token dictionary used to build a Chinese display name from a sound ID
     * path when no vanilla subtitle key exists. Keys are the lower-cased
     * path segments; values are the corresponding Chinese words.
     */
    private static final Map<String, String> ZH_TOKEN_DICT = buildZhTokenDict();

    private static Map<String, String> buildZhTokenDict() {
        Map<String, String> m = new HashMap<>();
        // ---- categories / actions ----
        m.put("block", "方块");
        m.put("entity", "实体");
        m.put("player", "玩家");
        m.put("music", "音乐");
        m.put("weather", "天气");
        m.put("ui", "界面");
        m.put("ambient", "环境");
        m.put("step", "脚步");
        m.put("break", "破坏");
        m.put("place", "放置");
        m.put("hit", "击打");
        m.put("fall", "落地");
        m.put("land", "着陆");
        m.put("jump", "跳跃");
        m.put("swim", "游泳");
        m.put("splash", "溅水");
        m.put("eat", "进食");
        m.put("drink", "饮用");
        m.put("attack", "攻击");
        m.put("hurt", "受伤");
        m.put("death", "死亡");
        m.put("shoot", "射击");
        m.put("explode", "爆炸");
        m.put("explosion", "爆炸");
        m.put("close", "关闭");
        m.put("open", "打开");
        m.put("click", "点击");
        m.put("craft", "合成");
        m.put("equip", "装备");
        m.put("unequip", "卸下");
        m.put("use", "使用");
        m.put("fill", "填充");
        m.put("empty", "倒空");
        m.put("pour", "倾倒");
        m.put("bottle", "瓶");
        m.put("bucket", "桶");
        m.put("fish", "钓鱼");
        m.put("enchant", "附魔");
        m.put("repair", "修理");
        m.put("tame", "驯服");
        m.put("breed", "繁殖");
        m.put("die", "死亡");
        m.put("idle", "闲置");
        m.put("wake", "醒来");
        m.put("sleep", "睡觉");
        m.put("sneak", "潜行");
        m.put("sprint", "疾跑");
        m.put("breathe", "呼吸");
        m.put("fire", "火焰");
        m.put("extinguish", "熄灭");
        m.put("fuse", "引信");
        m.put("prime", "点燃");
        // ---- blocks / materials ----
        m.put("grass", "草地");
        m.put("stone", "石头");
        m.put("dirt", "泥土");
        m.put("sand", "沙子");
        m.put("gravel", "沙砾");
        m.put("wood", "木头");
        m.put("log", "原木");
        m.put("leaves", "树叶");
        m.put("wool", "羊毛");
        m.put("metal", "金属");
        m.put("ancient", "远古");
        m.put("debris", "残骸");
        m.put("netherrack", "下界岩");
        m.put("nether", "下界");
        m.put("end", "末地");
        m.put("obsidian", "黑曜石");
        m.put("glass", "玻璃");
        m.put("ice", "冰");
        m.put("snow", "雪");
        m.put("powder", "粉末");
        m.put("slime", "史莱姆");
        m.put("honey", "蜂蜜");
        m.put("wet", "湿润");
        m.put("dry", "干燥");
        m.put("crop", "作物");
        m.put("fungus", "菌菇");
        m.put("stem", "茎");
        m.put("wart", "疣");
        m.put("roots", "根");
        m.put("vine", "藤蔓");
        m.put("coral", "珊瑚");
        m.put("kelp", "海带");
        m.put("seagrass", "海草");
        m.put("bamboo", "竹子");
        m.put("cherry", "樱花");
        m.put("moss", "苔藓");
        m.put("mud", "泥巴");
        m.put("sculk", "幽匿");
        m.put("deepslate", "深板岩");
        m.put("tuff", "凝灰岩");
        m.put("calcite", "方解石");
        m.put("dripstone", "钟乳石");
        m.put("amethyst", "紫水晶");
        m.put("copper", "铜");
        m.put("iron", "铁");
        m.put("gold", "金");
        m.put("diamond", "钻石");
        m.put("emerald", "绿宝石");
        m.put("lapis", "青金石");
        m.put("redstone", "红石");
        m.put("coal", "煤炭");
        m.put("quartz", "石英");
        m.put("netherite", "下界合金");
        m.put("pumpkin", "南瓜");
        m.put("melon", "西瓜");
        m.put("cake", "蛋糕");
        m.put("candle", "蜡烛");
        m.put("lantern", "灯笼");
        m.put("chain", "锁链");
        m.put("door", "门");
        m.put("trapdoor", "活板门");
        m.put("fence", "栅栏");
        m.put("gate", "栅栏门");
        m.put("chest", "箱子");
        m.put("barrel", "木桶");
        m.put("shulker", "潜影盒");
        m.put("ender", "末影");
        m.put("enderchest", "末影箱");
        m.put("anvil", "铁砧");
        m.put("grindstone", "砂轮");
        m.put("loom", "织布机");
        m.put("cartography", "制图台");
        m.put("fletching", "制箭台");
        m.put("smithing", "锻造台");
        m.put("stonecutter", "切石机");
        m.put("brewing", "酿造台");
        m.put("cauldron", "炼药锅");
        m.put("beacon", "信标");
        m.put("conduit", "潮涌核心");
        m.put("enchanting", "附魔台");
        m.put("table", "台");
        m.put("furnace", "熔炉");
        m.put("blast", "高炉");
        m.put("smoker", "烟熏炉");
        m.put("campfire", "营火");
        m.put("soul", "灵魂");
        m.put("basalt", "玄武岩");
        m.put("blackstone", "黑石");
        m.put("glowstone", "荧石");
        m.put("sea", "海");
        m.put("pickle", "泡菜");
        m.put("lanterns", "灯笼");
        m.put("scaffolding", "脚手架");
        m.put("ladder", "梯子");
        m.put("rope", "绳索");
        m.put("chainmail", "锁子甲");
        m.put("bow", "弓");
        m.put("crossbow", "弩");
        m.put("arrow", "箭");
        m.put("trident", "三叉戟");
        m.put("shield", "盾");
        m.put("sword", "剑");
        m.put("axe", "斧");
        m.put("pickaxe", "镐");
        m.put("shovel", "锹");
        m.put("hoe", "锄");
        m.put("shears", "剪刀");
        m.put("fishing", "钓鱼竿");
        m.put("rod", "竿");
        m.put("flint", "燧石");
        m.put("and", "与");
        m.put("steel", "钢");
        // ---- entities ----
        m.put("cow", "牛");
        m.put("pig", "猪");
        m.put("sheep", "羊");
        m.put("chicken", "鸡");
        m.put("horse", "马");
        m.put("donkey", "驴");
        m.put("mule", "骡子");
        m.put("camel", "骆驼");
        m.put("wolf", "狼");
        m.put("cat", "猫");
        m.put("ocelot", "豹猫");
        m.put("fox", "狐狸");
        m.put("panda", "熊猫");
        m.put("polar", "北极熊");
        m.put("bear", "熊");
        m.put("mooshroom", "哞菇");
        m.put("rabbit", "兔子");
        m.put("bat", "蝙蝠");
        m.put("parrot", "鹦鹉");
        m.put("bee", "蜜蜂");
        m.put("llama", "羊驼");
        m.put("turtle", "海龟");
        m.put("dolphin", "海豚");
        m.put("squid", "鱿鱼");
        m.put("glow", "发光");
        m.put("fish", "鱼");
        m.put("cod", "鳕鱼");
        m.put("salmon", "鲑鱼");
        m.put("pufferfish", "河豚");
        m.put("tropical", "热带");
        m.put("axolotl", "美西螈");
        m.put("frog", "青蛙");
        m.put("tadpole", "蝌蚪");
        m.put("sniffer", "嗅探兽");
        m.put("zombie", "僵尸");
        m.put("piglin", "猪灵");
        m.put("hoglin", "疣猪兽");
        m.put("skeleton", "骷髅");
        m.put("stray", "流浪者");
        m.put("wither", "凋灵");
        m.put("creeper", "苦力怕");
        m.put("spider", "蜘蛛");
        m.put("cave", "洞穴");
        m.put("enderman", "末影人");
        m.put("endermite", "末影螨");
        m.put("silverfish", "蠹虫");
        m.put("blaze", "烈焰人");
        m.put("ghast", "恶魂");
        m.put("magmacube", "岩浆怪");
        m.put("slime", "史莱姆");
        m.put("witch", "女巫");
        m.put("guardian", "守卫者");
        m.put("elder", "远古");
        m.put("shulker", "潜影贝");
        m.put("vex", "恼鬼");
        m.put("evoker", "唤魔者");
        m.put("vindicator", "卫道士");
        m.put("pillager", "掠夺者");
        m.put("ravager", "劫掠兽");
        m.put("warden", "监守者");
        m.put("villager", "村民");
        m.put("zombie_villager", "僵尸村民");
        m.put("wandering", "流浪");
        m.put("trader", "商人");
        m.put("iron", "铁");
        m.put("golem", "傀儡");
        m.put("snow", "雪");
        m.put("dragon", "龙");
        m.put("ender", "末影");
        m.put("phoenix", "不死鸟");
        m.put("tnt", "TNT");
        // ---- weather / environment ----
        m.put("rain", "雨");
        m.put("thunder", "雷");
        m.put("lightning", "闪电");
        m.put("wind", "风");
        m.put("underwater", "水下");
        m.put("portal", "传送门");
        m.put("beacon", "信标");
        m.put("conduit", "潮涌核心");
        return m;
    }

    /** Returns the code of the language currently selected in-game. */
    private static String getCurrentLanguage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return "en_us";
        String lang = client.options.language;
        return (lang == null || lang.isEmpty()) ? "en_us" : lang;
    }

    /** Whether the current in-game language is Chinese. */
    private static boolean isChineseLanguage() {
        return getCurrentLanguage().toLowerCase().startsWith("zh");
    }

    /**
     * Lazily loads the subtitle map for the current language. Reloads when
     * the player changes the game language.
     */
    private static synchronized void ensureLoaded() {
        String language = getCurrentLanguage();
        if (currentSubtitles != null && language.equals(loadedLanguage)) return;

        currentSubtitles = new HashMap<>();
        loadedLanguage = language;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Resource resource = client.getResourceManager()
                    .getResource(Identifier.of("minecraft", "lang/" + language + ".json"))
                    .orElse(null);
            if (resource == null) {
                VividSound.LOGGER.warn("Language file not found: {}", language);
                return;
            }
            try (InputStream is = resource.getInputStream();
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonHelper.deserialize(reader);
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("subtitles.") && entry.getValue().isJsonPrimitive()) {
                        currentSubtitles.put(key, entry.getValue().getAsString());
                    }
                }
            }
            VividSound.LOGGER.info("Loaded {} subtitle entries for {}", currentSubtitles.size(), language);
        } catch (Exception e) {
            VividSound.LOGGER.warn("Failed to load subtitles for {}", language, e);
        }
    }

    /**
     * Returns the localized subtitle text for a sound. Tries, in order:
     * <ol>
     *   <li>The sound's own subtitle key from the current language file.</li>
     *   <li>The conventional {@code subtitles.<sound-path>} key.</li>
     *   <li>A fallback name for sounds with no vanilla subtitle (e.g. block
     *       footsteps), built according to the current language.</li>
     * </ol>
     * Returns {@code null} when none of the above produced any text, so the
     * caller can fall back to a readable rendering of the sound path.
     */
    public static String getLocalizedName(String subtitleKey, String soundPath) {
        ensureLoaded();
        if (subtitleKey != null && !subtitleKey.isEmpty()) {
            String name = currentSubtitles.get(subtitleKey);
            if (name != null) return name;
        }
        if (soundPath != null) {
            String name = currentSubtitles.get("subtitles." + soundPath);
            if (name != null) return name;
            String fallback = buildFallbackName(soundPath);
            if (fallback != null) return fallback;
        }
        return null;
    }

    /** Convenience overload that only has the sound path. */
    public static String getLocalizedName(String soundPath) {
        return getLocalizedName(null, soundPath);
    }

    /**
     * Builds a display name for sounds that have no vanilla subtitle entry.
     * Chinese uses the token dictionary; other languages return {@code null}
     * so the caller renders the path in the game's own (Latin) form.
     */
    private static String buildFallbackName(String soundPath) {
        if (soundPath == null || soundPath.isEmpty()) return null;

        // Footstep sounds: a single generic label instead of the block type
        // (e.g. block.grass.step, block.stone.step, ...).
        if (soundPath.endsWith(".step") || soundPath.endsWith(".steps")
                || soundPath.contains(".step.")) {
            String key = "vivid-sound.sound.step";
            String translated = I18n.translate(key);
            if (!translated.equals(key)) return translated;
            return isChineseLanguage() ? "脚步声" : "Footsteps";
        }

        return isChineseLanguage() ? buildChineseFallbackName(soundPath) : null;
    }

    /**
     * Builds a Chinese display name by splitting the sound path on dots and
     * translating each segment via {@link #ZH_TOKEN_DICT}. Segments with no
     * known translation are kept as-is (capitalised). Returns {@code null}
     * when not a single segment could be translated.
     */
    private static String buildChineseFallbackName(String soundPath) {
        String[] parts = soundPath.split("\\.");
        StringBuilder sb = new StringBuilder();
        int translated = 0;
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('·');
            String token = parts[i].toLowerCase();
            String zh = ZH_TOKEN_DICT.get(token);
            if (zh != null) {
                sb.append(zh);
                translated++;
            } else {
                // Keep unknown tokens readable: replace underscores and
                // capitalise the first letter.
                String word = token.replace('_', ' ');
                if (!word.isEmpty()) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) sb.append(word.substring(1));
                }
            }
        }
        return translated > 0 ? sb.toString() : null;
    }
}
