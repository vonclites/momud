package commands

import akka.actor.Actor

class RegularRoomCommandHandler extends Actor {
  
  def receive = handleRoomCommands

  def handleRoomCommands: Receive = {
    case GetCommandSet => ;
  }
}