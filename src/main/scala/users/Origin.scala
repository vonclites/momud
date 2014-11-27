package users

object Origin {
  sealed abstract class Origin(val fullName: String, val abbreviation: String) {
    override def toString = abbreviation
  }
  case object WV extends Origin("West Virginia", "WV")
  case object NJ extends Origin("New Jersey", "NJ")
  
  def determineOrigin(proposedOrigin: String): Origin = {
    if (proposedOrigin.equalsIgnoreCase(WV.fullName) || proposedOrigin.equalsIgnoreCase(WV.abbreviation)) WV
    else if (proposedOrigin.equalsIgnoreCase(NJ.fullName) || proposedOrigin.equalsIgnoreCase(NJ.abbreviation)) NJ
    else null
  }
}