package users

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Actor.Receive
import akka.pattern.ask
import akka.util.Timeout
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import akka.actor.Props
import commands.CommandParser
import commands.UserCommandHandler
import scala.util.Try
import scala.util.Failure
import commands.Command
import commands.Commandable
import commands.UnparsedCommand
import akka.actor.PoisonPill
import commands.GetCommandSet
import server.MudServer
import server.UserLogon
import server.UserLogoff
import server.IsOnline
import scala.concurrent.Await
import scala.concurrent.duration._

case class ClientConnection(port: Socket)
case class UserMessage(message: String)

class User extends Actor with Commandable {
  private var userInput: BufferedReader = null
  private var userOutput: PrintWriter = null
  private var loggedIn = true
  private val commandParser = context.actorOf(Props(new CommandParser), "commandParser")
  private val userCommandHandler = context.actorOf(Props(new UserCommandHandler), "userCommandHandler")

  implicit def inputStreamWrapper(input: InputStream) = new BufferedReader(new InputStreamReader(input))
  implicit def outputStreamWrapper(output: OutputStream) = new PrintWriter(new OutputStreamWriter(output))

  def receive = handleUserMessages.orElse(handleCommandMessages)

  def handleUserMessages: Receive = {
    case c: ClientConnection => {
      context.actorOf(Props(new Actor {
        def receive = {
          case ClientConnection(port) => {
            createConnection(c.port.getInputStream, c.port.getOutputStream)
          }
        }
      }), "clientConnectionDaemon") ! c
    }
    case m: UserMessage => {
      userOutput.println(m.message)
      userOutput.flush
    }
  }

  private def createConnection(input: BufferedReader, output: PrintWriter) {
    userInput = input
    userOutput = output

    userOutput.println("Wassup fresh, welcome to Morgantown. By what name can I refer to my new homie?")
    userOutput.flush
    
    implicit val timeout = Timeout(10 seconds)
    var usernameTaken = true
    var username = ""
    while (usernameTaken) {
      username = userInput.readLine
      val isOnline = MudServer.server ? IsOnline(username)
      val result = Await.result(isOnline, timeout.duration).asInstanceOf[Boolean]
      if (result) {
        userOutput.println("Sorry beef, I already know a " + username + ". You got a nickname or something I can call you?")
        userOutput.flush
      } else {
        MudServer.server ! UserLogon(username, self)
        usernameTaken = false
      }
    }
    println("User created for: " + username)
    userOutput.println("Nice to meet you " + username + ". Grab yourself a beer, meet some new people, or bust some heads. " +
      "Whatever gets you there.")
    userOutput.flush

    userCommandHandler ! GetCommandSet
    
    while (loggedIn) {
      val command = userInput.readLine
      command match {
        case a: Any => {
          commandParser ! UnparsedCommand(command)
          userOutput.println(username + " => " + command)
          userOutput.flush
          if (command.trim().take(4).equalsIgnoreCase("quit")) {
            loggedIn = false
            MudServer.server ! UserLogoff(username)
          }
        }
      }
    }
  }
}