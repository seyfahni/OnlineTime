/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mr.minecraft15.onlinetime.bukkit;

import mr.minecraft15.onlinetime.api.PluginScheduler;
import mr.minecraft15.onlinetime.api.PluginTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class BukkitSchedulerAdapter implements PluginScheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitSchedulerAdapter(Plugin plugin, BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public PluginTask runAsyncOnceLater(long delay, Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskLater(plugin, task, delay * 20));
    }

    @Override
    public PluginTask scheduleAsync(long delay, long interval, Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskTimerAsynchronously(plugin, task, delay * 20, interval * 20));
    }
}
