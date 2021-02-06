package com.android.app_2_faces_net;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskRunner {

    private static final int CORE_THREADS = 3;
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static TaskRunner taskRunner = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor executor;

    private TaskRunner() {
        executor = newThreadPoolExecutor();
    }

    public static TaskRunner getInstance() {
        if (taskRunner == null) {
            taskRunner = new TaskRunner();
        }
        return taskRunner;
    }

    public void shutdownService() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void execute(Runnable command) {
        executor.execute(command);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public <R> void executeCallable(@NonNull Callable<R> callable, @NonNull OnCompletedCallback<R> callback) {
        executor.execute(() -> {
            R result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                e.printStackTrace(); // log this exception
            } finally {
                final R finalResult = result;
                handler.post(() -> callback.onComplete(finalResult));
            }
        });
    }

    private ThreadPoolExecutor newThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                CORE_THREADS,
                Integer.MAX_VALUE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
        );
    }

    public interface OnCompletedCallback<R> {
        void onComplete(@Nullable R result);
    }
}
