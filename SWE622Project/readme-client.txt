File Client:

To run the client:
From the command line, once in the directory where FileClient.class resides type:
java FileClient

When the client starts, it will display a welcome message followed by a list of commands
and descriptions of their function. This list can be accessed at any point with the 'help'
command. Commands can be entered into a pseudo command-line prompt where lines start with
"> ". The available commands are: dl, ul, rdl, rul, listserv, help, and exit. For 
this project, we assume all servers are known by the client from startup. The listserv
command checks each port from the list of known servers to see if it can establish
a connection, and displays information accordingly. All files are uploaded and downloaded
from the same folder, which can be changed 

Downloading:
The dl and rdl commands both consist of three arguments: one for the command itself,
the second for the filename, and the third for the name of the server to attempt the
download. If the server name is not known or cannot be connected, a corresponding
message is printed and the download is aborted. After sending the request to the server,
the client waits for the message "sending" followed by the filesize, and then the file
itself. If a message other than "sending" is received, this message is displayed as an
error and then the download is aborted. If any error is thrown, it will be caught and 
the download will be added to the ResumeDLList. This allows the ability to redownload files
in the event of a server crash. If the client goes down, the resume download (rdl) command 
will look on the filesystem to see if that file exists, and how many bytes it has received. 

Uploading:
The ul and rul commands consist of three and two arguments respectively: both start with
the command itself, followed by the filename, while a standard ul command has a third
argument for server name. Resuming uploads is always done to the server where the
original upload started (if known). If this is unknown (because the client went offline),
the client will be prompted with a query regarding if they would like to continue, and 
if so, which server they would like to attempt the upload. The client waits for the message
"ready" or the partial file size for resuming uploads and then begins to send the file. Like 
for downloads, if there is a failure in sending, the upload is added to the ResumeULList to 
allow for a faster response to rul commands (as long as the client has not terminated). If the
client chooses to reupload to a server that does not have a partial file, the rul command 
functions like a normal ul command to that server.

Verifying rdl and rul:
The easiest way to verify the validity of these functions is to attempt to upload or download
a large file (2 GB should be plenty) and then interupt the process by terminating either the 
client, the server, or both. In any case, the client and server should be able to resume without
any hiccups, and extra data will not be passed if it is not necessary. For rul, this is obvious
from the statements that print the bytes received, which will start at a value much greater than 
zero if the file was partially uploaded previously. The rdl command uses the same functionality,
so to avoid a large number of messages on the client side, the print statements are omitted.

Refer to the readme-server file for more documentation, including a few improvements that could
be made that were not included in the specification given for this project. 