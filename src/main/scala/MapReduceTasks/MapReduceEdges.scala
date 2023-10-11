package com.lsc
package MapReduceTasks

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.*
import org.apache.hadoop.io.*
import org.apache.hadoop.util.*
import org.apache.hadoop.mapred.*

import scala.collection.immutable.{Map, Set}
import scala.collection.mutable._
import scala.jdk.CollectionConverters._
import scala.util._
import scala.util.matching.Regex
import java.io.IOException
import java.util

import NetGraphAlgebraDefs.{NodeObject, *}
import Utilz.CreateLogger

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import org.slf4j.Logger


object MapReduceEdges {
  val logger: Logger = CreateLogger(this.getClass)

  def retrieveNodeObjects(part: String): List[NodeObject] = {
    val nodeObjectPattern: Regex = """NodeObject\((\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+),(\d+\.\d+)\)""".r
    val nodeObjectMatches = nodeObjectPattern.findAllMatchIn(part)
    val nodeObjects = nodeObjectMatches.map { matchResult =>
      val id = matchResult.group(1).toInt
      val children = matchResult.group(2).toInt
      val props = matchResult.group(3).toInt
      val propValueRange = matchResult.group(4).toInt
      val maxDepth = matchResult.group(5).toInt
      val maxBranchingFactor = matchResult.group(6).toInt
      val maxProperties = matchResult.group(7).toInt
      val storedValue = matchResult.group(8).toDouble

      NodeObject(id = id, children = children, props = props, propValueRange = propValueRange, maxDepth = maxDepth,
        maxBranchingFactor = maxBranchingFactor, maxProperties = maxProperties, storedValue = storedValue)
    }.toList
    nodeObjects
  }
  class Mapper1 extends MapReduceBase with Mapper[LongWritable, Text, Text, Text]:

    @throws[IOException]
    override def map(key: LongWritable, value: Text, output: OutputCollector[Text, Text], reporter: Reporter): Unit =

      val line: String = value.toString
      val pairs = line.split(";")

      def retrieveActionObjects(part: String): List[Action] = {
        val ActionObjectPattern: Regex = """Action\((\d+),(NodeObject\([^)]+\)),(NodeObject\([^)]+\)),(\d+),(\d+),(None|Some\(\d+\)),([\d.]+(?:E[-+]?\d+)?)\)""".r
        val ActionObjectMatches = ActionObjectPattern.findAllMatchIn(part)
        val ActionObjects = ActionObjectMatches.map { matchResult =>
          val actionType = matchResult.group(1).toInt
          val NodeObjectSource = retrieveNodeObjects(matchResult.group(2))
          val NodeObjectDest = retrieveNodeObjects(matchResult.group(3))
          val fromId = matchResult.group(4).toInt
          val toId = matchResult.group(5).toInt
          val resultingValue = matchResult.group(6) match {
            case s if s.startsWith("Some(") && s.endsWith(")") =>
              try {
                Some(s.substring(5, s.length - 1).toInt)
              } catch {
                case _: NumberFormatException => None
              }
            case _ => None
          }
          val cost = matchResult.group(7).toDouble

          Action(actionType = actionType, fromNode = NodeObjectSource.last, toNode = NodeObjectDest.last, fromId = fromId, toId = toId,
            resultingValue = resultingValue, cost = cost)
        }.toList
        ActionObjects
      }

      def calculateActionSimilarity(action: Action, G: List[Action]): List[(Action, Double)] = {
        val setOriginalProps = Set(action.actionType, action.fromId, action.toId, action.resultingValue, action.cost)
        G.map { actionP =>
          val setPerturbedProps = Set(actionP.actionType, action.fromId, action.toId, actionP.resultingValue, actionP.cost)
          val intersectionSize = setOriginalProps.intersect(setPerturbedProps).size.toDouble
          val unionSize = setOriginalProps.union(setPerturbedProps).size.toDouble
          val score = if (unionSize == 0.0) 0.0 else intersectionSize / unionSize
          (actionP, (score * 1000).round / 1000.toDouble)
        }
      }

      val ActionObjectsG1 = retrieveActionObjects(pairs(0))
      val ActionObjectsG2 = retrieveActionObjects(pairs(1))
      ActionObjectsG1.foreach(action =>
        val tupleList = calculateActionSimilarity(action, ActionObjectsG2)
        val jsonList = tupleList.map { case (actionObject, intValue) =>
          val actionString = actionObject.fromNode.id.toString + ':' + actionObject.toNode.id.toString
          ("key" -> actionString) ~ ("value" -> intValue)
        }
        val jsonString = compact(render(jsonList))
        val jsonText = new Text()
        jsonText.set(jsonString)
        output.collect(new Text(action.fromNode.id.toString + ':' + action.toNode.id.toString), jsonText)
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
