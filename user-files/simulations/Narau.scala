import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import experimental.GenericReporter
import experimental.ThreadAction
import experimental.JythonUtils
import experimental.JRubyUtils
import org.jruby.RubyProc
import org.python.core.PyObject   
import io.gatling.core.structure.ChainBuilder

class Narau extends Simulation {
  val vuserCount = 10 
  val q = new java.util.concurrent.LinkedBlockingQueue[Int]
  var pyobject:PyObject = null 
  var jrubyutils:JRubyUtils = null
  var jrubyproc:RubyProc = null
  val genericReporter = new GenericReporter(this)
  val threadAction:((String,(Session=>Any))=>ChainBuilder) = 
    (new ThreadAction(genericReporter,this)).threadAction 

  val httpConf = http     
    .baseURL("http://forum.sanriotown.com/")    
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")      
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")       
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0") 

  before {
    Future {
      blocking {
        while(true) {
          Thread.sleep(19000)
          q.put(scala.util.Random.nextInt(10)+1)
        }
      }
    }
    println("INFO: Creating Jython & JRuby Engines...")
    pyobject = (new JythonUtils("gatling_narau.py")).getPyObject 
    jrubyutils = new JRubyUtils("gatling_narau.rb")
    jrubyproc = jrubyutils.getRubyProc
    println("INFO: Engine Creations done") 
    genericReporter.start
  }

  after {
    println("All Scenarios done!")
    genericReporter.stop
  }

  def example_worker(session:Session):Any = {
    val sleepTime = session("diceRoll").as[Int] 
    Thread.sleep(sleepTime)
    if(sleepTime % 3 == 0) {
      throw new Throwable("Hot Potatoe!")
    }
    s"I did what I wanted as ${session.userId}"
  }

  def jyRequest(session:Session):Any = {
    pyobject._jcall(Array(session))
  }

  def jrubyRequest(session:Session):Any = {
    jrubyproc.call(
      jrubyutils.runtime.getCurrentContext,
      Array[org.jruby.runtime.builtin.IRubyObject](
        org.jruby.javasupport.JavaUtil.convertJavaToRuby(
          jrubyutils.runtime, session)))
  }

  def printStatus(session:Session):Session = {
    if(session.isFailed) {
      print("F")
    } else {
      print(".")
    }
    session
  }

  var scn1 = scenario("MyExampleScenario")
             .exec(session => {
               session.set("diceRoll",(scala.util.Random.nextInt(30)+1)*1000)
             })
             .exec(threadAction("example_1", example_worker))
             .exec(session => printStatus(session)) 

  var scn2 = scenario("MyJythonScenario")
             .exec(http("jyGet HK Forum Homepage")
               .get("/")
               .check(bodyString.saveAs("respdata")))
             .exec(threadAction("jy_request_1", jyRequest))
             .exec(session => printStatus(session)) 

  var scn3 = scenario("MyJRubyScenario") 
             .exec(http("jrubyGet HK Forum Homepage")
               .get("/")
               .check(bodyString.saveAs("respdata")))
             .exec(threadAction("jruby_request_1", jrubyRequest))
             .exec(session => printStatus(session)) 

  setUp(
    scn1.inject(constantUsersPerSec(20) during(59 seconds)),
    scn2.inject(rampUsers(vuserCount) over(60 seconds)),
    scn3.inject(rampUsers(vuserCount) over(60 seconds))
  ).protocols(httpConf)
}

