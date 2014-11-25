package commands

import akka.actor.Actor

case class UnparsedCommand(command: String)

class CommandParser extends Actor {
  def receive = {
    case c: UnparsedCommand => {
      val words = c.command.split(" ")
      sender ! HandleCommand(words.toList, sender)
    }
  }
}