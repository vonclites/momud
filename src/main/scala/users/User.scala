package users

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Random
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import commands.CommandParser
import commands.CommandRecipient
import commands.GetCommandSet
import commands.UnparsedCommand
import commands.UserCommandHandler
import server.MudServer
import server.UserLogoff
import server.UserLogon
import world.Gaia
import rooms._
import akka.actor.PoisonPill

case class ClientConnection(port: Socket)
case class WorldIntroduction(world: ActorRef)
case class Welcome(desc: String)
case class UserMessage(message: String)
case class UserDeath(name: String)
case class Move(dir: String)
case class Speak(msg: String)
case class Hit(target: String)
case class GetHit(attacker: String, damage: Int)
case class Die()
case class GetRoomCommands(handler: ActorRef)


class User extends Actor with CommandRecipient {
  private var userInput: BufferedReader = null
  private var userOutput: PrintWriter = null
  private var connection: Socket = null
  private var username = ""
  private var origin: users.Origin.Origin = null
  private var room: ActorRef = null
  private var world: ActorRef = null
  private var loggedIn = true
  private var hps = 100
  private var bac = 1.0
  private var lastCommand: String = ""
  private val commandParser = context.actorOf(Props(new CommandParser), "commandParser")
  private val userCommandHandler = context.actorOf(Props(new UserCommandHandler), "userCommandHandler")

  implicit def inputStreamWrapper(input: InputStream) = new BufferedReader(new InputStreamReader(input))
  implicit def outputStreamWrapper(output: OutputStream) = new PrintWriter(new OutputStreamWriter(output))

  def receive = handleUserMessages.orElse(handleCommandMessages)

  def handleUserMessages: Receive = {
    case ClientConnection(port) => {
    	connection = port
      context.actorOf(Props(new Actor {
        def receive = {
          case ClientConnection(portConnection) => {
            createConnection(portConnection.getInputStream, portConnection.getOutputStream)
            portConnection.close
          }
        }
      }), "clientConnectionDaemon") ! ClientConnection(port)
    }
    case WorldIntroduction(gaia) => world = gaia
    
    case Move(dir) => room ! Room.Depart(username, dir, bac)
    case Speak(msg) => room ! Room.Say(username, msg)
    case Room.Look => room ! Room.Look
    case Hit(target) => room ! Room.Hit(username, target)
    case GetHit(attacker, damage) => {
    	self ! UserMessage(attacker + " hits you for " + damage + " damage.")
    	if (hps - damage < 1) {
    		self ! UserMessage("You are dead.")
    		self ! Die
    	} else {
    		hps = hps - damage
    	}    	
    }
    case Die => {
  		room ! UserDeath(username)
  		MudServer.server ! UserLogoff(username)
    	self ! PoisonPill
    	connection.close
    }
    case Welcome(desc:String) => room = sender; self ! UserMessage(desc)
    case GetRoomCommands(handler) => handler ! GetCommandSet
    case UserMessage(message) => {
      userOutput.println(message)
      userOutput.flush
    }
  }

  private def createConnection(input: BufferedReader, output: PrintWriter) {
    userInput = input
    userOutput = output

    self ! UserMessage("\nWassup fresh, welcome to Morgantown. By what name can I refer to my new homie?")
    
    createPlayerCharacter
    
    self ! UserMessage("Well, it's been a pleasure, " + username + ". Grab yourself a beer, meet some new people, or bust some heads. " +
      "Whatever you're feeling.\n")

    userCommandHandler ! GetCommandSet
    world ! Gaia.ReceiveUser(username, new Player(username, origin, self))
    
    while (loggedIn) {
      var command = getUserInput
      command match {
        case a: Any => {
        	command = if (command.trim == "!") lastCommand else command
          commandParser ! UnparsedCommand(command)
          lastCommand = command
          if (command.trim().take(4).equalsIgnoreCase("quit")) {
            loggedIn = false       
            userOutput.println("Logging out...")
            userOutput.flush
            room ! UserLogoff(username)
            MudServer.server ! UserLogoff(username)
          }
        }
      }
    }
  }
  
  private def createPlayerCharacter() = {
    implicit val timeout = Timeout(10 seconds)
    var usernameTaken = true
    while (usernameTaken) {
      username = getUserInput
      val canLogon = MudServer.server ? UserLogon(username, self)
      val loginSuccessful = Await.result(canLogon, timeout.duration).asInstanceOf[Boolean]
      if (loginSuccessful) {
        usernameTaken = false
      } else {
        self ! UserMessage("Sorry beef, I already know a " + username + ". You got a nickname or something I can call you?")
      }
    }
    println("User created for: " + username)
    var noOriginDetermined = true
    self ! UserMessage("Nice to meet you. Outta curiosity, " + username + ", where are you from, the Mountain State, WV, or the Garden State, NJ?")
    while (noOriginDetermined) {
      var proposedOrigin = getUserInput
      origin = Origin.determineOrigin(proposedOrigin)
      if (origin == null) {
        self ! UserMessage("Haha, I don't believe you. You're definitely from West Virginia or Jersey, which is it?")
      } else if (origin == Origin.WV) {
        self ! UserMessage("Ha! I knew it! Always nice to meet a fellow West Virginian.")
        noOriginDetermined = false
      } else {
        self ! UserMessage("I figured as much. I could tell by the way you walked up like you owned the place.")
        noOriginDetermined = false
      }
    }
  }
  
  private def getUserInput: String = {
  	userOutput.print(username + " => ")
    userOutput.flush
    val buffer = userInput.readLine
    userOutput.println
    userOutput.flush
    buffer
  }
}