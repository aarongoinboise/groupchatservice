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

##	A section on observations/reflection on your development process and the roles of each team member

##  Sources

[How to randomize a list](https://www.geeksforgeeks.org/shuffle-or-randomize-a-list-in-java/#)