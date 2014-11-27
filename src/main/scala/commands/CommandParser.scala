package commands

import akka.actor.Actor

case class UnparsedCommand(command: String)

class CommandParser extends Actor {
  def receive = {
    case UnparsedCommand(command) => {
      val words = command.split(" ")
      sender ! HandleCommand(words.toList, sender)
    }
  }
}