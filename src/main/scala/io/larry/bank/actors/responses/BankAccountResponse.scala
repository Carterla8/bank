package io.larry.bank.actors.responses

import io.larry.bank.domain.BankAccount

import scala.util.Try

enum BankAccountResponse:
  case BankAccountCreatedResponse(id: String)
  case BankAccountBalanceUpdatedResponse(bankAccountOpt: Try[BankAccount])
  case GetBankAccountResponse(bankAccountOpt: Option[BankAccount])
