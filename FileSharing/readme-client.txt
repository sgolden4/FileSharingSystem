File Client:

To run the client:
From the command line, once in the directory where Client.jar resides type:
java -jar Client.jar <directory_path> <server 3210> <server 3211> <server 3212> ...

<directory_path> denotes the path to the download/upload directory. If no argument is given,
files are downloaded/uploaded to the current directory. If no servers are listed, it assumes
three servers running on the local machine (127.0.0.1). If you wish to specify servers,
you must also specify a directory.

When the client starts, it will display a welcome message followed by a list of commands
and descriptions of their function. This list can be accessed at any point with the 'help'
command. Commands can be entered into a pseudo command-line prompt where lines start with
"> ". The available commands are: dl, ul, listserv, listfiles, help, and exit. For 
this project, we assume all servers are known by the client from startup (either from
command-line arguments or the default). The listserv command checks each port from the 
list of known servers to see if it can establish a connection, and displays information 
accordingly. The listfiles command will list all the files stored at the specified server.

Downloading:

"dl <filename> <server_name>"

The dl command consists of three arguments: one for the command itself,
the second for the filename, and the third for the name of the server to attempt the
download. If the server name is not known or cannot be connected, a corresponding
message is printed and the download is aborted. The program then checks if the file
already exists on the client. After sending the request to the server (which includes
the number of bytes found on the client for that file), the client waits for the message
"sending" followed by the filesize, and then sends "ready" and waits for the file itself.
If a message other than "sending" is received, this message is displayed as an error and 
then the download is aborted.

Uploading:

"ul <filename> <server_name>"

The ul command consists of three arguments: it start with the command itself, followed 
by the filename, and finally the server name. After sending the request, the client waits 
for the partial file size (if one exists, otherwise it receives 0) and the message "ready"
and then begins to send the file. 

Verifying resuming:
The easiest way to verify the validity of this is to attempt to upload or download a large 
file (2 GB should be plenty) and then interupt the process by terminating either the client,
the server, or both. In any case, the client and server should be able to resume without
any hiccups, and extra data will not be passed if it is not necessary. Both downloads and 
uploads provide information on how many bytes were received/sent and it is trival to determine
the total size of the received file. In resuming, the file will finish with the full file size,
but will print that it sent/received fewer than the full file's bytes.

Refer to the readme-server file for more documentation, including a few improvements that could
be made that were not included in the specification given for this project. 