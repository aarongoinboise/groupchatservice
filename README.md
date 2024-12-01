##	Names of IDs of all team members

Aaron Goin
Skarlet Williams
Cole Brusa

##	A web link to the demo video

##	A file/folder manifest to guide reading through the code

<pre>
|- groupchatservice (the top level, project folder)
    - ChatClient.java (Driver class for the client, with related client logic)
    - ChatServer.java (Driver class for the server)
    - ChatServerParser.java (Parses arguments for ChatServer, and stops program with usage statements as needed)
    - GetServed.java (Server logic, with a thread pool for clients to connect)
    - Reporter.java (Contains logic for the server's debug level and a TermColors object)
    - runclient.sh (A script that compiles needed files and runs a ChatClient)
    - runserver.sh (A script that compiles needed files and runs a ChatServer with hard-coded arguments)
    - StringObject.java (A serialized object for sending strings through streams)
    - TermsColors.java (Provides logic for colored printing)
</pre>

##	A section on building and running the server/clients

Make sure you have Java fully installed in your environment (Version 17 and up).
You can do this by running the following command:
```
$ java --version
```
If you don't have it, learn how to install it 
[here](https://www.java.com/en/download/).\
Once you have Java, you have a couple options. To easily get started, run the server script from the directory *groupchatservice*:
```
$ runserver.sh
```
Then, take note of the args in the script (-p 5005 -d 1) and run the client script:
```
$ runclient.sh
```
To connect to the previous server (if you are on the same computer it was run on), use the following /connect command: /connect localhost 5111.

Note: If you get a "bad interpreter" error, use this command:
```
$ dos2unix *.sh
```

You can also manually compile and run the server with different arguments if you wish:
```
$ javac ChatServer.java GetServed.java TermColors.java StringObject.java ChatServerParser.java Reporter.java
$ java ChatServer -p <port-num> -d <0 or 1>
```
Note: The port number is used to connect with clients. Debug level 0 means that errors only will be displayed on the server, and 1 means that all events will be displayed on the server.

##	A section on how you tested it

- General smoke tests: It started with trying to use all of basic commands with one client. Afterwards, we tried testing the same commands with multiple clients. Finally, we tested multiple channels, make sure that clients can connect to the channels in a multitude of ways (multiple clients in the same channel, separate channels, etc.).

- There were also other approaches we tested to ensure that a client can get the messages displayed in a channel they are in. One of those included a soTimeout on the respective sockets. Another included an additional thread for displaying channel messages whenever they came in. However, it was settled that the easiest way to handle this is another command (/refresh), which will allow the client to decide if they want to see the updated messages. The best part about this is that we didn't have to change the existing protocol.

- Debugging is tough with multi-threading. Therefore, there were strategic print statements we used to determine the cause of certain errors. This led us to catching exceptions in certain areas as well as using synchronization blocks. This helped prevent incorrect values and breakings of the protocol.

##	A section on observations/reflection on your development process and the roles of each team member

We used an all hands on deck approach. Our group made sure to work in stages, and contribute at each stage. The easiest way to define this is the following:
- Basic Client and Server with one read/write operation and a serialized object
- Basic Client and Server with "easy commands" (/list and /help)
- Basic Client and Server with all commands
- Advanced Client and Server with multithreading and one channel
- Advanced Client and Server with multithreading and multiple channels

This was sometimes fluid, as we had to go back to previous stages at times. However, it kept us on track, and let us not get ahead of ourselves. 

An important note is that Aaron did this project before in Distributed Systems last semester. The formatting on the previous project was influential in this process, especially deciding on a server heavy design. This meant the server keeps track of client and channel information, which allows for less steps in the protocol. Also, things regarding the usage of a fixed thread pool for clients, as well as a TimerTask for the 3 minute idle counting of the server were inspired by that assignment. However, there are many differences, such as a reporter for printing messages as well as the decision to write a blank message to the client if they weren't in a channel (instead of a series of messages they haven't read yet in the channel). You'll also notice that synchronization key words on objects and Object locks were used more often. This gave everyone in the group a chance to contribute and create a unique project.

##  Sources

[How to randomize a list](https://www.geeksforgeeks.org/shuffle-or-randomize-a-list-in-java/#)