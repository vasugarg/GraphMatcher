package com.lsc
package MapReduceTasks

import NetGraphAlgebraDefs.*
import Randomizer.SupplierOfRandomness
import Utilz.ConfigReader.getConfigEntry
import Utilz.CreateLogger
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import org.mockito.Mockito.{mock, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

class MapReduceNodesTest extends AnyFlatSpec with Matchers with MockitoSugar {

  it should "handle valid input with special characters in properties" in {
    val input = "NodeObject(0,3,11,1,92,1,5,2,0.6143455019720926)"
    val result = MapReduceNodes.retrieveNodeObjects(input)
    println(result)
    val expected = List(
      NodeObject(0,3,11,1,92,1,5,2,0.6143455019720926)
    )
    result should contain theSameElementsAs expected
  }

  it should "handle nodes with few common properties" in {
    val node = NodeObject(1, 3, 12, 1, 92, 1, 3, 2, 0.6143455019720926)
    val nodeList = List(
      NodeObject(2, 4, 12, 2, 93, 2, 6, 3, 0.7143455019720926),
      NodeObject(3, 2, 12, 1, 93, 1, 6, 3, 0.6143455019720926)
    )

    val result = MapReduceNodes.calculateNodeSimilarity(node, nodeList)

    val score1 = result.head._2 // Get the score of the first node
    val score2 = result(1)._2 // Get the score of the second node

    val condition: Boolean = score1 < score2
    condition shouldBe true

  }
}

