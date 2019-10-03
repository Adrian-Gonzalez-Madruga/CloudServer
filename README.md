# CloudServer
Java Cloud Server paired with [Java Android Cloud Client](https://github.com/Adrian-Gonzalez-Madruga/CloudClient) to act as a file server. This server authenticates a user via sql, may download or upload to/from the client archiving data in their own folders and pathing using json. Pathing to the JSON uses a special character string that only allows the user who signed in to get their selected folder increasing the security of the application. The Cloud Server also connects/disconnects multiple users at a time allowing for synchronous usage of the server.


## Getting Started
The Cloud Server is ready to run with no setup after downloading repository. Running will let the program listen on the selected port for a connection from the client.

## Built With

* [JSON](https://www.json.org/) - File Managment
* [SQLite](https://sqlite.org/index.html) - Credential Storage

## Flaws, Fixes, and TODO
* Minor code redundancy - Reformatting and applying design pattern to improve efficiency
* Code to select file cannot be over 9 - Use Regex to cut code segments alowwing for unlimited length
* No Folder Creation - TODO
* No File/Folder Deletion - TODO
* Add Encryption for Security - TODO
* Add User Limited Space Allocation - TODO

## Authors

* **[Adrian Gonzalez Madruga](https://github.com/Adrian-Gonzalez-Madruga)** - *Creation, Debugging, Managment and Full Implementation of Server*
