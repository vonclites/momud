package rooms

import akka.actor._

object Room {
	case class SetExits(exits: Map[String,(String,ActorRef)])
	case class Arrive(name: String, ref: ActorRef)
	case class GetUsers()
	def props(id: Int, name: String, desc: String) = Props(classOf[Room], id, name, desc)
}

class Room(val id: Int, val name: String, val desc: String) extends Actor{
	import Room._
	var exits: Map[String,(String,ActorRef)] = Map()
	var users: Map[String,ActorRef] = Map()
	def receive = {
		case SetExits(exits) => this.exits = exits
		case Arrive(name, ref) => users = users + ((name,ref))
		case GetUsers => sender ! users
	}
}