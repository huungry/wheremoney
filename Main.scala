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
    val eurRateRegex = """eurRate=(\d+(\.\d+)?)""".r
    val usdRateRegex = """usdRate=(\d+(\.\d+)?)""".r

    val eurRate = args
      .find(eurRateRegex.findFirstIn(_).isDefined)
      .flatMap {
        case eurRateRegex(rate, _) => Try(BigDecimal(rate)).toOption
        case _                     => None
      }
      .getOrElse {
        logYellow("No valid EUR rate provided, using default 4.30. Use 'eurRate=4.30' to provide a custom rate.")
        BigDecimal(4.30)
      }

    val usdRate = args
      .find(usdRateRegex.findFirstIn(_).isDefined)
      .flatMap {
        case usdRateRegex(rate, _) => Try(BigDecimal(rate)).toOption
        case _                     => None
      }
      .getOrElse {
        logYellow("No valid USD rate provided, using default 3.80. Use 'usdRate=3.80' to provide a custom rate.")
        BigDecimal(3.80)
      }

    Analyser.run(eurRate, usdRate)
  else Parser.run()
