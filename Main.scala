//> using dep com.github.tototoshi::scala-csv:2.0.0
//> using dep io.circe::circe-core:0.14.10
//> using dep io.circe::circe-parser:0.14.10
//> using dep io.circe::circe-generic:0.14.10

package com.hungry.wheremoney

import com.github.tototoshi.csv.*

import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import Transaction.*
import Category.*
import Utils.*

@main def run() =

  println("Parsing config... 📜")
  val config = Config.unsafeParse()
  logGreen("Config parsed successfully ✅")

  println("Building categories... 🏗")
  val categories = CategoryBuilder(config).unsafeBuild()
  logGreen("Categories built successfully ✅")

  println("Building categorizer... 🏗")
  val categorizer = Categorizer(categories)
  logGreen("Categorizer built successfully ✅")

  println("Building banks handler... 🏗")
  val bankHandler = BankHandler(categorizer)
  logGreen("Banks handler built successfully ✅")
  import bankHandler.*

  logGreen("Starting... 🚀")

  val listInputCsvFiles = Utils.listInputCsvFiles
  val banksToProcess: List[SupportedBank] = listInputCsvFiles.flatMap { fileName =>
    SupportedBank.values.find(bank => fileName.replace(".CSV", "") == (bank.name.toString()))
  }

  logGreen(s"Found input files for banks: [${banksToProcess.mkString(", ")}]")

  banksToProcess.map(processBankData)

  logGreen("Finished! 🏁")
