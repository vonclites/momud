package commands

import akka.actor.ActorRef
import akka.actor.Actor.Receive
import scala.collection.mutable.HashMap
import users.UserMessage

case class NewCommandSet(commandSet: Set[String], commandHandler: ActorRef)
case class RemoveCommand(commandWord: String)
case class HandleCommand(command: List[String], origin: ActorRef)
case class GetAvailableCommands(origin: ActorRef)

trait Commandable {

  private val validCommands: HashMap[Set[String], ActorRef] = new HashMap[Set[String], ActorRef]

  def handleCommandMessages: Receive = {
    case NewCommandSet(commandSet, commandHandler) => {
      validCommands.put(commandSet, commandHandler)
    }
    case RemoveCommand(commandWord) => {
      for ((commandSet, commandHandler) <- validCommands) {
        if (commandSet.contains(commandWord)) validCommands.remove(commandSet)
      }
    }
    case HandleCommand(command, origin) => {
      route(command, origin)
    }
    case GetAvailableCommands(origin) => {
      origin ! UserMessage("Currently available commands: " + validCommands.keySet.toList.flatten.mkString(", "))
    }
  }

  private def route(command: List[String], origin: ActorRef) = {
    val commandHandlers = validCommands.filterKeys(commandSet => commandSet.contains(command(0).toLowerCase()))
    commandHandlers.foreach({ case (commandWord, commandHandler) => commandHandler ! Command(command, origin) })
  }
}