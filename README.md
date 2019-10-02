# CloudServer
Java Cloud Server paired with [Java Android Cloud Client](https://github.com/Adrian-Gonzalez-Madruga/CloudClient) to act as a file server. This server authenticates a user via sql, may download or upload to/from the client archiving data in folders and pathing using json, and connects/disconnects multiple users at a time allowing for synchronous usage.

Overall this was a fun project pairing it with the [Java Android Cloud Client](https://github.com/Adrian-Gonzalez-Madruga/CloudClient) to implement sockets and file sharing. Learning more in depth of sockets opposed to simple messaging I self taught in highschool felt full circle giving me knowledge and experience to use sockets and file sending to create an eventual NAS on my TODO List in the future.

## Getting Started
The Cloud Server is ready to run with no setup after downloading repository. Running will let the program listen on the selected port for a connection from the client.

## Built With

* [SQLite](https://sqlite.org/index.html) - Credential Storage
* [JSON](https://www.json.org/) - File Managment

## Flaws, Fixes, and TODO
* Minor code redundancy - Reformatting and applying design pattern to improve efficiency
* Code to select file cannot be over 9 - Use Regex to cut code segments alowwing for unlimeted length
* No Folder Creation - TODO
* No File/Folder Deletion - TODO
* Add Encryption for Security - TODO
* Add User Limited Space Allocation - TODO

## Authors

* **[Adrian Gonzalez Madruga](https://github.com/Adrian-Gonzalez-Madruga)** - *Creation, Debugging, and Managment*
