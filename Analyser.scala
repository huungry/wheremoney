package com.hungry.wheremoney

import com.github.tototoshi.csv.*

import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import Model.*
import Transaction.*
import Category.*
import Utils.*

object Analyser:
  def run() =
    val listOutputCsvFiles = Utils.listOutputCsvFiles
    println(s"Found output files: [${listOutputCsvFiles.mkString(", ")}]")

    val transactions: Map[String, List[Transaction]] = listOutputCsvFiles.map { fileName =>
      println(s"Processing $fileName... 🔄")
      val reader = CSVReader.open(new File(s"./outputs/$fileName"))(new DefaultCSVFormat {
        override val delimiter = ';'
      })

      val transactions: List[Transaction] = reader.all().drop(1).flatMap {
        case date :: account :: receiver :: description :: amount :: currency :: category :: _ :: _ =>
          Some(
            Transaction(
              date = LocalDate.parse(date),
              account = account,
              receiver = receiver,
              description = description,
              amount = amount.toDouble,
              currency = currency,
              category = Category.valueOf(category),
              categoryKeyword = None
            )
          )
        case line =>
          logRed(s"Error parsing line: $line from $fileName")
          None
      }

      fileName -> transactions
    }.toMap

    transactions.foreach { case (bankName, data) =>
      logGreen(s"[$bankName] Found ${data.size} transactions 📊")
    }
