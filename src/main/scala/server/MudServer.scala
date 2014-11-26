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

class MudServer extends Actor {
  def receive = {
    case StartServer => startServer
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