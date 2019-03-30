package edu.gmu.cs675.client;

import java.io.IOException;
import java.time.Instant;
import java.util.Scanner;

public class ClockCLient {

    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Kindly enter the IP for sync server ");
        String serverIP = scanner.nextLine();
        ClockClientSocket clockClientSocket = new ClockClientSocket(serverIP);
        try {
            System.out.println(clockClientSocket.serverConnectionPoint("Hello World"));
        } catch (IOException e) {
            System.out.println("Couldn't connect to server");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
