# P2Pchat
Based on TCP

# How to work?
Compile and run Server.java first, then compile Client.java and execute Client.java like below:
java Client xxx,xxx,x,xx(Host IP address) 29093(Host port number)

For example: java Client 192.168.0.14 29093
Use “JOIN” command to join the list. Use “LIST” command to get the list of online users.
Find the IP address and port number of the Client which you wanted to connect to.

Type the command below to connect to target:

CHAT_RQ:target IP address,target port number
For example: if you want to connect to a Client with IP:192.168.0.14, port number:58281:
CHAT_RQ:192.168.0.14,58281

After the connection has been setup, you can chat with the connected client by type in:
CHAT_TK:some words.
For example, you want to say “hello”: CHAT_TK:Hello

If you want to quit the chat, you can type in:
CHAT_QUIT
Then the p2p connection will be disconnected

