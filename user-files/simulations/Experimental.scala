package experimental      
  
import javax.script.Compilable    
import javax.script.ScriptEngineManager  
import org.jruby.RubyObject       
import org.jruby.RubyProc
import org.python.core.PyObject   
import org.python.core.FunctionThread
import java.lang.System

class JythonUtils(jythonFile:String) {    
  private var fd = scala.io.Source.fromFile(jythonFile)   
  private var jycode = fd.mkString
  fd.close()      
  private var jyeng = (new ScriptEngineManager()).getEngineByName("jython")    
  val jyCompiledCode = jyeng.asInstanceOf[Compilable].compile(jycode)

  def getPyObject():PyObject = {
    val bindings = jyeng.createBindings
    jyCompiledCode.eval(bindings)
    bindings.get("main").asInstanceOf[PyObject]
  }
} 
  
class JRubyUtils(jrubyFile:String) {
  private var fd = scala.io.Source.fromFile(jrubyFile)   
  private var jrubycode = fd.mkString
  fd.close()      
  System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe")
  System.setProperty("org.jruby.embed.localvariable.behavior", "transient") 
  System.setProperty("org.jruby.embed.sharing.variables", "false")
  private var jrubyeng = (new ScriptEngineManager()).getEngineByName("jruby")    
  private var jrubyCompiledCode = jrubyeng.asInstanceOf[Compilable]
                                    .compile(jrubycode)     
  private var yaml = jrubyeng.eval("require 'yaml'; YAML")
                       .asInstanceOf[RubyObject]       
  var runtime = jrubyeng.eval("itself").asInstanceOf[RubyObject].getRuntime      

  def getRubyProc():RubyProc = {
    jrubyCompiledCode.eval().asInstanceOf[RubyProc]
  }

  def stringToYAML(string:String):Object = {    
    yaml.callMethod("load", runtime.newString(string))    
  }       
}

