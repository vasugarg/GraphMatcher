package com.lsc
package GraphUtilz

import NetGraphAlgebraDefs.*
import Utilz.CreateLogger
import com.google.common.graph.*
import java.io.File
import org.slf4j.Logger

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.jdk.CollectionConverters._

object GraphBuilder {
  val logger: Logger = CreateLogger(this.getClass)

  def loadGraph(outGraphFileName: String, outputDirectory: String): Option[(NetGraph, List[NodeObject], List[Action])] = {

    val graph: Option[NetGraph] = if (outputDirectory.startsWith("s3")) {
      logger.info(s"Seems like the path is remote i.e. S3 bucket, loading it up.")
      NetGraph.load(outGraphFileName, outputDirectory, true, true)
    } else {
      logger.warn(s"File $outputDirectory$outGraphFileName is located, loading it up.")
      NetGraph.load(fileName = s"$outGraphFileName", dir = s"$outputDirectory", false, false)
    }
    graph match {
      case Some(netGraph) =>
        val nodes: List[NodeObject] = netGraph.sm.nodes.asScala.toList
        val edges: List[Action] = netGraph.sm.edges().asScala.toList.map { edge =>
          netGraph.sm.edgeValue(edge.source(), edge.target()).get
        }.sortBy(_.fromNode.id)
        Some((netGraph, nodes, edges)) // Return Some(valueGraph) when a NetGraph is present
      case None =>
        // Handle the case when 'graph' is None, e.g., by returning None
        None
    }
  }
}





