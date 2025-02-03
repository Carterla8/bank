package io.larry.bank.http

import cats.data.ValidatedNel
import cats.implicits.*
import io.larry.bank.domain.Currency


object Validation {

  // field must be present
  trait Required[A] extends (A => Boolean)

  // currency validation
  trait CurrencyValidation[A] extends (A => Either[IllegalArgumentException, Currency])

  // initial balance cannot be -ve
  trait Minimum[A] extends ((A, Double) => Boolean) // for numerical fields

  trait MinimumAbs[A] extends ((A, Double) => Boolean) // for numerical fields

  // Type class instances
  given minimumInt: Minimum[Int] = _ >= _

  given minimumAbsInt: MinimumAbs[Int] = Math.abs(_) >= _

  given minimumDouble: Minimum[Double] = _ >= _

  given minimumAbsDouble: MinimumAbs[Double] = Math.abs(_) >= _

  given requiredString: Required[String] = _.nonEmpty

  given currencyValidation: CurrencyValidation[String] = (str: String) => Currency.fromString(str)
  
  // usage
  def required[A](value: A)(using req: Required[A]): Boolean = req(value)

  def minimum[A](value: A, threshold: Double)(using min: Minimum[A]): Boolean = min(value, threshold)

  def minimumAbs[A](value: A, threshold: Double)(using min: MinimumAbs[A]): Boolean = min(value, threshold)

  def validateCurrency[A](value: A)(using cv: CurrencyValidation[A]): Either[IllegalArgumentException, String] =
    cv(value).map(_.toString)

  // Validated
  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  // validation failures
  trait ValidationFailure {
    def errorMessage: String
  }

  case class EmptyField(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"${fieldName} is empty"
  }

  case class NegativeValue(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"${fieldName} is negative"
  }

  case class BelowMinimumValue(fieldName: String, min: Double) extends ValidationFailure {
    override def errorMessage: String = s"${fieldName} is below minimum threshold ${min}"
  }

  case class InvalidCurrency(value: String) extends ValidationFailure {
    override def errorMessage: String = s"Invalid currency: $value"
  }

  // 'main' api
  def validateMinimum[A: Minimum](value: A, threshold: Double, fieldName: String): ValidationResult[A] =
    if (minimum(value, threshold))
      value.validNel
    else if (threshold == 0) NegativeValue(fieldName).invalidNel
    else BelowMinimumValue(fieldName, threshold).invalidNel

  def validateMinimumAbs[A: MinimumAbs](value: A, threshold: Double, fieldName: String): ValidationResult[A] =
    if (minimumAbs(value, threshold))
      value.validNel
    else BelowMinimumValue(fieldName, threshold).invalidNel

  def validateRequired[A: Required](value: A, fieldName: String): ValidationResult[A] =
    if (required(value)) value.validNel
    else EmptyField(fieldName).invalidNel


  def validateCurrencyString(value: String, fieldName: String): ValidationResult[String] =
    validateCurrency(value)
      .fold(
        _ => InvalidCurrency(value).invalidNel,
        _ => value.validNel
      )

  // general type class for requests
  trait Validator[A] {
    def validate(request: A): ValidationResult[A]
  }

  def validateEntity[A](value: A)(using validator: Validator[A]): ValidationResult[A] = validator.validate(value)


}
