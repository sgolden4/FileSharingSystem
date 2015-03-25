SWE 622 File Server:

To run the server:
From the command line, in the directory containing Server.jar, type:
java -jar Server.jar <server_address> <server_address> ...

When the server starts up, it starts checking the predetermined ports for other 
servers of its type.  It checks each server given at the command line, as well
as the local host (127.0.0.1).  As soon as the server finds an open port in the
range, it starts up a server socket on that port, and then continues checking 
the other ports for servers.  No two servers will take the same port.  
The server logs its status to the console, and when it has checked all
the ports, it will log that it is ready and start listening for connections.

Note: the client was designed assuming the servers would all be on the local
machine.  If a server is set up on a different machine, the client won't
find it, but files sent from the client to a server on the local machine
will get distributed to the other servers regardless of which machine
they are on.

The server accepts the following commands from the client:

To verify correct server send:
"verify"

To get a listing of files available on this server:
"listfiles"

To download:
 
"dl <filename>"
"dl <filename> resume <byteposition>"

To upload

"ul <filename>"
"ul <filename> resume"

If server receives "verify" it responds with "42".  If it receives "listfiles"
it responds with each file as a separate line. If it receives "dl" it 
responds with "sending" and "<file_length>", and if it receives "ul" it
responds with "<byte_position>" for the client to resume the upload at if
resuming, and then either way "ready" so the client knows it can start
sending data.

The server only allows filenames with alphanumeric characters and non-
consecutive periods, to prevent access to improper locations.

When the server finishes receiving a file upload, it will start uploading
the file to the other servers it's connected to.  To prevent an infinite 
upload loop, the server will send the command "ulserve" to the other servers 
when uploading, so they know it's coming from another server and not to upload
when finished receiving.

Since the distributed part of this system was underspecified in the assignment,
there are many useful features we did not implement in the server.  E.g.:
- Checking file modification datestamps for updating files.
- Uploading existing files to a server that just connected.
- Requiring some sort of authentication/authorization.
