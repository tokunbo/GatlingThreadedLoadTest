package experimental

import io.gatling.core.Predef._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ThreadAction(genericReporter:GenericReporter, simulation:Simulation) {

  def threadAction(requestName:String, worker:(Session=>Any)) = {
    exec(session => {
      var status:String = null
      var responseCode:String = null
      val startTime = java.lang.System.currentTimeMillis
      session.set("future", Future {
          blocking { 
            worker(session
            .set("requestName", requestName)
            .set("simulation", simulation))
          }
        } andThen {
          case Success(message) => {
            status = "OK"
            responseCode = "200"
          }
          case Failure(message) => {
            status = "KO"
            responseCode = "500"
          }
        } andThen {
          case(message) => genericReporter.logResponse(
                             session, requestName, startTime, 
                             java.lang.System.currentTimeMillis,
                             status, responseCode, message.toString)
      }) 
    })
    .asLongAs(session => !session("future").as[Future[Any]].isCompleted) {
      pause(250 milliseconds)
    }
    .exec(session => {
      val future = session("future").as[Future[Any]]
      var futureReturnValue:Any = null
      var updatedSession:Session = session 
      if(future.value.get.isFailure) {
        updatedSession = updatedSession.markAsFailed
        futureReturnValue = future.value.get.asInstanceOf[Failure[Any]]
      } else {
        futureReturnValue = future.value.get.asInstanceOf[Success[Any]].get
      }
      updatedSession.set("threadResult", futureReturnValue)
    })
  }
}
