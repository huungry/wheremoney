//> using dep com.github.tototoshi::scala-csv:2.0.0
//> using dep io.circe::circe-core:0.14.10
//> using dep io.circe::circe-parser:0.14.10
//> using dep io.circe::circe-generic:0.14.10

package com.hungry.wheremoney

import Analyser.*
import Parser.*

def main(args: Array[String]): Unit =
  println(args.mkString("App called with arguments: ", ", ", ""))
  if args.exists(_.equalsIgnoreCase("analysis")) then Analyser.run()
  else Parser.run()
