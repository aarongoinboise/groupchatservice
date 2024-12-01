##	Names of IDs of all team members

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

##	A section on how you tested it

##	A section on observations/reflection on your development process and the roles of each team member
 