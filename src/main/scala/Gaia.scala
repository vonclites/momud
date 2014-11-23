import akka.actor._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.StaticQuery.interpolation
import scala.collection.immutable.Map
import scala.concurrent.duration._



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

object Gaia extends App{
	import Room._
	val system = ActorSystem("momud")
  val rooms: TableQuery[Rooms] = TableQuery[Rooms]
  val exits: TableQuery[Exits] = TableQuery[Exits]
  var world: Map[Int,(String,ActorRef)] = Map();

	val db = Database.forURL("jdbc:mysql://localhost:3306/mud", driver="com.mysql.jdbc.Driver", user="root", password="root")
  db.withSession { implicit session => 	
  	def buildWorld = {
  		createRooms
  		setExits
  	}
  	def createRooms = {
	  	rooms foreach { case (id, name, desc) =>
	  		val room: ActorRef = system.actorOf(Room.props(id, name, desc), id+"-"+name)
	  		val mapping: (Int,(String,ActorRef)) = (id,(name,room))
	  		world = world + mapping
	    }
  	}
		def setExits = {
	  	world foreach{ case(id, room) =>
	  		val exitQuery = sql"SELECT exits.dir, exits.to FROM exits JOIN rooms ON rooms.id = exits.from WHERE exits.from = $id".as[(String,Int)]
	  		var exits: Map[String,(String,ActorRef)] = Map()
	  		exitQuery foreach { case(dir, dest) =>
	  			val mapping: (String,(String,ActorRef)) = (dir, world(dest))
	  			exits = exits + mapping
	  		}
	  		room._2  ! SetExits(exits)
	  	}
		}
		buildWorld
  	val inbox = Inbox.create(system)
  	inbox.send(world(2)._2 , "Get Exits")
  	val exits = inbox.receive(5.seconds)
  	println(exits)
	}
}
