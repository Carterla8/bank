package io.larry.bank.actors

import io.larry.bank.actors.commands.BankAccountCommand
import io.larry.bank.actors.events.BankEvent
import io.larry.bank.actors.responses.BankAccountResponse.{BankAccountBalanceUpdatedResponse, GetBankAccountResponse}
import io.larry.bank.actors.events.BankEvent.BankAccountCreated
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors as Behaviours}
import org.apache.pekko.persistence.typed.scaladsl.Effect
import org.apache.pekko.actor.typed.{ActorRef, Behavior as Behaviour}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.EventSourcedBehavior

import java.util.UUID
import scala.util.Failure

object BankActor {

  // commands = messages

  import BankAccountCommand._

  // events


  // state
  import io.larry.bank.domain.Bank


  // command handler
  def commandHandler(context: ActorContext[BankAccountCommand]): (Bank, BankAccountCommand) => Effect[BankEvent, Bank] = (state, command) => command match {
    case createCmd @ CreateBankAccount(_, _, _, _) =>
      val id = UUID.randomUUID().toString
      val newBankAccount = context.spawn(PersistentBankAccountActor(id), id)
      Effect
        .persist(BankAccountCreated(id))
        .thenReply(newBankAccount)(_ => createCmd)
    case updateCmd @ UpdateBalance(id, _, _, replyTo) => state.accounts.get(id).fold
      (Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(Failure(new RuntimeException("Bank account cannot be found"))))) // id not found in bank
      (a => Effect.reply(a)(updateCmd)) // id found
    case getCmd @ GetBankAccount(id, replyTo) => state.accounts.get(id).fold
      (Effect.reply(replyTo)(GetBankAccountResponse(None))) // id not found
      (a => Effect.reply(a)(getCmd))
  }


  // event hanlder
  def eventHandler(context: ActorContext[BankAccountCommand]): (Bank, BankEvent) => Bank = (state, event) => event match {
    case BankAccountCreated(id) =>
      val acc = context.child(id).getOrElse(context.spawn(PersistentBankAccountActor(id), id)) // exits after command handler - but does not exist in recovery mode
        .asInstanceOf[ActorRef[BankAccountCommand]]
      state.copy(state.accounts + (id -> acc))
  }


  //apply method
  def apply(): Behaviour[BankAccountCommand] = Behaviours.setup { context =>
    EventSourcedBehavior[BankAccountCommand, BankEvent, Bank](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = Bank(accounts = Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }

}
