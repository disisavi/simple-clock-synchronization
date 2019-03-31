package edu.gmu.cs675.client;

import java.io.IOException;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClockCLient {
    static Integer id = 0;

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Kindly enter the IP for sync server");
        String serverIP = scanner.nextLine();
        ClockClientSocket clockClientSocket = new ClockClientSocket(serverIP);
        System.out.println("Please enter the amount of time (in hours) you need to synchronize for ");
        ClockCLient cLient = new ClockCLient();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
        System.out.println("Thread id in main " + Thread.currentThread().getId());
        scheduler.scheduleAtFixedRate(() -> cLient.run(clockClientSocket), 1, 1, TimeUnit.SECONDS);

    }

    void run(ClockClientSocket clockClientSocket) {

        id++;

        System.out.println("Sending packet " + id + " from Thread id " + Thread.currentThread().getId());
        String now = Long.toString(Instant.now().toEpochMilli());
        String messageToServer = id + " " + now;
        try {
            String messageFromServer = clockClientSocket.serverConnectionPoint(messageToServer);
        } catch (IOException e) {
            System.out.println("The packet number " + id + " failed because " + e.getMessage());
        }
    }
}
