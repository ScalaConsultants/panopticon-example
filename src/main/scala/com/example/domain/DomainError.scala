package com.example.domain

sealed trait DomainError {
  def asThrowable: Throwable = this match {
    case RepositoryError(cause) => cause
    case ValidationError(msg)   => new Throwable(msg)
  }
}
final case class RepositoryError(cause: Exception) extends DomainError
final case class ValidationError(msg: String)      extends DomainError
