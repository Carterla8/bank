package io.larry.bank.actors.events

import io.larry.bank.domain.BankAccount

enum BankAccountEvent:
  case BankAccountCreated(bankAccount: BankAccount)
  case BalanceUpdated(newBalance: Double)
