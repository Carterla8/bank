package io.larry.bank.actors.commands

import io.larry.bank.actors.responses.BankAccountResponse
import io.larry.bank.domain.Currency
import org.apache.pekko.actor.typed.ActorRef

enum BankAccountCommand:
  case CreateBankAccount(user: String, currency: Currency, initialBalance: Double, replyTo: ActorRef[BankAccountResponse])
  case UpdateBalance(id: String, currency: Currency, amount: Double /* can be less than 0 */ , replyTo: ActorRef[BankAccountResponse])
  case GetBankAccount(id: String, replyTo: ActorRef[BankAccountResponse])
