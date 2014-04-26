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

    "future.sequence matrices example" in {
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
  }
}