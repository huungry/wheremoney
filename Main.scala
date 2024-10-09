//> using dep com.github.tototoshi::scala-csv:2.0.0
//> using dep io.circe::circe-core:0.14.10
//> using dep io.circe::circe-parser:0.14.10
//> using dep io.circe::circe-generic:0.14.10

package com.hungry.wheremoney

import com.hungry.wheremoney.Utils.*

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import Analyser.*
import Parser.*

def main(args: Array[String]): Unit =
  println(args.mkString("App called with arguments: ", ", ", ""))
  if args.exists(_.equalsIgnoreCase("analysis")) then
    args.find(_.startsWith("eurRate")).map(_.split("=")(1)) match
      case None =>
        logYellow("No EUR rate provided, using default 4.30. You can provide a custom rate with argument eurRate=4.30")
        Analyser.run(BigDecimal(4.30))
      case Some(value) =>
        Try(BigDecimal(value)) match
          case Failure(exception) =>
            logRed(s"Invalid EUR rate provided: $value. Please use a valid argument, for example eurRate=4.30")
          case Success(eurRate) =>
            Analyser.run(eurRate)
  else Parser.run()
