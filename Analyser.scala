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
  def run() =
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

      BankName(fileName.replace("_OUT.CSV", "")) -> transactions
    }.toMap

    val firstYearMonth = transactions.values.flatten.map(t => YearMonth.from(t.date)).min
    val lastYearMonth = transactions.values.flatten.map(t => YearMonth.from(t.date)).max

    val allYearMonths = (firstYearMonth.getYear to lastYearMonth.getYear).flatMap { year =>
      (1 to 12).map { month =>
        YearMonth.of(year, month)
      }
    }

    val groupedByCategory: Map[BankName, Map[Category, List[Transaction]]] = transactions
      .map { case (bankName, data) =>
        val missingCategories = Category.values.toSet -- data.map(_.category).toSet
        val grouped: Map[Category, List[Transaction]] = data.groupBy(_.category)
        val missing: Map[Category, List[Transaction]] = missingCategories.map { category =>
          category -> List.empty[Transaction]
        }.toMap
        bankName -> (grouped ++ missing)
      }

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

    writer.writeRow(List("", "") ::: allYearMonths.map(_.toString).toList)

    val categories = Category.values.toList.sortWith(_.name < _.name)

    groupedByMonth.map { case (bankName, data) =>
      categories.map { category =>
        writer.writeRow(
          List(bankName, category.toString()) ::: allYearMonths.map { yearMonth =>
            val transactionsFromMonthFromCategory = data.getOrElse(category, Map.empty).getOrElse(yearMonth, List.empty)
            (transactionsFromMonthFromCategory
              .filter(_.currency == "PLN")
              .map(_.amount)
              .sum +
              transactionsFromMonthFromCategory
                .filter(_.currency == "EUR")
                .map(_.amount)
                .sum * BigDecimal(4.30)).toString()
          }.toList
        )
      }
    }

    writer.close()
    logGreen(s"Summary written to ${fileOut.getAbsolutePath}")
