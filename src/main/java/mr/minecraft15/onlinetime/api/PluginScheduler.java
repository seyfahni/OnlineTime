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

package mr.minecraft15.onlinetime.api;

/**
 * Representation of a basic scheduler. Decouples the underlying APIs scheduler as interface for adapters.
 */
public interface PluginScheduler {

    /**
     * Execute a task asynchronously.
     *
     * @param task task to run
     * @return task representation
     */
    PluginTask runAsyncOnce(Runnable task);

    /**
     * Execute a task asynchronously after a fixed amount of time.
     *
     * @param delay seconds to delay execution
     * @param task task to run
     * @return task representation
     */
    PluginTask runAsyncOnceLater(long delay, Runnable task);

    /**
     * Execute a task asynchronously every interval seconds. The execution starts after delay seconds.
     *
     * @param delay seconds to delay execution
     * @param interval seconds to sleep inbetween executions
     * @param task task to run
     * @return task representation
     */
    PluginTask scheduleAsync(long delay, long interval, Runnable task);
}
