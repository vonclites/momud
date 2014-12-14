momud
=====
Welcome to the MorgantownMUD! This is our class project for Functional Programming and Concurrency!

Instructions for playing are as follows:
(Maybe one day we will have a nice package, sorry)
1. Skip to step #4 if you already have or know of a running instance of the game.
2. Download the project.
3. Initialize the DB
	3a. Import /db/data.sql into mysql database named 'mud' with root/root username/pw
4. In the root directory of the project do a "./activator run". This will start the game server and open a port listening for client connections.
5. Using any telnet or MUD client, connect to the host running the game using its IP address, or domain name if it has one, on port 8080. (This can be configured in the GameServer class if you run your own version of the game.)
6. Follow the prompts to create a character.
7. Play the game!
