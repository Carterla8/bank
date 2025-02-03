package io.larry.bank

import io.larry.bank.actors.BankActor
import io.larry.bank.actors.commands.BankAccountCommand
import io.larry.bank.actors.responses.BankAccountResponse
import io.larry.bank.actors.commands.BankAccountCommand.{CreateBankAccount, GetBankAccount}
import io.larry.bank.actors.responses.BankAccountResponse.{BankAccountCreatedResponse, GetBankAccountResponse}
import io.larry.bank.domain.Currency.GBP
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.Behaviors as Behaviours
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, Behavior as Behaviour}
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

object BankPlayground {
  def main(args: Array[String]) = {
    val rootBehaviour: Behaviour[NotUsed] = Behaviours.setup { context =>
      val bank = context.spawn(BankActor(), "bank")
      val logger = context.log

      val responseHandler = context.spawn(Behaviours.receiveMessage[BankAccountResponse] {
        case BankAccountCreatedResponse(id) =>
          logger.info(s"successfully created bank account ${id}")
          Behaviours.same
        case GetBankAccountResponse(optBankAccount) =>
          logger.info(s"Account details: ${optBankAccount}")
          Behaviours.same
      }, "replyHandler")

      given timeout: Timeout = Timeout(2.seconds)

      given scheduler: Scheduler = context.system.scheduler

      given ec: ExecutionContext = context.executionContext

      bank ! CreateBankAccount("Dave", GBP, 10, responseHandler)
//      bank ! GetBankAccount("4f5c2084-0f89-412a-9234-6dc7a1689449", responseHandler)

      Behaviours.empty
    }

    val system = ActorSystem(rootBehaviour, "BankDemo")
  }
}
