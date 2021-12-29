/*
 * Copyright 2020-2021 AVSystem <avsystem@avsystem.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.avsystem.anjay;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a standard Anjay event loop. In many cases it might be sufficient to build its more
 * complex extensions and for this purpose methods {@link AnjayEventLoop#schedRun()} and {@link
 * AnjayEventLoop#serveAny()} are provided. In particular, this code: <code>
 * while (true) {
 *     eventLoop.serveAny();
 *     eventLoop.schedRun();
 * }
 * </code> is equivalent to {@link AnjayEventLoop#run()}, assuming {@link
 * AnjayEventLoop#interrupt()} won't be called.
 */
public final class AnjayEventLoop implements Closeable {
    private final Anjay anjay;
    private final long maxWaitTime;
    private volatile Thread thread;

    private abstract static class EventLoopTask
            implements Consumer<AnjayEventLoop>, Comparable<EventLoopTask> {
        private final Instant time;

        EventLoopTask(Instant time) {
            this.time = time;
        }

        @Override
        public int compareTo(EventLoopTask task) {
            return time.compareTo(task.time);
        }
    }

    private final PriorityBlockingQueue<EventLoopTask> eventLoopTasks =
            new PriorityBlockingQueue<>();
    private final Selector eventLoopSelector;

    /**
     * @param anjay {@link Anjay} object used by the event loop
     * @param maxWaitTime Maximum time (in milliseconds) to spend in each call to {@link
     *     Selector#select(long)}. Larger times will prevent the event loop thread from waking up
     *     too frequently. However, during this wait time, the call to {@link
     *     AnjayEventLoop#interrupt()} may not be handled immediately and scheduler jobs requested
     *     from other threads will not be executed, so shortening this time will make the scheduler
     *     more responsive.
     * @throws IllegalArgumentException if the timeout is negative
     * @throws IOException thrown by {@link Selector#open()}
     */
    public AnjayEventLoop(Anjay anjay, long maxWaitTime) throws IOException {
        if (maxWaitTime < 0L) {
            throw new IllegalArgumentException("Maximum wait time must be non-negative");
        }

        this.anjay = anjay;
        this.maxWaitTime = maxWaitTime;
        eventLoopSelector = Selector.open();
    }

    /**
     * Calls {@link Selector#select(long)} on all sockets currently in use and then {@link
     * Anjay#serve(SelectableChannel)} if appropriate.
     *
     * <p>This is intended as a building block for custom event loops.
     *
     * @throws ClosedChannelException thrown by {@link SelectableChannel#register(Selector, int)}.
     * @throws IOException thrown by {@link Selector#select(long)} or {@link Selector#selectNow()}.
     */
    public synchronized void serveAny() throws IOException {
        List<SelectableChannel> sockets = anjay.getSockets();

        for (SelectionKey key : eventLoopSelector.keys()) {
            if (!sockets.contains(key.channel())) {
                key.cancel();
            }
        }
        for (SelectableChannel socket : sockets) {
            if (socket.keyFor(eventLoopSelector) == null) {
                socket.register(eventLoopSelector, SelectionKey.OP_READ);
            }
        }

        long waitTimeMs = anjay.timeToNext().map(Duration::toMillis).orElse(maxWaitTime);
        if (waitTimeMs > maxWaitTime) {
            waitTimeMs = maxWaitTime;
        }
        if (!eventLoopTasks.isEmpty()
                && eventLoopTasks.peek().time.isBefore(Instant.now().plusMillis(waitTimeMs))) {
            waitTimeMs = Duration.between(Instant.now(), eventLoopTasks.peek().time).toMillis();
        }
        if (waitTimeMs <= 0) {
            eventLoopSelector.selectNow();
        } else {
            eventLoopSelector.select(waitTimeMs);
        }
        for (Iterator<SelectionKey> it = eventLoopSelector.selectedKeys().iterator();
                it.hasNext(); ) {
            try {
                anjay.serve(it.next().channel());
            } catch (Throwable t) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Anjay::serve() failed");
            }
            it.remove();
        }
    }

    /**
     * Runs Anjay tasks and those from the eventLoopTasks while there is some task ready to be run.
     *
     * <p>This is intended as a building block for custom event loops.
     *
     * @throws InterruptedException thrown by {@link PriorityBlockingQueue#take()}.
     */
    public synchronized void schedRun() throws InterruptedException {
        Instant now = Instant.now();
        List<EventLoopTask> activeTasks = new LinkedList<>();
        while (eventLoopTasks.peek() != null && eventLoopTasks.peek().time.compareTo(now) <= 0) {
            activeTasks.add(eventLoopTasks.take());
        }
        for (EventLoopTask task : activeTasks) {
            task.accept(this);
        }
        if (!thread.isInterrupted() && anjay.timeToNext().map(Duration::toMillis).orElse(1L) <= 0) {
            anjay.schedRun();
        }
    }

    /**
     * Runs Anjay's main event loop that executes {@link Anjay#serve(SelectableChannel)} and {@link
     * Anjay#schedRun()} as appropriate.
     *
     * <p>This function will only return after either:
     *
     * <ul>
     *   <li>the thread running {@link AnjayEventLoop#run()} will be interrupted, (e.g. by {@link
     *       AnjayEventLoop#interrupt()}), or
     *   <li>a fatal error (e.g. out-of-memory or I/O error) occurs in the loop itself (errors from
     *       {@link Anjay#serve(SelectableChannel)} are <em>not</em> considered fatal)
     * </ul>
     *
     * @throws IOException thrown by {@link AnjayEventLoop#serveAny()}.
     */
    public synchronized void run() throws IOException {
        thread = Thread.currentThread();

        try {
            while (!thread.isInterrupted()) {
                serveAny();
                schedRun();
            }
        } catch (InterruptedException e) {
        } finally {
            thread = null;
        }
    }

    /**
     * Schedules an event loop task with a time in which it should be runned.
     *
     * @param task Task to be run in the event loop, it takes AnjayEventLoop as an argument, which
     *     is meant to serve mainly for task rescheduling.
     * @param time Time when the task should be run.
     */
    public void scheduleTask(Consumer<AnjayEventLoop> task, Instant time) {
        eventLoopTasks.add(
                new EventLoopTask(time) {
                    @Override
                    public void accept(AnjayEventLoop anjayEventLoop) {
                        task.accept(anjayEventLoop);
                    }
                });
    }

    /**
     * Interrupts the thread on which the event loop is running.
     *
     * @throws IllegalStateException if the event loop is not running
     */
    public void interrupt() {
        Thread eventLoopThread = thread;
        if (eventLoopThread == null) {
            throw new IllegalStateException();
        } else {
            eventLoopThread.interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        eventLoopSelector.close();
    }
}
