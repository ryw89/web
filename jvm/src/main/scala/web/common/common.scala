package com.ryanwhittingham.web.common

import org.joda.time.DateTime
import wvlet.log.Logger

import java.math.BigInteger
import java.security.MessageDigest
import scala.util.{Failure, Success, Try}

/** Simple helper function for unwrapping a Try[Option[T]] to a T.
  * Will raise an exception if not possible. */
object Unwrap {
  def unwrapTryOptionOrFail[T](x: Try[Option[T]]): T = {
    val unwrappedTry = x match {
      case Success(s) => s
      case Failure(f) => throw new RuntimeException(f)
    }

    val unwrappedOption = unwrappedTry match {
      case Some(s) => s
      case None    => throw new RuntimeException("Option contained a None.")
    }

    unwrappedOption
  }
}

/** Convert Unix timestamp in seconds to a datetime string. */
object UnixTimeToDate {
  private val logger = Logger.of[App]
  def unixTimeToDate(t: Int, fmt: String) = {
    logger.info(s"Converting Unix time ${t} to string...")
    new DateTime(t.toLong * 1000).toDateTime.toString(fmt)
  }
}

object Hash {
  def md5HashString(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }
}
