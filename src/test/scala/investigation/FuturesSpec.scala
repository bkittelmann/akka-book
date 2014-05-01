package investigation

import org.scalatest.{WordSpec, BeforeAndAfterAll}
import org.scalatest.Matchers
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import scala.concurrent.{ExecutionContext, Future, Await}


class FuturesSpec extends WordSpec with Matchers {
  import scala.math.BigInt
  lazy val fibs: Stream[BigInt] = BigInt(0) #:: BigInt(1) #:: 
    fibs.zip(fibs.tail).map { n => n._1 + n._2 }

  def factorize(num: BigInt): Tuple2[BigInt, Seq[Int]] = {
    import math._
    (num, (1 to floor(sqrt(num.toDouble)).toInt) filter { i => num % i == 0})
  }

  import ExecutionContext.Implicits.global

  "Future" should {

    "be tested by using map() and flatmap()" ignore {
      // add some numbers
      val future1 = Future {
        (1 to 3).foldLeft(0) { (a, i) =>
          println("Future 1 - " + i)
          Thread.sleep(5)
          a + i
        }
      }

      // concatenate some chars
      val future2 = Future {
        ('A' to 'C').foldLeft("") { (a, c) =>
          println("Future 2 - " + c)
          Thread.sleep(5)
          a + c
        }
      }

      // combine them
      val result = future1 flatMap { numsum =>
        future2 map { string =>
          (numsum, string)
        }
      }

      // ..or write it as a for comprehension
      val result2 = for {
        numsum <- future1
        string <- future2
      } yield (numsum, string)

      Await.result(result, 1.second) should be (6, "ABC")
      Await.result(result2, 1.second) should be (6, "ABC")
    }

    "using recover but not matching exception throws it again" in {
      val oops = Future(5).filter { _ % 2 == 0 }.recover {
        case e: ArithmeticException => 5
      }
      evaluating {
        val result = Await.result(oops, 1.second)
      } should produce[NoSuchElementException]
    }

    "future.sequence matrices example" ignore {
      // mock matrix
      case class Matrix(rows: Int, columns: Int) {
        def mult(other: Matrix): Option[Matrix] = {
          if (columns == other.rows) {
            Some(Matrix(rows, other.columns))
          } else None
        }
      }

      def matrixMult(matrices: Seq[Matrix]): Option[Matrix] = {
        matrices.tail.foldLeft(Option(matrices.head)) { (acc, m) =>
          acc flatMap { a => a mult m }
        }
      }

      val randoms = (1 to 20000) map { _ =>
        scala.util.Random.nextInt(500)
      }

      val matrices = randoms zip randoms.tail map {
        case (rows, columns) => Matrix(rows, columns)
      }

      val futures = matrices.grouped(500).map { ms => 
        Future(matrixMult(ms))
      }.toSeq

      val multResultFuture = Future.sequence(futures) map { r =>
        // r will contain matrices as Option, hence the need to flatten
        matrixMult(r.flatten)
      }

      val finished = Await.result(multResultFuture, 1 second)

      finished should not be ('empty)
    }

    "use Future.traverse if you want to apply a mapping directly" in {
      val futures = (1 to 20) map { i => Future(i) }
      val sequenced = Future.sequence(futures)
      val seqSquared = sequenced map { seq =>
        seq map { i => i * i}
      }

      val trvSquared = Future.traverse(futures) { futurei =>
        futurei map { i => i * i }
      }

      val squaredFromSeq = Await.result(seqSquared, 1 second)
      val squaredFromTrv = Await.result(trvSquared, 1 second)

      squaredFromSeq should be (squaredFromTrv)
    }

    "use Future.firstCompletedOf to select first finished one" ignore {
      def longCalculation = Future {
        Thread.sleep(scala.util.Random.nextInt(60))
        "5 - from the calculation"
      }

      def cache = Future {
        Thread.sleep(scala.util.Random.nextInt(50))
        "5 - from the cache"
      }

      val futures = List(cache, longCalculation)
      val result = Future.firstCompletedOf(futures) onSuccess {
        case result => println(result)
      }
    }

    "use Future.fold to add in sequence" ignore {
      val words = Vector("Joker", "Batman", "Two Face", "Catwoman")

      val futures = words map { w =>
        Future {
          val sleepTime = scala.util.Random.nextInt(15)
          Thread.sleep(sleepTime)
          println(s"$w finished after $sleepTime milliseconds")
          w
        }
      }

      val sum = Future.fold(futures)("A") { (acc, word) => 
        acc + word.charAt(0)
      }

      println("Waiting for result")
      // first chars plus acc in sequence
      Await.result(sum, 1 second) should be ("AJBTC")
    }

    "use Future.reduce to create a word" ignore {
      val letters = Vector("B", "a", "t", "m", "a", "n")

      val futures = letters map { l => 
        Future {
          val sleepTime = scala.util.Random.nextInt(15)
          Thread.sleep(sleepTime)
          println(s"$l finished after $sleepTime milliseconds")
          l
        }
      }

      val wordFuture = Future.reduce(futures) { (word, letter) =>
        word + letter
      }

      println("Waiting for result")
      Await.result(wordFuture, 1 second) should be ("Batman")
    }

    "use Future.find to locate a value in the results" ignore {
      val letters = Vector("B", "a", "t", "m", "a", "n")
      val futures = letters map { l => 
        Future {
          val sleepTime = scala.util.Random.nextInt(15)
          Thread.sleep(sleepTime)
          l
        }
      }
      val foundFuture = Future.find(futures) { ltr => ltr.charAt(0) <= 'm' }
      val found = Await.result(foundFuture, 1 second)
      println("Found " + found)
    }

    "use Future.onSuccess for side-effects" ignore {
      Future { 13 } filter {
        _ % 2 == 0
      } fallbackTo Future {
        "That didn't work"
      } onSuccess {
        case i: Int => println("Disco!")
        case m => println(s"Boogers! $m")
      }
    }
  }
}