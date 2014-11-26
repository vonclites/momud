package server

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Inbox
import java.net.ServerSocket
import akka.actor.actorRef2Scala
import scala.concurrent.duration._
import users._
import world._
import rooms._

case class StartServer()
case class UserLogon(username: String, user: ActorRef)
case class UserLogoff(username: String)
case class IsOnline(username: String)

class MudServer extends Actor {
  private var onlineUsers: Map[String, ActorRef] = Map[String, ActorRef]()
  
  def receive = {
    case StartServer => {
      context.actorOf(Props(new Actor {
        def receive = {
          case StartServer => {
            startServer
          }
        }
      }), "serverDaemon") ! StartServer
    }
    case UserLogon(username, user) => onlineUsers = onlineUsers + ((username, user))
    case UserLogoff(username) => onlineUsers = onlineUsers - username
    case IsOnline(username) => {
      if (onlineUsers.contains(username)) sender ! true else sender ! false
    }
  }

  private def startServer = {
    val serverPort = new ServerSocket(8080)

    while (true) {
      println("Listening for connections...")
      val clientConnection = serverPort.accept
      val user = context.actorOf(Props(new User))
      println("Connection made, creating user...")
      user ! ClientConnection(clientConnection)
    }
  }
}

object MudServer extends App {
  val system = ActorSystem("MorgantownMUD")
  val server = system.actorOf(Props(classOf[MudServer]), "server")
  val gaia = system.actorOf(Props(classOf[Gaia]), "gaia")
  println("Server starting...")
  server ! StartServer
  println("Building world...")  
  val inbox = Inbox.create(system)
  inbox.send(gaia, Gaia.BuildWorld)
}