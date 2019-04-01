package edu.gmu.cs675.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClockCLient {
    Integer id;
    ScheduledFuture future;
    ScheduledExecutorService scheduler;
    ClockClientSocket clockClientSocket;
    final int packetsToSend;
    Writer fileWriter;

    public ClockCLient(String ip) {

        this.id = 0;
        packetsToSend = this.numberOfPacketsToSend();
        try {
            fileWriter = new PrintWriter("time.txt", "UTF-8");
            fileWriter.write("-----------------------------------------------------------\n\tTime Analysis over "
                    + packetsToSend + " packets");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.clockClientSocket = new ClockClientSocket(ip);
        this.scheduler = Executors.newScheduledThreadPool(100);
        this.future = scheduler.scheduleAtFixedRate(this::run, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Kindly enter the IP for sync server");
        String serverIP = scanner.nextLine();
        ClockCLient clockCLient = new ClockCLient(serverIP);
        System.out.println("\n\nThe number of packets t0 be sent is " + clockCLient.packetsToSend);
        clockCLient.scheduler.schedule(() -> {

            try {
                clockCLient.fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clockCLient.future.cancel(false);
            clockCLient.scheduler.shutdown();

            System.out.println("Cancelled");
        }, clockCLient.packetsToSend * 10, TimeUnit.SECONDS);
    }

    void run() {
        id++;
//        System.out.println("Sending packet " + id + " from Thread id " + Thread.currentThread().getId());
        String now = Long.toString(Instant.now().toEpochMilli());
        String messageToServer = id + " " + now;
        try {
            String messageFromServer = this.clockClientSocket.serverConnectionPoint(messageToServer);
            messageFromServer = messageFromServer + " " + Instant.now().toEpochMilli();
            System.out.println(messageFromServer);
            processTime(messageFromServer);
        } catch (IOException e) {
            System.out.println("The packet number " + id + " failed because " + e.getMessage());
        }

    }

    int numberOfPacketsToSend() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the amount of time (in hours) you need to synchronize for ");
        System.out.println("Common Examples\n\t0.0166667 for 1 minutes\n\t0.16667 for 10 minutes\n\t0.5 for 30 minutes");
        float timeInHours = scanner.nextFloat();
        return (int) Math.rint((timeInHours * 60 * 60) / 10);
    }

    void processTime(String messages) {
        String time[] = messages.split(" ", 0);
        long t0 = Long.parseLong(time[4]);
        long t1 = Long.parseLong(time[3]);
        long t2 = Long.parseLong(time[2]);
        long t3 = Long.parseLong(time[1]);

        long rtt = (t2 - t3) + (t0 - t1);
        long theta = rtt / 2;
        try {
            fileWriter.write("\nid - " + id + " rtt : " + rtt + "; theta : " + theta);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}