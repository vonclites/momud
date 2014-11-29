package rooms

import akka.actor._
import users.Welcome
import users.UserMessage
import server.UserLogoff

object Room {
	case class SetExits(exits: Map[String,(String,ActorRef)])
	case class Arrive(name: String, ref: ActorRef)
	case class Depart(name: String, dir: String)
	case class Say(name: String, msg: String)
	case class Look()
	case class GetUsers()
	def props(id: Int, name: String, desc: String) = Props(classOf[Room], id, name, desc)
}

class Room(val id: Int, val name: String, val desc: String) extends Actor{
	import Room._
	var exits: Map[String,(String,ActorRef)] = Map()
	var users: Map[String,ActorRef] = Map()
	def receive = {
		case SetExits(exits) => this.exits = exits
		case Arrive(name, ref) => {
			ref ! Welcome(this toString)
			users foreach { case (_, user) => user ! UserMessage(name + " has arrived.")}
			users = users + ((name,ref))
		}
		case Depart(name, dir) => {
			if (exits.isDefinedAt(dir)){
			users = users - name
			exits(dir)._2 ! Arrive(name, sender)
			users foreach { case (_, user) => user ! UserMessage(name + " has departed " + getFullDirection(dir) + ".")}
			} else sender ! UserMessage("You cannot go that direction.")
		} 
		case Say(name, msg) => (users - name) foreach { case (_, user) => user ! UserMessage(name + " says, '" + msg + "'")}
		case Look => sender ! UserMessage(this toString)
		case UserLogoff(name) => {
			users = users - name
			users foreach { case (_, user) => user ! UserMessage(name + " has logged out.")}
		}
		case GetUsers => sender ! users
	}
	
	def getFullDirection(dir: String): String = dir match {
		case "n" => "north"
		case "s" => "south"
		case "e" => "east"
		case "w" => "west"
	}
	override def toString = {
		name + "\n" +
		desc + "\n" +
		//(exits.keys zip (exits.values map {case (name,ref) => name})) mkString("\n") +
		((users.keys.toList) map (name => (name + " is here."))).mkString("\n")
	}
}