package service

import java.io.File

import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory._

class SimpleStorage {

  val options = {
    val o = new Options()
    o.createIfMissing(true)
    o
  }

//  val db = factory.open(new File("pr-bot-storage"), options)

  val StorageFile = new File("pr-bot-storage")

  def get(key: String): Option[String] = {
    val db = factory.open(StorageFile, options)
    try {
      Option(db.get(bytes(key))).map(asString)
    } finally {
      db.close()
    }
  }

  def put(key: String, value: String): Unit = {
    val db = factory.open(StorageFile, options)
    try {
      db.put(bytes(key), bytes(value))
    } finally {
      db.close()
    }
  }

//  def close() = db.close()
}
