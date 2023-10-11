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
}

