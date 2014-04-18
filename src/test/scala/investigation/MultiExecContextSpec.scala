package investigation

import org.scalatest.{WordSpec, BeforeAndAfterAll}
import org.scalatest.Matchers

class MultiExecContextSpec extends WordSpec with Matchers {
	import scala.math.BigInt
  lazy val fibs: Stream[BigInt] = BigInt(0) #:: BigInt(1) #:: 
    fibs.zip(fibs.tail).map { n => n._1 + n._2 }

    "Future" should {
      "calculate fibonacci numbers" in {
        import java.util.concurrent.Executors
        import scala.concurrent.duration._
        import scala.concurrent.{ExecutionContext, Future, Await}

        val execService = Executors.newCachedThreadPool()
        implicit val execContext = ExecutionContext.fromExecutorService(execService)

        val futureFib = Future { fibs.drop(99).head }

        val fib = Await.result(futureFib, 1 second)

        fib should be (BigInt("218922995834555169026"))

        execContext.shutdown()
      }
    }
}