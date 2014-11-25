package users

import akka.actor.Actor
import akka.actor.Actor.Receive
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

case class ClientConnection(port: Socket)
case class UserMessage(message: String)

class User extends Actor with Commandable {
  private var userInput: BufferedReader = null
  private var userOutput: PrintWriter = null
  private var loggedIn = true
  private val commandParser = context.actorOf(Props(new CommandParser), "commandparser")
  private val userCommandHandler = context.actorOf(Props(new UserCommandHandler), "usercommandhandler")

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
      }), "clientconnection") ! c
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
    var username = userInput.readLine
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
          if (command.trim().take(4).equalsIgnoreCase("quit")) loggedIn = false
        }
      }
    }
  }
}