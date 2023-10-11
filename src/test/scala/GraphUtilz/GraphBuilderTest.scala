package com.lsc
package GraphUtilz

import GraphUtilz.*

import NetGraphAlgebraDefs.*
import Randomizer.SupplierOfRandomness
import Utilz.ConfigReader.getConfigEntry
import Utilz.CreateLogger
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class GraphBuilderTest extends AnyFunSpec with Matchers {

  describe("GraphBuilder.loadGraph") {

      it("should return None when the graph doesn't exist") {
        val outGraphFileName = "non-existent-file.graph"
        val outputDirectory = "/local/directory/"

        val result = GraphBuilder.loadGraph(outGraphFileName, outputDirectory)

        result should be(None)
      }
    }
}
