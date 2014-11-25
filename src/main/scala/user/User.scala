

package user

import akka.actor.Actor
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

case class ClientConnection(port: Socket) 

class User extends Actor {
  private var userInput: BufferedReader = null
  private var userOutput: PrintWriter = null

  implicit def inputStreamWrapper(input: InputStream) = new BufferedReader(new InputStreamReader(input))

  implicit def outputStreamWrapper(output: OutputStream) = new PrintWriter(new OutputStreamWriter(output))

  def receive = {
    case c: ClientConnection => createConnection(c.port.getInputStream, c.port.getOutputStream)
  }

  private def createConnection(input: BufferedReader, output: PrintWriter) {
    userInput = input
    userOutput = output

    userOutput.println("Wassup fresh, welcome to Morgantown. By what name can I refer to my new homie?")
    userOutput.flush
    var username = userInput.readLine
    println("User created for: " + username)
    userOutput.println("Nice to meet you " + username + ". Grab yourself a beer, meet some new people, or bust some heads. " +
      "Whatever gets you there.")
    userOutput.flush

    while (true) {
      val command = userInput.readLine()
      userOutput.println(username + " => " + command)
      userOutput.flush
    }
  }
}