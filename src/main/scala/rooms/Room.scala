package rooms

import akka.actor._
import scala.util.Random
import users._
import users.Origin._
import server.UserLogoff
import commands.RoomCommandHandler
import commands._

object Room {
	case class SetExits(exits: Map[String,(String,ActorRef)])
	case class Arrive(name: String, player: Player)
	case class Depart(name: String, dir: String, bac: Double)
	case class Say(name: String, msg: String)
	case class Look(name: String)
	case class Hit(attacker: String, target: String, bac: Double)
	case class GetUsers()
	def props(id: Int, name: String, desc: String, bar: Int) = Props(classOf[Room], id, name, desc, bar)
}

class Room(val id: Int, val name: String, val desc: String, bar: Int) extends Actor{
	import Room._
	var commandHandler: ActorRef = if (bar == 0) context.actorOf(Props(classOf[RegularRoomCommandHandler])) else context.actorOf(Props(new BarCommandHandler(self)))
	var exits: Map[String,(String,ActorRef)] = Map()
	var users: Map[String,Player] = Map()
	def receive = {
		case SetExits(exits) => this.exits = exits
		case Arrive(name, player) => {
			player.ref ! Welcome(this roomDescription(name))
			commandHandler ! GiveCommandSet(player.ref)
			player.origin match{
				case WV => users foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " arrives with a steady gait.")}
				case NJ => users foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " arrives, trying to look like a badass.")} 
			}
			users = users + ((name,player))
		}
		case Depart(name, dir, bac) => {
			if (exits.isDefinedAt(dir)){
				val player = users(name)
				(Random.nextInt(100) / bac) match{
		  		case n if (n < 20 && bac > 1) => {
		  			player.ref ! UserMessage("You trip trying to leave.")
		  			(users - name) foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " trips and falls on his face.  He's drunk!")}
		  		}
		  		case _ => {
		  			users = users - name
		  			commandHandler ! RemoveCommandSet(player.ref)
						exits(dir)._2 ! Arrive(name, player)
						users foreach { case (_, player) => player.ref ! UserMessage(name + " has departed " + getFullDirection(dir) + ".")}
		  		}
				}
			} else sender ! UserMessage("You cannot go that direction.")
		} 
		case Say(name, msg) => (users - name) foreach { case (_, player) => player.ref ! UserMessage(name + " says, '" + msg + "'")}
		case Look(name) => sender ! UserMessage(this roomDescription(name))
		case Hit(attackerName, targetName, bac) => {
			val attacker = users(attackerName)
			users.find( { case (name,player) => name.toLowerCase.startsWith(targetName.toLowerCase) } ) match {
				case Some((name,player)) => {
					if (successfulHit(bac)){
						player.ref ! GetHit(attackerName, (Random.nextInt(20).toDouble * bac).toInt)
						sender ! UserMessage("You hit " + name + ".")
					} else {
						sender ! UserMessage("You whiff.")
						player.ref ! UserMessage(attackerName + " tries to hit you but misses.")
					}
				}
				case None => sender ! UserMessage("That person does not seem to be here.")
			}
		}
		case GiveDrink(playerRef) => users.find( { case (name,player) => player.ref == playerRef } ) match{
			case Some((name,player)) => player.ref ! BuyDrink; (users - name) foreach { case (_, occupant) => occupant.ref ! UserMessage(name + " trips and falls on his face.  He's drunk!")}
			case None => ;
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
	
	def successfulHit(bac: Double): Boolean = (Random.nextInt(100) / bac) > 20
	
	def getFullDirection(dir: String): String = dir match {
		case "n" => "north"
		case "s" => "south"
		case "e" => "east"
		case "w" => "west"
	}
	def roomDescription(username: String) = {
		name + "\n" +
		desc + "\n" +
		((exits.toList) map { case (dir,(name,_)) => getFullDirection(dir) + ": " + name}).mkString("\n") + "\n" +
		((((users - username).toList) map { case (name, player) => {
			player.origin  match {
				case WV => name + " is here, wearing a WVU shirt."
				case NJ => name + " is here, his baseball cap rotated sideways."
			}
		}}).mkString("\n"))
	}
}

class Player(val name: String, val origin: Origin, val ref: ActorRef)