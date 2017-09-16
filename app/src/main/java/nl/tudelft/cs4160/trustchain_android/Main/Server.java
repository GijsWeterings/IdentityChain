package nl.tudelft.cs4160.trustchain_android.Main;

import android.app.Activity;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import nl.tudelft.cs4160.trustchain_android.R;

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class Server {
    Activity callingActivity;
    ServerSocket serverSocket;
    String messageLog = "";
    TextView statusText;

    public Server(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

    /**
     * Starts the socketServer thread which will listen for incoming messages.
     */
    public void start() {
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        statusText = (TextView) callingActivity.findViewById(R.id.status);
    }

    private class SocketServerThread extends Thread {
        static final int SocketServerPORT = 8080;
        int count = 0;

        /**
         * Starts the serverSocket, in the while loop it starts listening for messages.
         * serverSocket.accept() blocks until a message is received.
         */
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                callingActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        statusText.setText("Server is waiting for messages...");
                    }
                });

                while (true) {
                    Socket socket = serverSocket.accept();
                    count++;
                    messageLog += "#" + count + " from " + socket.getInetAddress()
                            + ":" + socket.getPort() + "\n";
                    callingActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            statusText.setText(messageLog);
                        }
                    });

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                            socket, count);
                    socketServerReplyThread.run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class SocketServerReplyThread extends Thread {
        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        /**
         * Replies to the client that sent a message to the server.
         */
        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "Hello from Android, you are #" + cnt;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();

                messageLog += "replied: " + msgReply + "\n";
            } catch (IOException e) {
                e.printStackTrace();
                messageLog += "Something wrong! " + e.toString() + "\n";
            } finally {
                callingActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        statusText.setText(messageLog);
                    }
                });
            }
        }

    }

}
