package com.yiva.jet_elytra.jet_elytra;

import com.yiva.jet_elytra.jet_elytra.gameplay.JetDiagnostics;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Every tunable from the design document, exposed as a server-side (world) config so the values can
 * be dialled in during play-testing.
 *
 * <p>The static fields below hold the live values and default to the calibrated design numbers, so
 * they are always usable even when no config file has been loaded yet (for example on a client that
 * is still on the main menu).</p>
 *
 * <p><b>玩家实际看到的说明在哪里。</b> Forge 会把每个 {@code .comment(...)} 的文本原样写进生成的
 * TOML，放在对应键的上方，后面再跟一行自动生成的 {@code #Range: 最小值 ~ 最大值}。所以"这个值是什么、
 * 什么单位、往哪调、调了会怎样"这些内容必须写在这些字符串里 —— 写在旁边的 Java 注释里玩家是看不到的。
 * Forge 会去掉开头多余的 {@code #}；而 {@code defineInRange} 意味着手改超出范围的值只会被夹到边界，
 * 不会导致游戏崩溃。</p>
 *
 * <p>注释一律用中文：这些键是给中文玩家调的。</p>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class JetConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue FUEL_CAPACITY = BUILDER
            .comment("燃料箱容量，单位是燃料点。按默认消耗速度，20 点 = 1 秒推进。",
                    "调大可以延长两次补给之间的飞行时间。",
                    "调小的话，已有存档里超出上限的燃料会在下次加载时被压到新上限。")
            .defineInRange("fuelCapacity", 200.0D, 1.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_CONSUMPTION_PER_SECOND = BUILDER
            .comment("推进时每秒消耗的燃料。按默认值算，满箱可连续推进 8 秒。",
                    "调小则推进更省油；设为 0 则完全不消耗燃料。")
            .defineInRange("fuelConsumptionPerSecond", 25.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_REFILL_IN_FLIGHT_PER_SECOND = BUILDER
            .comment("停止推进后，仍在滑翔状态下每秒恢复的燃料。",
                    "刻意低于消耗速度，这样一直按住推进键就一定会亏燃料，不会自给自足。",
                    "如果调到高于消耗速度，就能无限续航。")
            .defineInRange("fuelRefillInFlightPerSecond", 12.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_REFILL_OUT_OF_FLIGHT_PER_SECOND = BUILDER
            .comment("已经不在滑翔状态（落地、走路、骑乘）时每秒恢复的燃料，是两档里的快档。",
                    "它负责把燃料补回来，让下一次起飞有油可用。",
                    "设为 0 则只能靠烟花火箭补充燃料。")
            .defineInRange("fuelRefillOutOfFlightPerSecond", 30.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.IntValue REFILL_DELAY_TICKS = BUILDER
            .comment("推进停止后，等待多少 tick 才开始恢复燃料，滑翔档和落地档共用这个延迟。",
                    "20 tick = 1 秒。这个延迟是为了防止连点推进键时燃料被偷偷回满；",
                    "设为 0 则立即开始恢复。")
            .defineInRange("refillDelayTicks", 20, 0, 72000);

    public static final ForgeConfigSpec.DoubleValue MIN_FUEL_TO_START = BUILDER
            .comment("进入推进模式所需的最低燃料。低于此值时按前进键没有反应，必须先落地补燃料。",
                    "已经在推进中的不受此限制，会一直烧到 0 —— 所以空中烧干会直接断推。",
                    "设为 0 则空箱也能起推（但下一个 tick 就会停）。")
            .defineInRange("minFuelToStart", 20.0D, 0.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue AUTO_REFUEL_THRESHOLD = BUILDER
            .comment("滑翔期间，燃料降到该值及以下时自动消耗背包里的烟花火箭。",
                    "设为 0 等于关闭自动补充，只剩右键手动补充。",
                    "每支火箭补充多少，见下面三个 fireworkFuelFlightN。")
            .defineInRange("autoRefuelThreshold", 50.0D, 0.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue MAX_SPEED_BLOCKS_PER_SECOND = BUILDER
            .comment("推进的终端速度，单位是格/秒：加速到这个速度后就保持不变。",
                    "原版滑翔大约只有 18 格/秒，所以高于它的数值才会有\"被推着走\"的感觉。",
                    "调高这个值时建议同时调低 accelerationTicks，否则最高速度越高、加速也越久。")
            .defineInRange("maxSpeedBlocksPerSecond", 33.5D, 0.0D, 1000.0D);

    public static final ForgeConfigSpec.IntValue ACCELERATION_TICKS = BUILDER
            .comment("从当前速度加速到终端速度所需的 tick 数；松开按键后的减速也用同一速率。",
                    "20 tick = 1 秒，所以 60 就是 3 秒到满速。",
                    "数值越小越跟手，越大加速越平缓。")
            .defineInRange("accelerationTicks", 60, 1, 72000);

    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT = BUILDER
            .comment("推进时每 tick 在玩家身后生成的烟花尾迹粒子数量。纯视觉效果，设为 0 关闭尾迹。")
            .defineInRange("particleCount", 2, 0, 64);

    public static final ForgeConfigSpec.DoubleValue PARTICLE_SPREAD = BUILDER
            .comment("每个尾迹粒子的随机偏移量，单位是格。数值越大尾迹越散，越小越像一条细线。")
            .defineInRange("particleSpread", 0.3D, 0.0D, 8.0D);

    public static final ForgeConfigSpec.IntValue SYNC_INTERVAL_TICKS = BUILDER
            .comment("服务端每隔多少 tick 把燃料值同步给对应客户端。5 tick = 每秒 4 次。",
                    "当显示数字快要变化时 HUD 也会立即更新，所以这个值只决定最坏情况下的延迟。",
                    "调大会减少网络流量，但燃料条跳动更明显。")
            .defineInRange("syncIntervalTicks", 5, 1, 200);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_1 = BUILDER
            .comment("飞行时间 1 的烟花火箭（最短的那种）能补充的燃料量。")
            .defineInRange("fireworkFuelFlight1", 50, 0, 100000);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_2 = BUILDER
            .comment("飞行时间 2 的烟花火箭能补充的燃料量。")
            .defineInRange("fireworkFuelFlight2", 100, 0, 100000);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_3 = BUILDER
            .comment("飞行时间 3 的烟花火箭（最长的那种）能补充的燃料量。",
                    "其它飞行时间的火箭一律按飞行时间 1 计算。")
            .defineInRange("fireworkFuelFlight3", 150, 0, 100000);

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("是否输出 logs/jet_elytra-diagnostic.log：每 tick 一行，",
                    "覆盖 输入 -> 网络包 -> 推力判定 -> 速度 的完整链路。",
                    "这是排查问题用的，正常游玩请保持关闭，否则日志会迅速变大。")
            .define("debugLog", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ---- 运行时实际读取的值 ----
    //
    // 这些才是模拟逻辑真正读取的值，由下面的 load() 从 TOML 覆盖；
    // 这里的数字仅作为尚未加载配置时的兜底（例如客户端还停在主菜单）。
    // 改这里等于改"新世界的默认值"；要改已有世界，请改该存档下的
    // serverconfig/jet_elytra-server.toml。

    public static double fuelCapacity = 200.0D;
    public static double fuelConsumptionPerSecond = 25.0D;
    public static double fuelRefillInFlightPerSecond = 12.0D;
    public static double fuelRefillOutOfFlightPerSecond = 30.0D;
    public static int refillDelayTicks = 20;
    public static double minFuelToStart = 20.0D;
    public static double autoRefuelThreshold = 50.0D;
    public static double maxSpeedBlocksPerSecond = 33.5D;
    public static int accelerationTicks = 60;
    public static int particleCount = 2;
    public static double particleSpread = 0.3D;
    public static int syncIntervalTicks = 5;
    public static int fireworkFuelFlight1 = 50;
    public static int fireworkFuelFlight2 = 100;
    public static int fireworkFuelFlight3 = 150;
    public static boolean debugLog = false;

    private JetConfig() {
    }

    /** Fuel burned per tick while thrusting. */
    public static double consumptionPerTick() {
        return fuelConsumptionPerSecond / 20.0D;
    }

    /**
     * Fuel regenerated per tick while fall-flying. Slower than the grounded rate on purpose: a glide
     * should not be able to sustain the jet indefinitely without the player spending rockets.
     */
    public static double refillPerTickInFlight() {
        return fuelRefillInFlightPerSecond / 20.0D;
    }

    /** Fuel regenerated per tick while not fall-flying; the faster of the two rates. */
    public static double refillPerTickOutOfFlight() {
        return fuelRefillOutOfFlightPerSecond / 20.0D;
    }

    /** Terminal speed in blocks per tick. */
    public static double maxSpeedPerTick() {
        return maxSpeedBlocksPerSecond / 20.0D;
    }

    /** Per-tick speed increment applied while ramping up. */
    public static double accelerationPerTick() {
        return maxSpeedPerTick() / Math.max(1, accelerationTicks);
    }

    /** Fuel granted by a rocket of the given flight duration; anything unexpected counts as level 1. */
    public static int fuelForFlightDuration(int flight) {
        return switch (flight) {
            case 2 -> fireworkFuelFlight2;
            case 3 -> fireworkFuelFlight3;
            default -> fireworkFuelFlight1;
        };
    }

    @SubscribeEvent
    static void onConfigLoad(final ModConfigEvent event) {
        load();
    }

    @SubscribeEvent
    static void onConfigReload(final ModConfigEvent.Reloading event) {
        load();
    }

    /** Pulls the live values out of the spec, falling back to the design numbers when unavailable. */
    public static void load() {
        try {
            fuelCapacity = FUEL_CAPACITY.get();
            fuelConsumptionPerSecond = FUEL_CONSUMPTION_PER_SECOND.get();
            fuelRefillInFlightPerSecond = FUEL_REFILL_IN_FLIGHT_PER_SECOND.get();
            fuelRefillOutOfFlightPerSecond = FUEL_REFILL_OUT_OF_FLIGHT_PER_SECOND.get();
            refillDelayTicks = REFILL_DELAY_TICKS.get();
            minFuelToStart = MIN_FUEL_TO_START.get();
            autoRefuelThreshold = AUTO_REFUEL_THRESHOLD.get();
            maxSpeedBlocksPerSecond = MAX_SPEED_BLOCKS_PER_SECOND.get();
            accelerationTicks = ACCELERATION_TICKS.get();
            particleCount = PARTICLE_COUNT.get();
            particleSpread = PARTICLE_SPREAD.get();
            syncIntervalTicks = SYNC_INTERVAL_TICKS.get();
            fireworkFuelFlight1 = FIREWORK_FUEL_FLIGHT_1.get();
            fireworkFuelFlight2 = FIREWORK_FUEL_FLIGHT_2.get();
            fireworkFuelFlight3 = FIREWORK_FUEL_FLIGHT_3.get();
            debugLog = DEBUG_LOG.get();
            JetDiagnostics.enabled = debugLog;
        } catch (final IllegalStateException notLoadedYet) {
            // Config not available on this side yet - keep the defaults.
        }
    }
}
