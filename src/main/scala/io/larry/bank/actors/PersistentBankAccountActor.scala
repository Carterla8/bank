package io.larry.bank.actors

import io.larry.bank.actors.commands.BankAccountCommand
import io.larry.bank.actors.events.BankAccountEvent
import io.larry.bank.domain
import io.larry.bank.domain.Currency.NotACurrency
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.Effect
import org.apache.pekko.persistence.typed.scaladsl.EventSourcedBehavior

import scala.util.{Failure, Success}

// a single bank account
object PersistentBankAccountActor {
  // event sourcing
  // store events i.e. bits of data which comprise the journey to the latest state of the application at this moment

  /*
   - fault tolerance
   - auditing
   */

  // commands = messages
  import BankAccountCommand._

  // events = to persist to cassandra
  import BankAccountEvent._

  // state
  import io.larry.bank.domain.BankAccount

  // responses
  import io.larry.bank.actors.responses.BankAccountResponse._

  // command handler = message handler => persist an event
  // event handler => update state
  // state
  val commandHandler: (BankAccount, BankAccountCommand) => Effect[BankAccountEvent, BankAccount] =
    (state, command) => command match {
      case CreateBankAccount(user, currency, balance, replyTo) =>
        val id = state.id
        /*
          - bank creates me
          - bank sends me CreateBankAccount
          - I persist BankAccountCreated
          - I update my state
          - reply to bank with BankAccountCreatedResponse
          - Bank surfaces the response to the http server
         */
        Effect.persist(BankAccountCreated(domain.BankAccount(id, user, currency, balance))) // persist into cassandra
          .thenReply(replyTo)(_ => BankAccountCreatedResponse(id))
      case UpdateBalance(_, _, amount /* can't be less than 0 */, replyTo) =>
        val newBalance = state.balance + amount
        if newBalance < 0 then
          Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(Failure(new RuntimeException("Withdrawal amount exceeds available amount"))))
        else
          Effect.persist(BalanceUpdated(amount))
            .thenReply(replyTo)(newState => BankAccountBalanceUpdatedResponse(Success(newState)))
      case GetBankAccount(id, bank) => Effect.reply(bank)(GetBankAccountResponse(Some(state)))
    }

  val eventHandler: (BankAccount, BankAccountEvent) => BankAccount = (state, event) => event match {
    case BankAccountCreated(bankAccount) => bankAccount
    case BalanceUpdated(amount) => state.copy(balance = state.balance + amount)
  }

  def apply(id: String): Behavior[BankAccountCommand] = EventSourcedBehavior[BankAccountCommand, BankAccountEvent, BankAccount](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = BankAccount(id, "", NotACurrency, 0), // unused
    commandHandler = commandHandler,
    eventHandler = eventHandler
  )

}
