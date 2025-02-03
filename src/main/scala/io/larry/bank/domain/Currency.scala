package io.larry.bank.domain

import io.circe.{Decoder, Encoder, Json}

import scala.util.Try

enum Currency:
  case USD, GBP, JPY, EUR, NotACurrency

object Currency:
  given Conversion[String, Currency] = fromString(_).fold(
    err => throw new IllegalArgumentException(err.getMessage),
    identity
  )

  def fromString(str: String): Either[IllegalArgumentException, Currency] =
    Currency.values.find(_.toString == str)
      .toRight(new IllegalArgumentException(s"Invalid currency: $str"))

  given Encoder[Currency] = Encoder.instance(c => Json.fromString(c.toString))
  given Decoder[Currency] = Decoder.decodeString.emap(str =>
    fromString(str).left.map(_.getMessage)
  )
