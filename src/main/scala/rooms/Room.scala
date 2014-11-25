package rooms

import akka.actor._

object Room {
	case class SetExits(exits: Map[String,(String,ActorRef)])
	def props(id: Int, name: String, desc: String) = Props(classOf[Room], id, name, desc)
}

class Room(val id: Int, val name: String, val desc: String) extends Actor{
	import Room._
	var exits: Map[String,(String,ActorRef)] = Map()
	def receive = {
		case SetExits(exits: Map[String,(String,ActorRef)]) => this.exits = exits
		case "Get Exits" => sender ! exits
	}
}