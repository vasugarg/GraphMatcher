package com.lsc

import java.io.*
import scala.util.*
import scala.util.matching.Regex
import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters.*
import org.json4s.*
import org.json4s.jackson.JsonMethods.*
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import _root_.com.google.common.graph.MutableValueGraph

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import GraphUtilz.*
import HelperUtilz.*

import NetGraphAlgebraDefs.*
import Utilz.CreateLogger

import scala.collection.mutable.{HashMap, HashSet}

object Main {
  val config: Config = ConfigFactory.load("application.conf")
  val logger: Logger = CreateLogger(classOf[Main.type])

  def main(args: Array[String]): Unit =
    val originalResult: Option[(NetGraph, List[NodeObject], List[Action])] =
      GraphBuilder.loadGraph(config.getString("NGSimulator.ngsCompareLocal.originalGraphFileName"),
        config.getString("NGSimulator.ngsCompareLocal.graphDirectory"))
    val originalSubgraphs: List[MutableValueGraph[NodeObject, Action]] = originalResult match {
      case Some((netGraph, nodes, actions)) =>
        logger.info("Original Graph found!")
        val graphSize = netGraph.sm.nodes().size()
        val subgraphSize = Math.sqrt(graphSize).toInt
        logger.info("Generating induces sugbraphs for the original graph")
        val G = subgraphGenerator(netGraph)
        G.generateSubGraphs(netGraph.initState, subgraphSize)
      case None =>
        logger.warn("Original Graph not found or is empty!")
        List.empty
    }
    val perturbedResult: Option[(NetGraph, List[NodeObject], List[Action])] =
      GraphBuilder.loadGraph(config.getString("NGSimulator.ngsCompareLocal.perturbedGraphFileName"),
        config.getString("NGSimulator.ngsCompareLocal.graphDirectory"))
    val perturbedSubgraphs: List[MutableValueGraph[NodeObject, Action]] = perturbedResult match {
      case Some((netGraph, nodes, actions)) =>
        logger.info("Perturbed Graph found!")
        val graphSize = netGraph.sm.nodes().size()
        val subgraphSize = Math.sqrt(graphSize).toInt
        logger.info("Generating induces sugbraphs for the perturbed graph")
        val G = subgraphGenerator(netGraph)
        G.generateSubGraphs(netGraph.initState, subgraphSize)
      case None =>
        logger.warn("Perturbed Graph not found or is empty!")
        List.empty
    }
    val edgeWriter = edgeFileWriter(filepath = config.getString("NGSimulator.ngsCompareLocal.edgeShardsPath"), originalSubgraphs, perturbedSubgraphs)
    edgeWriter.writer()
    val nodeWriter = nodeFileWriter(filepath = config.getString("NGSimulator.ngsCompareLocal.nodeShardsPath"), originalSubgraphs, perturbedSubgraphs)
    nodeWriter.writer()
    logger.info("Created the required shards, starting the mapReduce jobs")
    val JobScheduler = new JobScheduler(logger)
    val task1 = JobScheduler.schedlueJob1()
    val task2 = JobScheduler.schedlueJob2()

    val yamlFilePath = config.getString("NGSimulator.ngsCompareLocal.yamlFilePath")
    val yamlParser = new YamlParser(yamlFilePath)
    val (nodesYAML, edgesYAML) = yamlParser.parseYaml()
    logger.info("Successfully Parsed the YAML file")
    logger.info("Started parsing the output result for Edges:")
    val edgesResult = TextParser(filePath = config.getString("NGSimulator.ngsCompareLocal.mapReduceOutputDir") + config.getString("NGSimulator.ngsCompareLocal.task3Name") + "/" + "part-00000")
    val OutputEdgeResults = edgesResult.parse()
    logger.info("Started parsing the output result for Nodes:")
    val nodeResult = TextParser(filePath = config.getString("NGSimulator.ngsCompareLocal.mapReduceOutputDir") + config.getString("NGSimulator.ngsCompareLocal.task4Name") + "/" + "part-00000")
    val OutputNodeResults = nodeResult.parse()
    logger.info("Calculating the Results........")
    Results.calculateScores(OutputNodeResults, OutputEdgeResults, nodesYAML, edgesYAML)
}