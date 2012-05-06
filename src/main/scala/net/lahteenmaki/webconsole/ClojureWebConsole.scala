package net.lahteenmaki.webconsole

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


class ClojureWebConsole extends Applet {
	object ui extends UI with Reactor {
		def init() = {
			System.err.println(getCodeBase())
			System.err.println(getClass().getClassLoader())
			System.err.println(getClass().getResourceAsStream("clojure/version.properties"))
			System.err.println("bar")
			Thread.currentThread().setContextClassLoader(classOf[clojure.lang.IMeta].getClassLoader())
			System.err.println(clojure.lang.RT.baseLoader())
			System.err.println(clojure.lang.Compiler.LOADER.isBound())
			clojure.main.main(Array())
		}
	}
}