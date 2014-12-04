package users

import akka.actor._
import rooms._

case class PrepMove(dir: String)
case class PrepHit(target: String)

class ActionBuffer extends Actor{
	def receive: Receive = {
		case PrepMove(dir) => sender ! PerformMove(dir); Thread.sleep(2500)
		case PrepHit(target) => sender ! PerformHit(target); Thread.sleep(2500)
	}
}