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

  it should "handle valid input with special characters in properties" in {
    val input = "NodeObject(1, 2, 3, 4, 5, 6, 7, 1.23)"
    val result = MapReduceEdges.retrieveNodeObjects(input)
    val expected = List(
      NodeObject(1, 2, 3, 4, 5, 6, 7, 1, 1.23)
    )
    result should contain theSameElementsAs expected
  }

}
