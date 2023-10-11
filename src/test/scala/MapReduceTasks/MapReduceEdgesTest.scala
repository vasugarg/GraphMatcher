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

  "calculateActionSimilarity" should "return a list of action scores for a given action and a list of actions" in {
    val action = Action(3,NodeObject(4,1,0,1,37,0,1,11,0.6106211718868338),NodeObject(218,5,10,1,23,1,0,8,0.8836287535263367),0,7,None,0.9658401952926567)
    val actionList = List(
      Action(3,NodeObject(10,5,4,1,62,1,6,9,0.16823869789996215),NodeObject(5,2,2,1,9,3,3,0,0.9394583671960672),0,7,None,0.15537360692964874),
      Action(18,NodeObject(0,0,10,1,45,2,1,4,0.9652635676061158),NodeObject(4,1,0,1,37,0,1,11,0.6106211718868338),0,0,Some(12),0.5270488266131074),
      Action(15,NodeObject(32,4,4,1,28,3,4,19,0.9651301228875142),NodeObject(6,6,8,1,37,3,0,2,0.7451703675811245),88,97,None,0.09087667464854066),
      Action(14,NodeObject(0,0,10,1,45,2,1,4,0.9652635676061158),NodeObject(32,4,4,1,28,3,4,19,0.9651301228875142),0,71,None,0.5930816561207392),
    )

    val result = MapReduceEdges.calculateActionSimilarity(action, actionList)
    val scoreKey1 = result.head._2
    val maxScore = result.maxBy(_._2)._2

    maxScore shouldBe scoreKey1
  }
}

