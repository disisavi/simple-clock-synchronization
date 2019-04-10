package edu.gmu.cs675.client;

import org.decimal4j.util.DoubleRounder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ClockClient {
    Integer id;
    ScheduledFuture future;
    ScheduledExecutorService scheduler;
    ClockClientSocket clockClientSocket;
    final int packetsToSend;
    Writer fileWriter;
    SortedSet<Integer> packetReceivedSet;
    Set<Integer> droppedPacketsSet;
    Calendar localCLienTime;
    List<Map.Entry<Long, Double>> smoothTheta;
    double averageTheta;
    double averageRoundTrip;
    float timeInHours;
    Map<Double, Integer> thetaFrequency;

    public ClockClient(String ip) {


        this.packetsToSend = this.numberOfPacketsToSend();
        initialiseVariables(ip);
        this.initializeFile();
        this.initializeSchedule();
    }

    public ClockClient(String ip, String bashTime) throws NumberFormatException {

        this.packetsToSend = this.numberOfPacketsToSend(bashTime);
        initialiseVariables(ip);
        this.initializeFile();
        this.initializeSchedule();
    }

    void initialiseVariables(String ip) {
        this.id = 0;
        this.droppedPacketsSet = new HashSet<>();
        this.thetaFrequency = new TreeMap<>();

        this.localCLienTime = Calendar.getInstance();
        localCLienTime.setTimeInMillis(Instant.now().toEpochMilli());

        this.smoothTheta = new ArrayList<>();
        this.averageTheta = 0;
        this.averageRoundTrip = 0;
        this.clockClientSocket = new ClockClientSocket(ip);
        this.packetReceivedSet = Collections.synchronizedSortedSet(new TreeSet<>());
    }

    void initializeFile() {
        try {
            fileWriter = new PrintWriter("log.csv", "UTF-8");
            fileWriter.write(",,,,,,Time Analysis over "
                    + packetsToSend + " packets");
            fileWriter.write("\npacket_ID,rtt,theta," +
                    "smooth theta, HW time, Corrected time " +
                    ",HW time(in mmddyy format),Corrected time(in mmddyy format)");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void initializeSchedule() {
        System.out.println("base thread --> " + Thread.currentThread().getId());
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.future = scheduler.scheduleAtFixedRate(this::run, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");

        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        String serverIP;
        String timeGiven;
        ClockClient clockClient;
        if (args.length == 0) {
            System.out.println("Kindly enter the IP for sync server");
            serverIP = scanner.nextLine();
            clockClient = new ClockClient(serverIP);
        } else {
            serverIP = args[0];
            timeGiven = args[1];
            clockClient = new ClockClient(serverIP, timeGiven);
        }

        System.out.println("The current system time  = " + clockClient.localCLienTime.getTime());
        System.out.println("\n\nThe number of packets to be sent is " + clockClient.packetsToSend);
        clockClient.scheduler.schedule(() -> clockClient.manageClientShutdown(), clockClient.packetsToSend * 10, TimeUnit.SECONDS);

        try {
            clockClient.future.get();
        } catch (ExecutionException e) {
            System.out.println("Schedule thread failed because " + e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException | CancellationException ignored) {

        }
    }


    void run() {
        id++;
        int id = this.id;
        String now = Long.toString(Instant.now().toEpochMilli());
        String messageToServer = id + " " + now;
        try {
            this.clockClientSocket.sendToServer(messageToServer);
            String messageFromServer = this.clockClientSocket.receiveFromServer();
            packetReceivedSet.add(id);
//            messageFromServer += " " + this.localCLienTime.getTimeInMillis();
            messageFromServer += " " + Instant.now().toEpochMilli();
            System.out.println(messageFromServer + " Process Thread -->" + Thread.currentThread().getId());
            processTime(messageFromServer, id, packetReceivedSet.size());
        } catch (IOException e) {
            droppedPacketsSet.add(id);
            System.out.println("The packet number " + id + " failed because " + e.getMessage() + " inside thread " + Thread.currentThread().getId());
            System.out.println("\n");
        }
    }

    int numberOfPacketsToSend() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the amount of time (in hours) you need to synchronize for ");
        System.out.println("Common Examples\n\t0.0166667 for 1 minutes\n\t0.16667 for 10 minutes\n\t0.5 for 30 minutes");
        System.out.print("Time --> ");
        timeInHours = scanner.nextFloat();
        return (int) Math.rint((timeInHours * 60 * 60) / 10);
    }

    int numberOfPacketsToSend(String bashTime) {
        try {
            timeInHours = Float.parseFloat(bashTime);
            return (int) Math.rint((timeInHours * 60 * 60) / 10);
        } catch (NumberFormatException e) {
            System.out.println("The time provided " + bashTime + " is of incorrect format.");
            throw e;
        }

    }

    void processTime(String messages, int id, int size) {
        String time[] = messages.split(" ", 0);
        StringBuilder printString = new StringBuilder();
        if (packetReceivedSet.last() <= id) {
            long t0 = Long.parseLong(time[4]);
            long t1 = Long.parseLong(time[3]);
            long t2 = Long.parseLong(time[2]);
            long t3 = Long.parseLong(time[1]);

            long rtt = (t2 - t3) + (t0 - t1);
            double theta = (double) (((t2 - t3) - (t0 - t1)) / 2);

            double histTheta = DoubleRounder.round(theta, 5);

            if (thetaFrequency.containsKey(histTheta)) {
                thetaFrequency.put(histTheta, (thetaFrequency.get(histTheta) + 1));
            } else {
                thetaFrequency.put(histTheta, 1);
            }

            if (smoothTheta.size() == 8) {
                smoothTheta.remove(0);
            }
            smoothTheta.add(new AbstractMap.SimpleEntry<>(rtt, theta));

            long low = Integer.MAX_VALUE;
            Double newTheta = -1.0;
            for (Map.Entry<Long, Double> entry : smoothTheta) {
                if (entry.getKey() <= low) {
                    low = entry.getKey();
                    newTheta = entry.getValue();
                }
            }

            long newTime = (long) (t0 + newTheta);
            this.localCLienTime.setTimeInMillis(newTime);

            Calendar hwTime = Calendar.getInstance();
            hwTime.setTimeInMillis(t3);


            printString.append("\n" + id +
                    "," + rtt + "," + theta + "," + newTheta + "," + t3
                    + "," + newTime);
            printString.append("," + hwTime.getTime() + "," + this.localCLienTime.getTime());
            averageTheta = ((averageTheta * (size - 1)) + theta) / size;
            averageRoundTrip = (averageRoundTrip * (size - 1) + rtt) / size;

        } else {
            droppedPacketsSet.add(id);
            System.out.println("Packet " + id + " dropped as it came late ");
        }

        try {
            this.fileWriter.write(printString.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void manageClientShutdown() {

        try {
            if (droppedPacketsSet.size() > 0) {
                this.fileWriter.write("\n\nDropped Packets List");
                for (Integer packetId : this.droppedPacketsSet) {
                    this.fileWriter.write("\n" + packetId + ",");
                }
            }
            this.fileWriter.close();

            fileWriter = new PrintWriter("results.txt", "UTF-8");
            int packetsProcessed = this.packetsToSend - this.droppedPacketsSet.size();
            float packetsProcessedPercentage = (float) (this.droppedPacketsSet.size() * 100 / this.packetsToSend);
            StringBuilder reportMessage = new StringBuilder();
            reportMessage.append("---------- Report and Stats -------------\n");
            reportMessage.append("\n1. Hours of operations....").append(this.timeInHours).append(" (").append(this.timeInHours * 60).append(" minutes)");
            reportMessage.append("\n2. Number of Packets Processed ").append(packetsProcessed);
            reportMessage.append("\n3. Number of dropped Packets ").append(this.droppedPacketsSet.size());
            reportMessage.append("\n4. Percentage of dropped Packets ").append(packetsProcessedPercentage);
            reportMessage.append("\n5. Average rtt ").append(this.averageRoundTrip);
            reportMessage.append("\n6. Average theta (Drift)").append(this.averageTheta).append(" MilliSeconds per Second");
            reportMessage.append("\n7. The Histogram --> {");
            thetaFrequency.forEach((k, v) -> reportMessage.append("\n\t").append(k).append(" -->").append(v));
            reportMessage.append("\n}");
            this.fileWriter.write(reportMessage.toString());
            this.fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.future.cancel(true);
        this.scheduler.shutdown();

        System.out.println("\n The program will now terminate");
    }
}