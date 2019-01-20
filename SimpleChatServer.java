import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;


/**
 * A multi-client simple chat server that will accept incoming connections, and which
 * will broadcast anything received from one client to all other clients.
 *
 */


public class SimpleChatServer {
    private ArrayList<ChatHandler> handlers = new ArrayList<ChatHandler>();

    /**
     * Start the server and handle incoming connections
     * @param port TCP port to listen on
     */

    public void handle(int port) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // create new socket server
        //ServerSocket server = null;
        //try {
        //	server = new ServerSocket(port);
        //} catch (IOException e) {
        //	e.printStackTrace();
        //}

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        SSLSocket socket = (SSLSocket) factory.createSocket();

        //BufferedReader in = new BufferedReader(socket.getOutputStream());
        //PrintWriter out = new PrintWriter(socket.getInputStream());

        BufferedReader in = null;
        PrintWriter out = null;

        // load keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream ksIs = new FileInputStream("./keystore.jks");
        ks.load(ksIs, "password".toCharArray());

        // have keymanager operate using keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        // create ssl context
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), null, new SecureRandom());

        // start server
        SSLServerSocket server = (SSLServerSocket)sc.getServerSocketFactory().createServerSocket(port);


        while (true) {
            // WRITE ME: accept connection, extract streams and start thread
            Socket sock5 = null;

            try {

                sock5 = server.accept();
                in = new BufferedReader(new InputStreamReader(sock5.getInputStream()));
                out = new PrintWriter(sock5.getOutputStream(),true);

            } catch (Exception e) {
                System.err.println(e);
            }
            //start thread
            assert sock5 != null;
            ChatHandler client = new ChatHandler(in, out);
            Thread t = new Thread(client);
            t.start();

            // register callback
            client.registerCallback(this);
            handlers.add(client);

        }
    }

    /**
     * Send chat message to all clients, except originator
     * @param text Text to send
     * @param except Client to exclude
     */


    public void sendToAll(String text, ChatHandler except) {
        for (ChatHandler ch : handlers) {
            if (ch != except) {
                ch.send(text);
            }

        }

    }

    /**
     * Called when a client disconnects.
     * @param c Disconnecting client
     */

    public void leave(ChatHandler c) {
        int i = handlers.indexOf(c);
        synchronized(System.out) {
            if (i != -1) {
                System.out.print("Client disconnected. ");
                handlers.remove(c);		// WRITE ME: send message to all channels, except to sender

            }
            System.out.println(handlers.size()+" clients remaining.");
        }
    }

    /**
     * Driver method
     */
    public static void main(String args[]) {
        int port = 9443;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        SimpleChatServer ob = new SimpleChatServer();
        try {
            ob.handle(port);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }


    /**
     * Single thread implementation
     */
    class ChatHandler implements Runnable {
        private BufferedReader in;
        private PrintWriter out;
        private SimpleChatServer master;

        /**
         * Constructor
         * @param in Input stream
         * @param out Output stream
         */
        public ChatHandler(BufferedReader in, PrintWriter out) {
            this.in = in;
            this.out = out;
        }

        public ChatHandler(BufferedReader in, PrintWriter out, String s) {
        }

        /**
         * Register chat server handler for callbacks
         */
        protected void registerCallback(SimpleChatServer c) {
            this.master = c;
        }

        /**
         * Display text to client
         * @param text Text to be displayed
         */
        protected void send(String text) {
            out.printf("%s\r\n", text);
        }

        @Override
        /**
         * Thread logic
         */
        public void run() {

            /* WRITE ME: read line-by-line. After each line read, callback to
             * server app to send input to all.
             *
             * OPTIONAL: the word QUIT will disconnect a client.
             */
            while (true) {
                String input = null;
                try {
                    input = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                if (input == null) {

                    master.sendToAll("User connection has dropped.", null);
                    master.leave(this);
                    break;

                }

                if (input.equals("QUITS")) {

                    master.sendToAll("User has disconnected from the channel.", null);
                    master.leave(this);
                    break;

                }
                // don't send users blank lines
                if (input.equals("")) {
                    master.sendToAll("Must enter text to send.", null);

                }

                //socket.startHandshake();
                master.sendToAll(input, this);
            }

        }
    }
}
