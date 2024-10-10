package com.hungry.wheremoney

import cats.conversions.all
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
  def run(eurRate: BigDecimal, usdRate: BigDecimal) =
    logGreen(Console.BOLD + s"📊 Running analysis with EUR rate: $eurRate and USD rate: $usdRate 📊" + Console.RESET)
    val listOutputCsvFiles = Utils.listOutputCsvFiles
    println(s"Found output files: [${listOutputCsvFiles.mkString(", ")}]")

    val transactions: Map[BankName, List[Transaction]] = listOutputCsvFiles.map { fileName =>
      println(s"Processing $fileName... 🔄")
      val reader = CSVReader.open(new File(s"./outputs/$fileName"))(new DefaultCSVFormat {
        override val delimiter = ';'
      })

      val transactions: List[Transaction] = reader
        .all()
        .drop(1)
        .flatMap {
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

      reader.close()
      BankName(fileName.replace("_OUT.CSV", "")) -> transactions
    }.toMap

    val firstYearMonth = transactions.values.flatten.map(t => YearMonth.from(t.date)).min
    val lastYearMonth = transactions.values.flatten.map(t => YearMonth.from(t.date)).max
    val allYearMonths = (firstYearMonth.getYear to lastYearMonth.getYear).flatMap { year =>
      (1 to 12).map { month =>
        YearMonth.of(year, month)
      }
    }

    val groupedByCategory: Map[BankName, Map[Category, List[Transaction]]] = transactions.mapValues(_.groupBy(_.category)).toMap
    val groupedByMonth: Map[BankName, Map[Category, Map[YearMonth, List[Transaction]]]] = groupedByCategory.map {
      case (bankName, byCategory) =>
        bankName -> (byCategory.mapValues { transactions =>
          transactions.groupBy(t => YearMonth.from(t.date)).toMap
        }).toMap
    }

    val fileOut = new File(s"./outputs/SUMMARY.csv")
    val writer = CSVWriter.open(fileOut, append = false)(new DefaultCSVFormat {
      override val delimiter = ';'
    })

    // Header row
    writer.writeRow(List(s"Euro rate: $eurRate", "") ::: allYearMonths.map(_.toString).toList)

    val categories = Category.values.toList.sortWith(_.name < _.name)
    groupedByMonth.map { case (bankName, data) =>
      categories.map { category =>
        writer.writeRow(
          List(bankName, category.toString()) ::: allYearMonths.map { yearMonth =>
            val transactionsFromMonthFromCategory = data.getOrElse(category, Map.empty).getOrElse(yearMonth, List.empty)

            val plnSum = transactionsFromMonthFromCategory
              .filter(_.currency == "PLN")
              .map(_.amount)
              .sum

            val eurSum = transactionsFromMonthFromCategory
              .filter(_.currency == "EUR")
              .map(_.amount)
              .sum * eurRate

            val usdSum = transactionsFromMonthFromCategory
              .filter(_.currency == "USD")
              .map(_.amount)
              .sum * usdRate

            (plnSum + eurSum + usdSum).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toString()
          }.toList
        )
      }
    }

    writer.close()
    logGreen(s"Summary exported! 🚀")
