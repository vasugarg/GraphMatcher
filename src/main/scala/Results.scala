package com.lsc

import scala.collection.mutable
import scala.collection.mutable.{HashMap, HashSet}
import HelperUtilz.*
import Utilz.CreateLogger
import org.slf4j.Logger

object Results {
  val logger: Logger = CreateLogger(classOf[Main.type])
  def calculateScores(OutputNodeResults: (Set[String], Set[String], Set[String], Set[String]),
                      OutputEdgeResults: (Set[String], Set[String], Set[String], Set[String]),
                      nodesYAML: Nodes,
                      edgesYAML: Edges
                     ): (Double, Double) = {

    def convertToHashMap(inputHashSet: Set[String]): HashMap[Int, Int] = {
      try {
        inputHashSet.foldLeft(HashMap.empty[Int, Int]) {
          case (map, element) =>
            val Array(key, value) = element.split(":")
            map + (key.toInt -> value.toInt)
        }
      } catch {
        case e: NumberFormatException =>
          logger.warn("NumberFormatException occurred while converting to HashMap.", e)
          HashMap.empty[Int, Int]
      }
    }

    logger.info("Calculating the results for the following edge categories:")
    val removedEdges = convertToHashMap(OutputEdgeResults._3)
    logger.info(s"Removed Edges: $removedEdges")
    val addedEdges = convertToHashMap(OutputEdgeResults._4)
    logger.info(s"Added Edges: $addedEdges")
    val modifiedEdges = convertToHashMap(OutputEdgeResults._2)
    logger.info(s"Modified Edges: $modifiedEdges")
    val matchedEdges = convertToHashMap(OutputEdgeResults._1)
    logger.info(s"Matched Edges: $matchedEdges")


    def calculateEdgeScore(edgePredicted: HashMap[Int, Int], edgesYAML: Map[Int, Int]): (Int, Int, Int) = {
      val (atl, wtl) = edgePredicted.foldLeft((0, 0)) {
        case ((atlAcc, wtlAcc), (source, target)) =>
          if (edgesYAML.contains(source) && edgesYAML(source) == target) {
            logger.info(s"Match found for target:$target and source:$source,incrementing the ATL")
            (atlAcc + 1, wtlAcc)
          } else {
            (atlAcc, wtlAcc + 1)
          }
      }

      val ctl = edgesYAML.foldLeft(0) {
        case (ctlAcc, (source, target)) =>
          if (!(edgePredicted.contains(source) && edgePredicted(source) == target)) {
            logger.info(s"No match for target:$target and source:$source,incrementing the CTL")
            ctlAcc + 1
          } else {
            ctlAcc
          }
      }
      (atl, ctl, wtl)
    }

    val (atlEdges, ctlEdges, wtlEdges) = calculateEdgeScore(removedEdges, edgesYAML.Removed) match {
      case (atl, ctl, wtl) =>
        val (atlModified, ctlModified, wtlModified) = calculateEdgeScore(modifiedEdges, edgesYAML.Modified)
        val (atlAdded, ctlAdded, wtlAdded) = calculateEdgeScore(addedEdges, edgesYAML.Added)
        (atl + atlModified + atlAdded, ctl + ctlModified + ctlAdded, wtl + wtlModified + wtlAdded)
    }
    logger.info(s"OutputNodes: $OutputNodeResults")

    logger.info("Calculating the results for the following node categories:")
    val addedNodesPred = OutputNodeResults._4.map(_.toInt)
    val removedNodesPred = OutputNodeResults._3.map(_.toInt)
    val modifiedNodesPred = OutputNodeResults._2.map(_.toInt)
    val matchedNodesPred = OutputNodeResults._1.map(_.toInt)


    def calculateNodeScore(nodePredicted: Set[Int], nodesYAML: HashSet[Int]): (Int, Int, Int) = {
      val (atl, wtl) = nodePredicted.foldLeft((0, 0)) {
        case ((atlAcc, wtlAcc), node) =>
          if (nodesYAML.contains(node)) {
            logger.info(s"Match found for node:$node,incrementing the ATL")
            (atlAcc + 1, wtlAcc)
          } else {
            (atlAcc, wtlAcc + 1)
          }
      }

      val ctl = nodesYAML.foldLeft(0) {
        case (ctlAcc, node) =>
          if (!nodePredicted.contains(node)) {
            logger.info(s"No match for target:$node ,incrementing the CTL")
            ctlAcc + 1
          } else {
            ctlAcc
          }
      }
      (atl, ctl, wtl)
    }
    logger.info("Completed the iteration over nodes and edges. Now calculating the accuracy/precision")

    val (atlNodes, ctlNodes, wtlNodes) = calculateNodeScore(removedNodesPred, mutable.HashSet(nodesYAML.Removed.keys.toSeq: _*)) match {
      case (atl, ctl, wtl) =>
        val (atlModified, ctlModified, wtlModified) = calculateNodeScore(modifiedNodesPred, mutable.HashSet(nodesYAML.Modified.keys.toSeq: _*))
        (atl + atlModified, ctl + ctlModified, wtl + wtlModified)
    }

    val dtl = 0
    val GTL = atlEdges + atlNodes + dtl
    val BTL = ctlEdges + ctlNodes + wtlEdges + wtlNodes
    val RTL = GTL + BTL
    val VPR: Double = ((GTL - BTL) / (2 * RTL)) + 0.5
    logger.info(s"VPR: $VPR")
    val ACC: Double = GTL / RTL
    logger.info(s"ACC: $ACC")
    (VPR, ACC)
  }
}