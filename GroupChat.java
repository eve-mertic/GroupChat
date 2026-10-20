import java.net.*;
import java.io.*;
import java.util.*;

public class GroupChat {
    private static final String TERMINATE = "Exit";
    static String name;
    static volatile boolean finished = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Two arguments required: <name> <port-number>");
        } else {
            try {
                InetAddress group = InetAddress.getByName(args[0]);
                if (!group.isMulticastAddress()) {
                    System.out.println("Invalid multicast address");
                    return;
                }

                int port = Integer.parseInt(args[1]);
                if (port < 1024 || port > 65535) {
                    System.out.println("Error: port number must be in the range 1024 to 65535");

                    return;
                }

                Scanner sc = new Scanner(System.in);
                System.out.print("Enter your name: ");
                name = sc.nextLine();

                MulticastSocket socket = new MulticastSocket(port);

                socket.setTimeToLive(1);

                Thread t = new Thread(new ReadThread(socket, group, port));
                t.start();

                System.out.println("Start typing messages.....\n");

                try {
                    while (true) {
                        String message = sc.nextLine();
                        if (message.equals(TERMINATE)) {
                            finished = true;
                            socket.leaveGroup(group);
                            socket.close();
                            break;
                        }
                        message = name + ": " + message;
                        byte[] buffer = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
                        socket.send(packet);
                    }
                } finally {
                    if (!socket.isClosed()) {
                        socket.leaveGroup(group);
                        socket.close();
                    }
                }
                sc.close();
            }

            catch (UnknownHostException uhe) {
                System.out.println("Error: " + uhe.getMessage());
                uhe.printStackTrace();
            } catch (NumberFormatException e) {
                System.out.println("Error: port number must be an integer");
                e.printStackTrace();
            } catch (SocketException se) {
                System.out.println("Error: " + se.getMessage());
                se.printStackTrace();
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}

class ReadThread implements Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private static final int MAX_LENGTH = 1000;

    public ReadThread(MulticastSocket socket, InetAddress group, int port) {
        this.socket = socket;
        this.group = group;
        this.port = port;
    }

    @Override
    public void run() {
        while (!GroupChat.finished) {
            byte[] buffer = new byte[ReadThread.MAX_LENGTH];
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length, group, port);
            try {
                socket.receive(datagram);
                String message = new String(buffer, 0, datagram.getLength(), "UTF-8");
                if (!message.startsWith(GroupChat.name)) {
                    System.out.println(message);
                }
            } catch (SocketException se) {
                if (!GroupChat.finished) {
                    System.out.println("Error: " + se.getMessage());
                    se.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }
}
