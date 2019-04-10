package edu.gmu.cs675.server;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    public InetAddress inetAddress;
    private int port = 1024;
    private Map<String, String> packetsRecivedMap = new ConcurrentHashMap<>();

    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 1024);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }


    public void startSocketServer() {
        try {
            DatagramSocket socket = new DatagramSocket(this.port);
            this.inetAddress = getSelfIP();
            System.out.println("Clock Server Startup Complete");
            System.out.println("ip -- " + this.inetAddress.getHostAddress());
            Thread clock = new Thread(new SocketListner(socket, packetsRecivedMap));
            clock.start();
            exitCommand();
        } catch (IOException e) {
            System.out.println("Problem in starting server ... " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void exitCommand() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1. Enter S for Number of packets received");
        System.out.println("2. Please Enter 'E' for exit");
        while (true) {
            try {
                char input = scanner.nextLine().toUpperCase().charAt(0);
                if (input == 'E') {
                    System.exit(1);
                } else if (input == 'S') {
                    System.out.println("Number of packets received = " + packetsRecivedMap.size());
                } else {
                    System.out.println("Please enter the right command.");
                }
            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("Kindly enter a command");
            }
        }
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        SocketServer socketServer = new SocketServer();
        socketServer.startSocketServer();
    }
}
