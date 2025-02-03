package io.larry.bank.http

import cats.data.Validated.{Invalid, Valid}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.Location
import org.apache.pekko.http.scaladsl.server.Route
import io.circe.generic.auto.*
import io.larry.bank.actors.commands.BankAccountCommand
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport.*
import io.larry.bank.actors.commands.BankAccountCommand.GetBankAccount
import io.larry.bank.actors.responses.BankAccountResponse
import io.larry.bank.actors.responses.BankAccountResponse.*
import io.larry.bank.domain.{BankAccountCreationRequest, BankAccountUpdateRequest, FailureResponse}
import io.larry.bank.http.Validation.{Validator, validateEntity}
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.util.{Success, Failure}


class BankRouter(bank: ActorRef[BankAccountCommand])(using system: ActorSystem[_]) {
  given timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreationRequest): Future[BankAccountResponse] =
    bank.ask(replyTo => request.toCommand(replyTo))

  def getBankAccount(id: String): Future[BankAccountResponse] =
    bank.ask(replyTo => GetBankAccount(id, replyTo))

  def updateBankAccount(id: String, request: BankAccountUpdateRequest): Future[BankAccountResponse] =
    bank.ask(replyTo => request.toCommand(id, replyTo))


  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route =
    validateEntity(request) match {
      case Valid(_) => routeIfValid
      case Invalid(failures) => complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }



  /*
    POST /bank/
        Payload: bank acc creation request as JSON
        Response:
          201 Created
          Location: /bank/uuid

    GET /bank/uid/
        Response:
          200 OK
            JSON representation of bank details

          404 Not Found


    PUT /bank/uid/
      Payload: (currency, amount) as json
      Response
        200 OK
          Payload: New bank account details as json
        404 Not Found
        TODO 400 Bad Request

   */

  val routes =
    pathPrefix("bank") {
      pathEndOrSingleSlash {
        post {
          entity(as[BankAccountCreationRequest]) {
            request =>
              //validation
              validateRequest(request) {
                /*
                - convert the request into a command for the bank actor
                - send the command to the bank
                - expect a reply
               */
                onSuccess(createBankAccount(request)) { // send back a http response
                  case BankAccountCreatedResponse(id) =>
                    respondWithHeader(Location(s"/bank/$id")) {
                      complete(StatusCodes.Created)
                    }
                }
              }
          }
        }
      } ~
        path(Segment) { id =>
          get {
            /*
            - send command to bank
            - expect a reply
            - send back the http response
             */
            onSuccess(getBankAccount(id)) {
              case GetBankAccountResponse(Some(account)) =>
                complete(account)
              case GetBankAccountResponse(None) =>
                complete(StatusCodes.NotFound, FailureResponse(s"Bank account $id cannot be found."))
            }

          } ~ put {

            entity(as[BankAccountUpdateRequest]) { request =>
              //  validation
              validateRequest(request) {
                /*
                - transform request into command
                -  send the command to the bank
                - expect a reply
                - send back a HTTP response
                 */

                onSuccess(updateBankAccount(id, request)) {
                  case BankAccountBalanceUpdatedResponse(Success(account)) =>
                    complete(account)
                  case BankAccountBalanceUpdatedResponse(Failure(ex)) =>
                    complete(StatusCodes.BadRequest, FailureResponse(s"${ex.getMessage}"))
                }
              }
            }
          }
        }
    }
}
