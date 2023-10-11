package com.lsc
package GraphUtilz

import GraphUtilz.*
import NetGraphAlgebraDefs.*
import Randomizer.SupplierOfRandomness
import Utilz.ConfigReader.getConfigEntry
import Utilz.CreateLogger
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import org.mockito.Mockito.{mock, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class subgraphGeneratorTest extends AnyFlatSpec with Matchers with MockitoSugar {
  val logger: Logger = CreateLogger(this.getClass)
  logger.info(NetModelAlgebra.getFields.mkString(","))
  val node1: NodeObject = NodeObject(id = 1, children = 1, props = 1, propValueRange = 1, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node2: NodeObject = NodeObject(id = 2, children = 2, props = 2, propValueRange = 2, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node3: NodeObject = NodeObject(id = 3, children = 3, props = 3, propValueRange = 3, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node4: NodeObject = NodeObject(id = 4, children = 4, props = 4, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node5: NodeObject = NodeObject(id = 5, children = 5, props = 5, propValueRange = 5, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node6: NodeObject = NodeObject(id = 6, children = 6, props = 6, propValueRange = 6, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node7: NodeObject = NodeObject(id = 7, children = 7, props = 7, propValueRange = 7, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node8: NodeObject = NodeObject(id = 8, children = 8, props = 8, propValueRange = 8, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node9: NodeObject = NodeObject(id = 9, children = 8, props = 8, propValueRange = 9, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)
  val node10: NodeObject = NodeObject(id = 9, children = 8, props = 8, propValueRange = 9, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, 1)

  val edge12: Action = Action(actionType = 1, fromNode = node1, toNode = node2, fromId = 1, toId = 2, resultingValue = Some(1), cost = 0.1)
  val edge13: Action = Action(actionType = 2, fromNode = node1, toNode = node3, fromId = 1, toId = 3, resultingValue = Some(2), cost = 0.2)
  val edge14: Action = Action(actionType = 3, fromNode = node1, toNode = node4, fromId = 1, toId = 4, resultingValue = Some(3), cost = 0.3)
  val edge25: Action = Action(actionType = 4, fromNode = node2, toNode = node5, fromId = 2, toId = 5, resultingValue = Some(4), cost = 0.4)
  val edge26: Action = Action(actionType = 5, fromNode = node2, toNode = node6, fromId = 2, toId = 6, resultingValue = Some(5), cost = 0.5)
  val edge35: Action = Action(actionType = 6, fromNode = node3, toNode = node5, fromId = 3, toId = 5, resultingValue = Some(6), cost = 0.6)
  val edge37: Action = Action(actionType = 7, fromNode = node3, toNode = node7, fromId = 3, toId = 7, resultingValue = Some(7), cost = 0.7)
  val edge47: Action = Action(actionType = 8, fromNode = node4, toNode = node7, fromId = 4, toId = 7, resultingValue = Some(8), cost = 0.8)
  val edge58: Action = Action(actionType = 9, fromNode = node5, toNode = node8, fromId = 5, toId = 8, resultingValue = Some(9), cost = 0.9)
  val edge68: Action = Action(actionType = 10, fromNode = node6, toNode = node8, fromId = 6, toId = 8, resultingValue = Some(10), cost = 0.91)
  val edge78: Action = Action(actionType = 11, fromNode = node7, toNode = node8, fromId = 7, toId = 8, resultingValue = Some(11), cost = 0.92)
  val edge79: Action = Action(actionType = 12, fromNode = node7, toNode = node9, fromId = 7, toId = 9, resultingValue = Some(12), cost = 0.93)
  val edge82: Action = Action(actionType = 13, fromNode = node8, toNode = node2, fromId = 8, toId = 2, resultingValue = Some(13), cost = 0.94)
  val edge84: Action = Action(actionType = 14, fromNode = node8, toNode = node4, fromId = 8, toId = 4, resultingValue = Some(14), cost = 0.95)
  val edge89: Action = Action(actionType = 15, fromNode = node8, toNode = node9, fromId = 8, toId = 9, resultingValue = Some(15), cost = 0.96)

  def createTestGraph(): NetGraph = {
    val graph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    def addnode(node: NodeObject): Unit = {
      if !graph.addNode(node) then
        logger.error(s"Node $node already exists")
      else logger.debug(s"Added node $node")
    }
    def addedge(from: NodeObject, to: NodeObject, edge: Action): Unit = {
      Try(graph.putEdgeValue(from, to, edge)) match
        case Success(_) => logger.debug(s"Added edge $edge between nodes $from -> $to")
        case Failure(e) => logger.error(s"Edge $from -> $to cannot be added because $e")
    }
    addnode(node1)
    addnode(node2)
    addnode(node3)
    addnode(node4)
    addnode(node5)
    addnode(node6)
    addnode(node7)
    addnode(node8)
    addnode(node9)
    addnode(node10)

    addedge(node1, node2, edge12)
    addedge(node1, node3, edge13)
    addedge(node1, node4, edge14)
    addedge(node2, node5, edge25)
    addedge(node2, node6, edge26)
    addedge(node3, node5, edge35)
    addedge(node3, node7, edge37)
    addedge(node4, node7, edge47)
    addedge(node5, node8, edge58)
    addedge(node6, node8, edge68)
    addedge(node7, node8, edge78)
    addedge(node7, node9, edge79)
    addedge(node8, node2, edge82)
    addedge(node8, node4, edge84)
    addedge(node8, node9, edge89)
    NetGraph(graph, node1)
  }

  it should "return a list of induced subgraphs" in {
    // Create a sample graph and add nodes and edges
    // Replace the sampleGraph and node setup with your actual test data
    val graph = createTestGraph()
    val sg = new subgraphGenerator(graph)
    val subgraphs = sg.generateSubGraphs(graph.initState, 2)

    // Assert that the result is not empty and contains induced subgraphs
    subgraphs should not be empty
    subgraphs.foreach { inducedGraph =>
      inducedGraph.nodes().asScala should not be empty
    }
  }
}


