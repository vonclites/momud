package commands

import akka.actor.Actor
import akka.actor.ActorRef
import users._
import scala.concurrent.duration

trait RoomCommandHandler {
	def commands: Set[String]
}

case class GiveCommandSet(user: ActorRef)
case class RemoveCommandSet(user: ActorRef)
case class GiveDrink(user: ActorRef)

class BarCommandHandler(val room: ActorRef) extends Actor with RoomCommandHandler{
	def commands = Set("drink")
  def receive = handleBarCommands

  def handleBarCommands: Receive = {
    case c: Command if c.command(0).equalsIgnoreCase("drink") => room ! GiveDrink(c.origin); Thread.sleep(5000)
    case GiveCommandSet(user) => assignCommandsToSender(user)
    case RemoveCommandSet(user) => commands foreach (user ! RemoveCommand(_))
  }
  
  private def assignCommandsToSender(requestingUser: ActorRef) = {
    requestingUser ! NewCommandSet(Set("drink"), self)
  }
}

class RegularRoomCommandHandler extends Actor with RoomCommandHandler{
  def commands = Set()
  def receive = handleRoomCommands

  def handleRoomCommands: Receive = {
    case GetCommandSet => ;
  }
}