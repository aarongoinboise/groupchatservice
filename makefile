build:
	javac *.java

ChatServer.class: ChatServer.java
	javac ChatServer.java

server: ChatServer.class
	java ChatServer -p 5111 -d 0

ChatClient.class: ChatClient.java
	javac ChatClient.java

client: ChatClient.class
	java ChatClient

clean:
	rm *.class