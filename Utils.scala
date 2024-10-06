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

object Utils {
  def listInputCsvFiles: List[String] =
    val dir = new File("./inputs")
    if dir.exists && dir.isDirectory then
      dir.listFiles.filter(_.isFile).map(_.getName.toUpperCase).filter(_.endsWith(".CSV")).toList
    else List.empty

  def listOutputCsvFiles: List[String] =
    val dir = new File("./outputs")
    if dir.exists && dir.isDirectory then
      dir.listFiles.filter(_.isFile).map(_.getName.toUpperCase).filter(_.endsWith("_OUT.CSV")).toList
    else List.empty

  def findLastProceccedDate(
      bankName: BankName,
      dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE
  ): Option[LastProcessedDate] = {
    val readerOpt = Try {
      CSVReader.open(new File(s"./outputs/${bankName}_OUT.csv"))(new DefaultCSVFormat {
        override val delimiter = ';'
      })
    }.toOption

    readerOpt.flatMap { reader =>
      reader.all().lastOption match
        case Some(date :: _) =>
          println(s"[$bankName] Last processed date: $date")
          reader.close()
          Some(LastProcessedDate(LocalDate.parse(date)))
        case _ =>
          println(s"[$bankName] No last processed date found")
          None
    }
  }

  def logUncategorized(data: Seq[Transaction], bankName: BankName): Unit = {
    val count = data.count(_.category == Category.Uncategorized)
    println(s"[$bankName] Uncategorized: $count out of ${data.size}")
  }

  def exportCsv(data: Seq[Transaction], bankName: BankName): Unit =
    println(s"[$bankName] Starting to write to file... 📝")
    val sortedByDate = data.sortBy(_.date)
    val fileOut = new File(s"./outputs/${bankName}_OUT.csv")
    val writer = CSVWriter.open(fileOut, append = true)(new DefaultCSVFormat {
      override val delimiter = ';'
    })
    // write the header if new file
    findLastProceccedDate(bankName) match
      case None =>
        writer.writeRow(
          List(
            "Date",
            "Account",
            "Receiver",
            "Description",
            "Amount",
            "Currency",
            "Category",
            "Category Keyword"
          )
        )
      case _ if data.nonEmpty => writer.writeRow(List.empty)
      case _                  => ()
      // write the data
    writer.writeAll(sortedByDate.map { transaction =>
      List(
        transaction.date.toString,
        transaction.account,
        transaction.receiver,
        transaction.description,
        transaction.amount,
        transaction.currency,
        transaction.category.toString,
        transaction.categoryKeyword.getOrElse("")
      )
    })
    writer.close()
    println(s"[$bankName] Done writing to file! 🎉")

  def logGreen(message: String): Unit = println(Console.GREEN + message + Console.RESET)
  def logRed(message: String): Unit = println(Console.RED + message + Console.RESET)
}
