package experimental

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import javax.script.ScriptEngineManager  
import java.lang.System
import io.gatling.core.config.GatlingConfiguration
import akka.actor.ActorSystem
import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import scala.collection.mutable.Map
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.scenario.Simulation
import io.gatling.core.stats.writer.RunMessage
import io.gatling.commons.stats.Status
import io.gatling.core.stats.message.ResponseTimings 
import io.gatling.core.session.Session

class GenericReporter(simulation:Simulation,defaultSimId:String="ThreadName",
               runDescription:String="ThreadActions") {
  var mySimulation:Simulation = simulation
  var myDefaultSimId = defaultSimId
  var myRunDescription = runDescription
  var actorSystem:ActorSystem = null
  var actorRefForUser:ActorRef = null
  var simulationParams:SimulationParams = null
  var dwse:DataWritersStatsEngine = null
  var reportFolderName:String = null

  //Don't call start until after Simulation class's setUp for scenarios 
  object start extends StrictLogging {     
    println("INFO: Attempting to start instance of DataWritersStatsEngine...")

    val gconfig  = GatlingConfiguration.load(Map[String, AnyRef]())
    actorSystem = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())
    
    val runMessage = new RunMessage(
      simulation.getClass().getName(), 
      Some(simulation.getClass().getName()+"-customReporter"), 
      myDefaultSimId,
      java.lang.System.currentTimeMillis(),
      myRunDescription)

    //BEGIN MAGIC
    val jyeng = (new ScriptEngineManager()).getEngineByName("jython")
    val bindings = jyeng.createBindings()
    bindings.put("simulation", mySimulation)
    bindings.put("gconfig", gconfig)
    bindings.put("actorSystem", actorSystem)
    jyeng.eval("simparams = simulation.params(gconfig)", bindings)
    jyeng.eval("actorRef = actorSystem.actorFor('user')", bindings)
    simulationParams = bindings.get("simparams").asInstanceOf[SimulationParams] 
    actorRefForUser = bindings.get("actorRef").asInstanceOf[ActorRef]
    //END MAGIC 

    dwse = DataWritersStatsEngine.apply(
      actorSystem, 
      simulationParams, 
      runMessage,
      gconfig)

    dwse.start()
    reportFolderName = runMessage.userDefinedSimulationId.get.toString
    reportFolderName += "-"+runMessage.start.toString
    println("INFO: DataWritersStatsEngine started")
  }


  /*NOTES: 
    - Just like JMeter with non-HTTP samplers, you should probably
    stick to string "200" for pass and "500" for failure for the responseCode.
    - Status string can only be "OK", for pass or "KO" for fail. Anything else
    and the Status class will throw an error.
  */
  def logResponse(session:Session, requestName:String, startTime:Long, 
           endTime:Long, status:String, responseCode:String, 
           message:String, extraInfo:List[Any] = Nil) = {

    //Because from jython, None becomes null, but it needs to be Nil  
    val myExtraInfo = {
      if(extraInfo == null) {
        Nil
      } else {
        extraInfo
      }
    }
    dwse.logResponse(session, requestName, ResponseTimings(startTime, endTime),
                     Status(status), Some(responseCode), Some(message),
                     myExtraInfo)
  }

  /*If you don't call this, the log file probably won't be written
    Also, Gatling will probably hang because actorSystem still running
  */
  def stop() = {
    println(">>>>>>> Shutting down Reporter and ActorSystem...")
    dwse.stop(actorRefForUser, Some(new Exception("WHY_DWSE_STOP_EXCEPTION")))
    Await.result(actorSystem.terminate, 5 seconds)
    println(">>>>>>> Custom Report data should be in folder: "+reportFolderName)
  }
}
