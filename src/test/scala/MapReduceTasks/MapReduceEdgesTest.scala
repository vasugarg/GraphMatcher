package com.lsc
package MapReduceTasks

import NetGraphAlgebraDefs.*
import Randomizer.SupplierOfRandomness
import Utilz.ConfigReader.getConfigEntry
import Utilz.CreateLogger
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import com.lsc.MapReduceTasks.MapReduceEdges.Mapper1
import org.mockito.Mockito.{mock, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

class MapReduceEdgesTest extends AnyFlatSpec with Matchers with MockitoSugar {

  "retrieveActionObjects" should "parse valid input correctly" in {
    val input = "Action(1,NodeObject(0,3,11,1,92,1,5,2,0.6143455019720926),NodeObject(1,4,12,2,93,2,6,3,0.7143455019720926),3,4,Some(42),2.5)"
    val result = MapReduceEdges.retrieveActionObjects(input)

    val expectedAction = Action(
      actionType = 1,
      fromNode = NodeObject(0, 3, 11, 1, 92, 1, 5, 2, 0.6143455019720926),
      toNode = NodeObject(1, 4, 12, 2, 93, 2, 6, 3, 0.7143455019720926),
      fromId = 3,
      toId = 4,
      resultingValue = Some(42),
      cost = 2.5
    )

    result should contain theSameElementsAs List(expectedAction)
  }

  it should "handle invalid input gracefully" in {
    // Test with an invalid input that doesn't match the ActionObjectPattern
    val input = "InvalidInput"
    val result = MapReduceEdges.retrieveActionObjects(input)

    result should be(empty)
  }

}

