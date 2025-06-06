package net.vrcub.play.itembank.tools;


import cn.handyplus.lib.adapter.HandyRunnable;
import cn.handyplus.lib.adapter.HandySchedulerUtil;
import net.vrcub.play.itembank.ItemBank;

public class RunnableUtil {
    public static void initScheduler(ItemBank plugin) {
        HandySchedulerUtil.init(plugin);
    }

    public static void runTask(HandyRunnable runnable) {
        HandySchedulerUtil.runTask(runnable);
    }

    public static void runTaskLater(HandyRunnable runnable, long delay) {
        HandySchedulerUtil.runTaskLater(runnable, delay);
    }

    public static void runTaskTimer(HandyRunnable runnable, long delay, long period) {
        HandySchedulerUtil.runTaskTimer(runnable, delay, period);
    }

    public static void runTaskAsynchronously(HandyRunnable runnable) {
        HandySchedulerUtil.runTaskAsynchronously(runnable);
    }

    public static void runTaskLaterAsynchronously(HandyRunnable runnable, int delay) {
        HandySchedulerUtil.runTaskLaterAsynchronously(runnable, delay);
    }

    public static void runTaskTimerAsynchronously(HandyRunnable runnable, int delay, int timer) {
        HandySchedulerUtil.runTaskTimerAsynchronously(runnable, delay, timer);
    }
    public static void runTask(Runnable runnable) {
        HandySchedulerUtil.runTask(runnable);
    }

    public static void runTaskLater(Runnable runnable, long delay) {
        HandySchedulerUtil.runTaskLater(runnable, delay);
    }

    public static void runTaskTimer(Runnable runnable, long delay, long period) {
        HandySchedulerUtil.runTaskTimer(runnable, delay, period);
    }

    public static void runTaskAsynchronously(Runnable runnable) {
        HandySchedulerUtil.runTaskAsynchronously(runnable);
    }

    public static void runTaskLaterAsynchronously(Runnable runnable, int delay) {
        HandySchedulerUtil.runTaskLaterAsynchronously(runnable, delay);
    }

    public static void runTaskTimerAsynchronously(Runnable runnable, int delay, int timer) {
        HandySchedulerUtil.runTaskTimerAsynchronously(runnable, delay, timer);
    }
    public static void cancelTask() {
        HandySchedulerUtil.cancelTask();
    }
}
