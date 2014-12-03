package commands

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import users.UserMessage
import users.Move
import users.Speak
import users.Hit
import rooms.Room.Look

case class Command(command: List[String], origin: ActorRef)
case class GetCommandSet()

class UserCommandHandler extends Actor {

  def receive = handleUserCommands

  def handleUserCommands: Receive = {
    case c: Command if c.command(0).equalsIgnoreCase("say") => c.origin ! UserMessage("You said: " + c.command.tail.mkString(" ")); c.origin ! Speak(c.command.tail.mkString(" "))
    case c: Command if c.command(0).equalsIgnoreCase("north") || c.command(0).equalsIgnoreCase("n") => c.origin ! UserMessage("You travel north."); c.origin ! Move("n")
    case c: Command if c.command(0).equalsIgnoreCase("south") || c.command(0).equalsIgnoreCase("s")  => c.origin ! UserMessage("You travel south."); c.origin ! Move("s")
    case c: Command if c.command(0).equalsIgnoreCase("east") || c.command(0).equalsIgnoreCase("e")  => c.origin ! UserMessage("You travel east."); c.origin ! Move("e")
    case c: Command if c.command(0).equalsIgnoreCase("west") || c.command(0).equalsIgnoreCase("w")  => c.origin ! UserMessage("You travel west."); c.origin ! Move("w")
    case c: Command if c.command(0).equalsIgnoreCase("look") => c.origin ! Look
    case c: Command if c.command(0).equalsIgnoreCase("hit") => c.origin ! Hit(c.command(1))
    case c: Command if c.command(0).equalsIgnoreCase("quit") => c.origin ! PoisonPill
    case c: Command if c.command(0).equalsIgnoreCase("help") => c.origin ! GetAvailableCommands(c.origin)
    case GetCommandSet => assignCommandsToSender(sender)
  }

  private def assignCommandsToSender(requestingUser: ActorRef) = {
    requestingUser ! NewCommandSet(Set("say"), self)
    requestingUser ! NewCommandSet(Set("north", "n"), self)
    requestingUser ! NewCommandSet(Set("south", "s"), self)
    requestingUser ! NewCommandSet(Set("east", "e"), self)
    requestingUser ! NewCommandSet(Set("west", "w"), self)
    requestingUser ! NewCommandSet(Set("look"), self)
    requestingUser ! NewCommandSet(Set("hit"), self)
    requestingUser ! NewCommandSet(Set("quit"), self)
    requestingUser ! NewCommandSet(Set("help"), self)
  }
}