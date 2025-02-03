package io.larry.bank.domain

import io.larry.bank.actors.commands.BankAccountCommand
import io.larry.bank.actors.commands.BankAccountCommand.{UpdateBalance}
import io.larry.bank.actors.responses.BankAccountResponse
import io.larry.bank.http.Validation.{Validator, validateMinimum, validateMinimumAbs, validateRequired}
import org.apache.pekko.actor.typed.ActorRef
import cats.implicits.*

case class BankAccountUpdateRequest(currency: String, amount: Double) {
  def toCommand(id: String, replyTo: ActorRef[BankAccountResponse]): BankAccountCommand = UpdateBalance(id, currency, amount, replyTo)
}

object BankAccountUpdateRequest {
  given validator: Validator[BankAccountUpdateRequest] = (request: BankAccountUpdateRequest) => {
    val currencyValidation = validateRequired(request.currency, "currency")
    val amountValidation = validateMinimumAbs(request.amount, 0.01, "amount")

    (currencyValidation, amountValidation).mapN(BankAccountUpdateRequest.apply)
  }
}