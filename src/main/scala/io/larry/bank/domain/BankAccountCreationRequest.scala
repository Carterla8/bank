package io.larry.bank.domain

import io.larry.bank.actors.commands.BankAccountCommand
import io.larry.bank.actors.commands.BankAccountCommand.CreateBankAccount
import io.larry.bank.actors.responses.BankAccountResponse
import io.larry.bank.http.Validation._
import org.apache.pekko.actor.typed.ActorRef
import cats.implicits._


// For regular BankAccountCreationRequest
case class BankAccountCreationRequest(user: String, currency: String, balance: Double) {
  def toCommand(replyTo: ActorRef[BankAccountResponse]): BankAccountCommand = CreateBankAccount(user, currency, balance, replyTo)
}

object BankAccountCreationRequest {
  given validator: Validator[BankAccountCreationRequest] = new Validator[BankAccountCreationRequest] {
    override def validate(request: BankAccountCreationRequest): ValidationResult[BankAccountCreationRequest] = {
      val userValidation = validateRequired(request.user, "user")
      val currencyValidation =  validateRequired(request.currency, "currency").combine(validateCurrencyString(request.currency, "currency"))
      val balanceValidation = validateMinimum(request.balance, 0, "balance").combine(validateMinimumAbs(request.balance, 0.01, "balance"))

      (userValidation, currencyValidation, balanceValidation).mapN(BankAccountCreationRequest.apply)
    }
  }
}