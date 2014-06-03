
package edu.buffalo.cse.cse486586.groupmessenger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static final String[] portGroup = {
            REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4
    };
    static int COUNT = 0;
    ContentResolver CONTENT_RESOLVER;
    private static final String COLUMN_1 = "key";
    private static final String COLUMN_2 = "value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no
         * grading component on how you display the messages, if you implement
         * it, it'll make your debugging easier.
         */

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setReuseAddress(true);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        CONTENT_RESOLVER = getContentResolver();

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is
         * the "PTest" button. OnPTestClickListener demonstrates how to access a
         * ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the
         * "Send" button. In your implementation you need to get the message
         * from the input box (EditText) and send it to other AVDs in a
         * total-causal order.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.

                String myPort = getMyPort();
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

    }

    /***
     * Calculate the port number that this AVD listens on.
     * 
     * @author vbhalla
     */
    private String getMyPort() {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        return myPort;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages and also
     * ServerTask of REMOTE_PORT0 i.e. avd0 acts as a sequencer. The sequencer
     * assigns proper sequence to the messages and B-Multicasts them to all
     * other avd's. ServerTask is created by ServerTask.executeOnExecutor() call
     * in GroupMessengerActivity.
     * 
     * @author vbhalla
     */
    private class ServerTask extends AsyncTask<ServerSocket, MessageWrapper, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            ObjectInputStream input;
            Socket socket = null;
            MessageWrapper msg;

            do {
                try {
                    // Accept Connection and Initialize Input Stream
                    socket = serverSocket.accept();
                    input = new ObjectInputStream(socket.getInputStream());
                    msg = (MessageWrapper) input.readObject();

                    if (msg.isMulticast()) {

                        msg.setSeq_no(COUNT++);
                        msg.setMulticast(false);

                        // Centralized Multicast
                        for (int i = 0; i < portGroup.length; i++) {
                            Socket socket1 = new Socket(InetAddress.getByAddress(new byte[] {
                                    10, 0, 2, 2
                            }),
                                    Integer.parseInt(portGroup[i]));
                            ObjectOutputStream output = new ObjectOutputStream(
                                    socket1.getOutputStream());

                            output.writeObject(msg);
                            socket1.close();
                        }

                    } else {

                        ContentValues cv = new ContentValues();
                        cv.put(COLUMN_1, Integer.toString(msg.getSeq_no()));
                        cv.put(COLUMN_2, msg.getMsg());
                        CONTENT_RESOLVER.insert(GroupMessengerProvider.providerUri, cv);

                        publishProgress(msg);

                    }

                } catch (ClassNotFoundException e) {
                    Log.e("ServerTask", "ClassNotFoundException");
                } catch (IOException e) {
                    Log.e("ServerTask", "IOException");
                }

            } while (!socket.isInputShutdown());

            return null;
        }

        @Override
        protected void onProgressUpdate(MessageWrapper... messages) {

            String key = Integer.toString(messages[0].getSeq_no());
            String value = messages[0].getMsg();

            /*
             * The following code displays what is received in doInBackground().
             */
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(key + " : " + value + "\n");

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network. It
     * is created by ClientTask.executeOnExecutor() call in
     * GroupMessengerActivity, whenever OnClickListener.onClick() detects an
     * Send button press event.
     * 
     * @author vbhalla
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                // Send all messages to the Sequencer which is
                // avd0/Emulator-5554 with Remote Port = 11108
                Socket socket = new Socket(InetAddress.getByAddress(new byte[] {
                        10, 0, 2, 2
                }),
                        Integer.parseInt(REMOTE_PORT0));

                // Initialize Output Stream
                MessageWrapper message = new MessageWrapper();
                message.setMsg(msgs[0]);
                message.setIdentifier(msgs[1]);
                message.setSeq_no(COUNT);

                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

                output.writeObject(message);
                output.flush();
                socket.close();

            } catch (UnknownHostException e) {
                Log.e("ClientTask", "UnknownHostException");
            } catch (IOException e) {
                Log.e("ClientTask", "Socket IOException");
            }

            return null;
        }
    }
}
