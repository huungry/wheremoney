package com.hungry.wheremoney

import com.github.tototoshi.csv.CSVReader

import java.io.File
enum Category:
  case AccountPartner extends Category
  case AccountPersonal extends Category
  case AccountShared extends Category

  case Accounting extends Category
  case Car extends Category
  case Cash extends Category
  case Charity extends Category
  case Clothes extends Category
  case Cosmetic extends Category
  case Entertainment extends Category
  case Gifts extends Category
  case Groceries extends Category
  case Healthcare extends Category
  case Home extends Category
  case Income extends Category
  case InternetShopping extends Category
  case Mortgage extends Category
  case OrderedFood extends Category
  case OutsideFood extends Category
  case PublicTransport extends Category
  case Revolut extends Category
  case Sport extends Category
  case Subscriptions extends Category
  case Taxi extends Category
  case Taxes extends Category
  case Traveling extends Category
  case Uncategorized extends Category
  case ZUS extends Category

  def name = this.toString.toLowerCase

class CategoryBuilder(config: Config) {
  def unsafeBuild(): Map[Category, List[String]] =
    Category.values.map(category => category -> config.keywords(category.name)).toMap
}
