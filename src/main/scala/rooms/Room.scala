package rooms

import akka.actor._
import scala.util.Random
import users.Welcome
import users.UserMessage
import users.GetHit
import server.UserLogoff
import users.Origin._
import users.UserDeath

object Room {
	case class SetExits(exits: Map[String,(String,ActorRef)])
	case class Arrive(name: String, player: Player)
	case class Depart(name: String, dir: String)
	case class Say(name: String, msg: String)
	case class Look()
	case class Hit(attacker: String, target: String)
	case class GetUsers()
	def props(id: Int, name: String, desc: String) = Props(classOf[Room], id, name, desc)
}

class Room(val id: Int, val name: String, val desc: String) extends Actor{
	import Room._
	var exits: Map[String,(String,ActorRef)] = Map()
	var users: Map[String,Player] = Map()
	def receive = {
		case SetExits(exits) => this.exits = exits
		case Arrive(name, player) => {
			player.ref ! Welcome(this toString)
			player.origin match{
				case WV => users foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " arrives with a steady gait.")}
				case NJ => users foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " arrives, trying to look like a badass.")} 
			}
			users = users + ((name,player))
		}
		case Depart(name, dir) => {
			if (exits.isDefinedAt(dir)){
				val player = users(name)	
				users = users - name
				exits(dir)._2 ! Arrive(name, player)
				users foreach { case (_, player) => player.ref ! UserMessage(name + " has departed " + getFullDirection(dir) + ".")}
			} else sender ! UserMessage("You cannot go that direction.")
		} 
		case Say(name, msg) => (users - name) foreach { case (_, player) => player.ref ! UserMessage(name + " says, '" + msg + "'")}
		case Look => sender ! UserMessage(this toString)
		case Hit(attackerName, targetName) => {
			val attacker = users(attackerName)
			users.find( { case (name,player) => name.toLowerCase.startsWith(targetName.toLowerCase) } ) match {
				case Some((name,player)) => player.ref ! GetHit(attackerName, Random.nextInt(20)); sender ! UserMessage("You hit " + name + ".")
				case None => sender ! UserMessage("That person does not seem to be here.")
			}
		}
		case UserDeath(name) => {
			users = users - name
			users foreach { case(_, player) => player.ref ! UserMessage(name + " has been killed.")}
		}
		case UserLogoff(name) => {
			users = users - name
			users foreach { case (_, player) => player.ref ! UserMessage(name + " has logged out.")}
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
		((exits.toList) map { case (dir,(name,_)) => getFullDirection(dir) + ": " + name}).mkString("\n") + "\n" +
		(((users.toList) map { case (name, player) => {
			player.origin  match {
				case WV => name + " is here, wearing a WVU shirt."
				case NJ => name + " is here, his baseball cap rotated sideways."
			}
		}}).mkString("\n"))
	}
}

class Player(val name: String, val origin: Origin, val ref: ActorRef)