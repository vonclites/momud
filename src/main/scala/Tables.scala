import scala.slick.driver.MySQLDriver.simple._
import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}

class Rooms(tag: Tag)
  extends Table[(Int, String, String)](tag, "rooms") {

  def id: Column[Int] = column[Int]("id", O.PrimaryKey)
  def name: Column[String] = column[String]("name")
  def desc: Column[String] = column[String]("description")
  
  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String, String)] =
    (id, name, desc)
}

class Exits(tag: Tag)
  extends Table[(Int,Int,String)](tag, "exits") {

  def from: Column[Int] = column[Int]("from")
  def to: Column[Int] = column[Int]("to")
  def dir: Column[String] = column[String]("dir")
  
  def * : ProvenShape[(Int, Int, String)] =
    (from, to, dir)
  
  // A reified foreign key relation that can be navigated to create a join
  def origin: ForeignKeyQuery[Rooms, (Int, String, String)] = 
    foreignKey("FROM_FK", from, TableQuery[Rooms])(_.id)
  def destination: ForeignKeyQuery[Rooms, (Int, String, String)] = 
    foreignKey("TO_FK", to, TableQuery[Rooms])(_.id)
}