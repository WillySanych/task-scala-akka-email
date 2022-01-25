import com.typesafe.scalalogging._
import org.slf4j.LoggerFactory

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import javax.mail._
import javax.mail.internet._
import java.util._

object Main extends App{

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  object Guard {

    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { ctx =>
        val sender: ActorRef[Sender.msg] = ctx.spawn(Sender(), "sender")

        Behaviors.receiveMessage { _ =>

          println("Enter your e-mail:")
          val emailSender = scala.io.StdIn.readLine()

          println("Enter your password:")
          val emailSenderPassword = scala.io.StdIn.readLine()

          println("Enter the receiver:")
          val emailReceiver = scala.io.StdIn.readLine()

          println("Enter the sending YouTube link:")
          val text = scala.io.StdIn.readLine()

          sender ! Sender.msg(emailSender, emailSenderPassword, emailReceiver, text)
          Behaviors.receive{ (_, _) =>
            Behaviors.stopped
          }
        }
      }
    }
  }
  object Sender {
    case class msg (emailSender: String, emailSenderPassword: String, emailReceiver: String, text: String)

    def apply(): Behavior[msg] = {
      Behaviors.receiveMessage{
        case msg(emailSender, emailSenderPassword, emailReceiver, text) =>
          val refYT = """https:\/\/(?:youtu\.be\/|(?:[a-z]{2,3}\.)?youtube\.com\/watch(?:\?|#\!)v=)[\w-]{11}.*""".r
          text match {
            case refYT() =>
              val properties = new Properties()
              properties.put("mail.smtp.host", "smtp.gmail.com")
              properties.put("mail.smtp.starttls.enable", "true")
              properties.put("mail.smtp.ssl.trust", "smtp.gmail.com")
              properties.put("mail.transport.protocol", "smtp")
              properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2")

              val session = Session.getDefaultInstance(properties)
              val message = new MimeMessage(session)

              message.setFrom(s"<$emailSender>")
              message.addRecipients(Message.RecipientType.TO, s"<$emailReceiver>")
              message.setText(s"$text")

              Transport.send(message, emailSender, emailSenderPassword)
              logger.info("The e-mail was sent.")
              Behaviors.same
            case _ =>
              logger.error("You entered not a YouTube link.")
              Behaviors.same
          }
      }
    }
  }
  val guard: ActorSystem[NotUsed] = ActorSystem (Guard(), "guard")

  guard ! NotUsed
  guard ! NotUsed
}
