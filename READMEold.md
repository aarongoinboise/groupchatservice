# Project #1: Chat Server with Sockets

* Authors: Aaron Goin and Sean Varie
* Class: CS455 Distributed Systems Section #001 Spring 2024

## Overview

This program provides a basic protocol for communicating through a chat server. Chat clients can obtain information from the chat server, join channels, and send instant messages.

## Manifest

Channel.java: Holds information for a channel and sends messages to clients.
ChatClient.java: A client that represents a user who would connect to the server.
ChatMessage.java: Represents messages sent between clients and servers.
ChatServer.java: Runs the server and checks command line arguments.
ClientConnection.java: Contains in/out streams and information about clients.
ColorChatClient.java: A ChatClient, but color output on the terminal.
makefile: Provides shortcuts to use on the terminal for running programs.
README.md: This document, which explains the project.
Server.java: A server that holds information and controls how clients chat.

## Building the project

Open a command-line shell, and make sure that you have Java installed (Version 11 and up).
You can do this by running the following command:
```
$ java --version
```
If you see output, you are good to go. If not, find installation instructions
[here](https://www.java.com/en/download/).\
Once you have Java, you can use the commands from the makefile. Run these series of commands:
```
$ make
```
You are now ready to use this program.

## Features and usage

After building the project, you can quickly get started by running this command in one shell:
```
$ make server
```
This will compile the code with these arguments: -p 5111 -d 0. On another shell, run this command:
```
$ make client
```
On the client side, you can run some simple commands (displayed on the console). To connect to
the server, enter the following:
```
$ /connect localhost 5111
```
The second argument is the server name (aka the hostname) and the third argument is the port number.
These will be the Socket arguments, and there needs to be two of them. It's recommended to try
this specific command before experimenting. 

To join a channel, enter the following:
```
$ /join channelName
```
A feature of our program is that the server starts with 0 channels. When a channel name is entered
that doesn't exist, a channel gets created and the client who entered that name gets put in the
channel. Everyone in the channel will be able to see all of the messages. When the channel reaches
0 clients, a timer begins, and all channels with 0 clients after three minutes will get removed from
the server.

Additionally, a server who has 0 clients after three minutes will get shutdown.

## Testing

Throughout developement, we performed tests where applicable, it was often difficult as many parts 
of the project were dependent on others to get any meaningful output. Regardless, we still tested 
partially implemented features when we could. 

The very first smoke test was always ensuring our files compiled. After that, any driver class would 
be run and any other class would have an object of its' type instantiated to ensure there would not 
be an immediate exception thrown.

Unit tests were less universal. As mostly standalone features were implemented, such as help messages, 
they were tested on their own by simply running the appropriate driver and engaging the feature (i.e. 
running the /help command). Features which are more dependent on other features and functionality for 
meaningful output were not unit tested in the same level of isolation. The most useful testing of 
these features, such as joining a channel, occurred after the system as a whole and other features 
were developed to a point of minimal functionality to allow for output from the feature being tested 
that would legitimately show us it's functionality.

System level tests were performed by simulating multiple users connecting to the server and using it 
as would be expected by normal chat users. System level tests of potential edge cases or points of 
conflict were also tested, such as multiple users attempting to obtain the same nickname.

## Link to Video
```
https://drive.google.com/file/d/1taiuapICF4KWy0QbfcnAyCh2bSd-qOAh/view?usp=sharing
```

### Known Bugs

None known at the present moment.

## Reflection

The biggest issues we had came from designing the protocol. We both changed our minds on it quite
a bit, and it was hard to decide on something. We would implement an idea, and either think of a 
better one, or see that it was a'stupid. However, we managed to come up with something that combined
simplicity and sophistication. A lot of this was learned through trial and error, and fixing the orders
of things in the protocol (when to write and read stuff, who takes care of what). Meanwhile, learning
about colored text in the console, reviewing makefile stuff, and discovering shutdown hooks were also
a part of this process.

In terms of problems and errors, the biggest ones were exceptions that appeared, but didn't change
needed values for the client or server. Since boolean values and other objects were used to check
the state of clients and servers, it was important that they were being set and reset to enter the
right blocks of code. Another error was the client not switching back to its menu after quitting
the server connection. This was due to improper order in the blocks of code. The surprising thing
was that most errors had to do with ordering in the code itself. Early attempts at solving these
problems were done by synchronizing blocks. However, changing the order of setting booleans did the
trick, along with double checking synchronized methods that already existed. Most of these errors
occurred during the cancellation of a server or a client.

The protocol itself was the most challenging part. I think once we settled on something, it was 
easier to see how to implement it. Once things started to work, the best protocol was clicking
for us. This will be useful in future projects, as less time will be spent on figuring out the
best protocol. Additionally, it goes back to the finite state machine example in class. Part of the 
problem was not designing ALL of the steps that were required. When unexpected things happened, it
was because there was nothing accounting for them (example: server output when a client ctrl-c).

Once all these states were figured out, we connected them with the directions to the next states.

## Sources used

[Color console](https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println)\
[Makefile resource](https://stackoverflow.com/questions/32127524/how-to-install-and-use-make-in-windows)\
[Oracle docs](https://docs.oracle.com/javase/8/docs/api/)\
[Shutdown hooks](https://www.geeksforgeeks.org/jvm-shutdown-hook-java/)\
[Synchronization 1](https://www.geeksforgeeks.org/synchronization-in-java/)\
[Synchronization 2](https://www.baeldung.com/java-synchronized)
[Timestamp](https://stackoverflow.com/questions/8345023/need-to-get-current-timestamp-in-java)