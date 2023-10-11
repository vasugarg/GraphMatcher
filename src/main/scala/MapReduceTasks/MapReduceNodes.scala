package com.lsc
package MapReduceTasks

import NetGraphAlgebraDefs.*
import Utilz.CreateLogger
import org.apache.hadoop.conf.*
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.*
import org.apache.hadoop.mapred.*
import org.apache.hadoop.util.*
import org.json4s.*
import org.json4s.JsonDSL.*
import org.json4s.jackson.JsonMethods.*
import org.slf4j.Logger

import java.io.IOException
import java.util
import scala.collection.immutable.{Map, Set}
import scala.collection.mutable.*
import scala.jdk.CollectionConverters.*
import scala.util.*
import scala.util.matching.Regex

object MapReduceNodes {

  val logger: Logger = CreateLogger(this.getClass)

  def retrieveNodeObjects(part: String): List[NodeObject] = {
    val nodeObjectPattern: Regex = """NodeObject\((\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+\.\d+)\)""".r
    val nodeObjectMatches = nodeObjectPattern.findAllMatchIn(part)
    val nodeObjects = nodeObjectMatches.map { matchResult =>
      val id = matchResult.group(1).toInt
      val children = matchResult.group(2).toInt
      val props = matchResult.group(3).toInt
      val currentDepth = matchResult.group(4).toInt
      val propValueRange = matchResult.group(5).toInt
      val maxDepth = matchResult.group(6).toInt
      val maxBranchingFactor = matchResult.group(7).toInt
      val maxProperties = matchResult.group(8).toInt
      val storedValue = matchResult.group(9).toDouble
      NodeObject(id = id, children = children, props = props, currentDepth = currentDepth, propValueRange = propValueRange, maxDepth = maxDepth,
        maxBranchingFactor = maxBranchingFactor, maxProperties = maxProperties, storedValue = storedValue)
    }.toList
    nodeObjects
  }

  def calculateNodeSimilarity(node: NodeObject, G: List[NodeObject]): List[(NodeObject, Double)] = {
    val childrenWeight = 10.0
    val propsWeight = 2.0
    val currentDepthWeight = 2.0
    val propValueRangeWeight = 2.0
    val maxDepthWeight = 2.0
    val maxBranchingFactorWeight = 2.0
    val maxPropertiesWeight = 5.0
    val maxStoredValueWeight = 10.0

    val setOriginalProps = Set(node.children, node.props, node.currentDepth, node.propValueRange,
      node.maxDepth, node.maxBranchingFactor, node.maxProperties, node.storedValue)

    G.map { nodeP =>
      val setPerturbedProps = Set(nodeP.children, nodeP.props, nodeP.currentDepth, nodeP.propValueRange,
        nodeP.maxDepth, nodeP.maxBranchingFactor, nodeP.maxProperties, nodeP.storedValue)
      val intersectionSize = setOriginalProps.intersect(setPerturbedProps).size.toDouble
      val unionSize = setOriginalProps.union(setPerturbedProps).size.toDouble
      val score = if (unionSize == 0.0) 0.0 else intersectionSize / unionSize

      (nodeP, (score * 1000).round / 1000.toDouble)
    }
  }

  class Mapper1 extends MapReduceBase with Mapper[LongWritable, Text, Text, Text]:

    @throws[IOException]
    override def map(key: LongWritable, value: Text, output: OutputCollector[Text, Text], reporter: Reporter): Unit =
      val line: String = value.toString
      val pairs = line.split(";")

      val nodeObjectsG1 = retrieveNodeObjects(pairs(0))
      val nodeObjectsG2 = retrieveNodeObjects(pairs(1))
      //implicit val formats: DefaultFormats.type = DefaultFormats
      nodeObjectsG1.foreach(node =>
        val tupleList = calculateNodeSimilarity(node, nodeObjectsG2)
        val jsonList = tupleList.map { case (nodeObject, intValue) =>
          val nodeString = nodeObject.id.toString
          ("key" -> nodeString) ~ ("value" -> intValue)
        }
        val jsonString = compact(render(jsonList))
        val jsonText = new Text()
        jsonText.set(jsonString)

        output.collect(new Text(node.id.toString), jsonText)
      )

  class Reducer1 extends MapReduceBase with Reducer[Text, Text, Text, Text]:
    override def reduce(key: Text,
                        values: util.Iterator[Text],
                        output: OutputCollector[Text, Text],
                        reporter: Reporter): Unit =

      val valueList = ListBuffer[String]()

      def valuesNext(iterator: util.Iterator[Text]): String = {
        if (iterator.hasNext) {
          val textValue = iterator.next().toString
          valueList += textValue.stripPrefix("[").stripSuffix("]")
          valuesNext(iterator)
        }
        val aggregatedValue = valueList.mkString(",")
        aggregatedValue
      }

      output.collect(key, new Text(valuesNext(values)))

  class Mapper2 extends MapReduceBase with Mapper[LongWritable, Text, Text, Text]:

    @throws[IOException]
    override def map(key: LongWritable, value: Text, output: OutputCollector[Text, Text], reporter: Reporter): Unit =

      val line: String = value.toString
      val pairs = line.split("\t")
      val origKey = pairs(0)
      val matched: String = "matched"
      val modified: String = "modified"
      val removed: String = "removed"
      val matchedPerturbed: String = "matchedPerturbed"
      val minScorePerturbed: String = "minScorePerturbed"

      implicit val formats: DefaultFormats.type = DefaultFormats
      val jsonList: List[Map[String, Any]] = parse(s"[${pairs(1)}]").extract[List[Map[String, Any]]]
      //println(jsonList)
      val mapWithMaxValue = jsonList.maxBy(_("value").asInstanceOf[Double])
      val maxScore = mapWithMaxValue("value").asInstanceOf[Double]
      val keyWithMaxValue = mapWithMaxValue("key").asInstanceOf[String]
      val mapWithMinValue = jsonList.minBy(_("value").asInstanceOf[Double])
      val keyWithMinValue = mapWithMinValue("key").asInstanceOf[String]
      output.collect(new Text(minScorePerturbed), new Text(keyWithMinValue))

      if (maxScore > 0.9) {
        output.collect(new Text(matched), new Text(origKey))
        output.collect(new Text(matchedPerturbed), new Text(keyWithMaxValue))
      } else if (maxScore > 0.7 && maxScore <= 0.9) {
        output.collect(new Text(modified), new Text(origKey))
        output.collect(new Text(matchedPerturbed), new Text(keyWithMaxValue))
      } else {
        output.collect(new Text(removed), new Text(origKey))
      }

  class Reducer2 extends MapReduceBase with Reducer[Text, Text, Text, Text]:
    override def reduce(key: Text,
                        values: util.Iterator[Text],
                        output: OutputCollector[Text, Text],
                        reporter: Reporter): Unit =

      val valueList = ListBuffer[String]()

      def valuesNext(iterator: util.Iterator[Text]): String = {
        if (iterator.hasNext) {
          val textValue = iterator.next().toString
          valueList += textValue
          valuesNext(iterator)
        }
        val aggregatedValue = valueList.mkString(",")
        aggregatedValue
      }

      output.collect(key, new Text(valuesNext(values)))
}
