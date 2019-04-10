Simple Clock Synchronization 

Part 1: Source Tree.
    .the structure of the zip file provided is as follows 
        ./    
            ./simple-clock-synchronization
                ./src/
                ./pom.xml
            ./runTimeLogs/
            ./output/

    All the relevant source is under simple-clock-synchronization

Part 2: Bulding the source
    . Move to dir simple-clock-synchronization
    . Make a mvn package
    . As a result of this, 2 jar will be created... 
        . simple-clock-synchronization-1.0-SNAPSHOT-client.jar
        . simple-clock-synchronization-1.0-SNAPSHOT-server.jar
    . From the project root (./simple-clock-synchronization), run the scripts provided to run the program 
        . runClient.sh --> Run the client in the forground 
        . runClientBackground --> Run the client in the backgorund and stdout and stderr is piped toclientTime.log. Optional server ip as command line argument for the script
        . runServer.sh --> Run the server in foreground
        . runServerBackground.sh --> Run the Server in background and stdout and stderr is piped to serverTime.log

part 3: Navigating the Program
    

    Menu options for Server... 
        . The server, Upon starting only provides 2 menu options
            . Press e to exit the server at any time
            . Press s to display number of packets processed till now
    Menu options for Client 
        . None. Client doenst accept any commands. 
    
    . If client is booted at foreground, it will ask for the following information 
        . Server IP 
        . Time to Run. It accepts floating point input so that we can give time less than an hour. 