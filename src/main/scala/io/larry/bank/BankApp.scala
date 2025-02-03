package io.larry.bank

import io.larry.bank.actors.commands.RootCommand.RetrieveBankActor
import io.larry.bank.actors.BankActor
import io.larry.bank.actors.commands.{BankAccountCommand, RootCommand}
import io.larry.bank.http.BankRouter
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior as Behaviour}
import org.apache.pekko.actor.typed.scaladsl.Behaviors as Behaviours
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.{Try, Success, Failure}


object BankApp {
  def startHttpServer(bank: ActorRef[BankAccountCommand])(using system: ActorSystem[_]) = {
    given ec: ExecutionContext = system.executionContext
    val router = new BankRouter(bank)
    val routes = router.routes
    val httpBindingFuture = Http().newServerAt("localhost", 8080).bind(routes)
    httpBindingFuture.onComplete{
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        system.log.error(s"Failed to bind with http server with: ${ex}")
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehaviour: Behaviour[RootCommand] = Behaviours.setup { context =>
      val bankActor = context.spawn(BankActor(), "bank")
      Behaviours.receiveMessage {
        case RetrieveBankActor(replyTo) =>
          replyTo ! bankActor
          Behaviours.same
      }
    }

    given system: ActorSystem[RootCommand] = ActorSystem(rootBehaviour, "BankSystem")
    given timeout: Timeout = Timeout(5.seconds)
    given ec: ExecutionContext = system.executionContext
    val bankActorFuture: Future[ActorRef[BankAccountCommand]] = system.ask(replyTo => RetrieveBankActor(replyTo))
    bankActorFuture.foreach(startHttpServer)
  }
}
