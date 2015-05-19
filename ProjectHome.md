Allows you to estabilish a TCP like stream connection using the UDP protocol.

It is reliable and data are coming in order. You can user standard InputStream and OutputStream to read and write the "socket".

This library does not starts a new thread for every connection on the server side, so it can be used to handle thousands of clients.

Because the project is under active development i don't have any precise number for the allowed client count, but the target is over 100 000. But the number of the allowed clients is depends on the amount of data they send.

This library is good if you have lots of clients with relatively small amount of data per client (like chat server and client).