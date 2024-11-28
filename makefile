build:
	javac *2.java

ChatServer2.class: ChatServer2.java
	javac ChatServer2.java

server: ChatServer2.class
	java ChatServer2 -p 5111 -d 1

ChatClient2.class: ChatClient2.java
	javac ChatClient2.java

client: ChatClient2.class
	java ChatClient2

clean:
	rm *.class