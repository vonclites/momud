package commands

import akka.actor.Actor
import akka.actor.ActorRef

class BarCommandHandler extends Actor {

  def receive = handleBarCommands

  def handleBarCommands: Receive = {
    case c: Command if c.command(0).equalsIgnoreCase("buy drink") => null
    case GetCommandSet => assignCommandsToSender(sender)
  }
  
  private def assignCommandsToSender(requestingUser: ActorRef) = {
    requestingUser ! NewCommandSet(Set("buy drink"), self)
  }
}