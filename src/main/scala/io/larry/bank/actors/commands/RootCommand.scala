package io.larry.bank.actors.commands

import io.larry.bank.actors.commands.BankAccountCommand
import org.apache.pekko.actor.typed.ActorRef

enum RootCommand:
  case RetrieveBankActor(replyTo: ActorRef[ActorRef[BankAccountCommand]]) extends RootCommand
