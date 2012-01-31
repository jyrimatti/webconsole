package net.lahteenmaki.webconsole

import scala.swing.Applet
import scala.swing.Applet

import scala.swing.Reactor
import scala.swing._
import scala.swing.Button
import scala.swing.event.ButtonClicked
import scala.swing.BoxPanel
import scala.swing._
import scala.tools.nsc._
import scala.tools.nsc.settings._
import scala.tools.nsc.interpreter._
import java.io._
import scala.swing.event._
import java.util.concurrent._
import java.awt.event.{ KeyEvent, KeyAdapter }
import java.security._
import java.net.URL
import java.io.InputStream
import java.net.URLDecoder.decode

import org.jruby.demo._
import org.jruby.Ruby
import org.jruby.RubyInstanceConfig
import org.jruby.internal.runtime.ValueAccessor

/**
 * Pretty much copied from org.jruby.demo.IRBConsole
 */
class JRubyWebConsole extends Applet {

	object ui extends UI with Reactor {
		def init() = {
			val pane = new TextArea
			contents = new ScrollPane { contents = pane }

			val ref = if (getDocumentBase.getRef != null) decode(getDocumentBase.getRef, "UTF-8") + "\n" else ""
	        val tar = new TextAreaReadline(pane.peer)
	
	        val config = new RubyInstanceConfig {
	            setInput(tar.getInputStream())
	            setOutput(new PrintStream(tar.getOutputStream))
	            setError(new PrintStream(tar.getOutputStream))
	            setArgv(Array())
	        }
	        val runtime = Ruby.newInstance(config)
	
	        runtime.getGlobalVariables.defineReadonly("$$", new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))))
	        runtime.getLoadService.init(new java.util.ArrayList)
	
	        tar.hookIntoRuntime(runtime)
	
	        Executors.newSingleThreadExecutor.submit(Executors.callable(new PrivilegedAction[Unit] {
	            def run {
	                runtime.evalScriptlet(
	                        "ARGV << '--readline' << '--prompt' << 'inf-ruby';"
	                        + "require 'irb'; require 'irb/completion';"
	                        + "IRB.start")
	            }
	        }))
		}
	}

}