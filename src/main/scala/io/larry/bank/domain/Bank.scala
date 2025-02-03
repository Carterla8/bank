package io.larry.bank.domain

import io.larry.bank.actors.commands.BankAccountCommand
import org.apache.pekko.actor.typed.ActorRef

case class Bank(accounts: Map[String, ActorRef[BankAccountCommand]])
