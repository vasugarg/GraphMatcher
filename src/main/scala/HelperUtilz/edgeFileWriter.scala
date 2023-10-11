package com.lsc
package HelperUtilz

import _root_.com.google.common.graph.MutableValueGraph
import NetGraphAlgebraDefs.{Action, NodeObject}
import Utilz.CreateLogger
import org.slf4j.Logger
import scala.io.{BufferedSource, Source}
import java.io.{BufferedWriter, File, FileWriter}
import scala.collection.immutable.List
import scala.jdk.CollectionConverters.*
import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.core.sync.RequestBody
import java.net.URI

class edgeFileWriter(filepath: String, originalSubgraphs: List[MutableValueGraph[NodeObject, Action]],
                     perturbedSubgraphs: List[MutableValueGraph[NodeObject, Action]]) {
  val config: Config = ConfigFactory.load("application.conf")
  val logger: Logger = CreateLogger(this.getClass)

  def writer(): Unit = {
    if (filepath.startsWith("s3:")) {
      // Use S3-specific code for writing
      val s3UriParts = filepath.stripPrefix("s3://").split("/")
      val bucket = s3UriParts.head
      val objectKey = s3UriParts.tail.mkString("/") + config.getString("NGSimulator.ngsCompare.edgeShardsfileName")
      val awsRegion = Region.US_EAST_1 // Replace with your AWS region
      val s3 = S3Client.builder().region(awsRegion).build()

      try {
        logger.info("Creating shards for edges. Each shard will have a list of edges from the Original and Perturbed Graph")

        val content = originalSubgraphs.flatMap { s1 =>
          val s1Action = s1.edges().asScala.flatMap { edge =>
            val edgeValueOptional = s1.edgeValue(edge.nodeU(), edge.nodeV())
            if (edgeValueOptional.isPresent) {
              Some(edgeValueOptional.get())
            } else {
              None
            }.collect {
              case action: Action => action
            }.toSet
          }
          perturbedSubgraphs.flatMap { s2 =>
            val s2Action = s2.edges().asScala.flatMap { edge =>
              val edgeValueOptional = s2.edgeValue(edge.nodeU(), edge.nodeV())
              if (edgeValueOptional.isPresent) {
                Some(edgeValueOptional.get())
              } else {
                None
              }.collect {
                case action: Action => action
              }.toSet
            }
            if (s1Action.nonEmpty && s2Action.nonEmpty) {
              Some(s"[${s1Action.mkString(", ")}]; [${s2Action.mkString(", ")}]\n")
            } else {
              None
            }
          }
        }.mkString

        val bucketName = "cs-441" // Replace with your S3 bucket name
        val key = "HW1/ngscompare/shards/edgeShards.txt"
        val request = PutObjectRequest.builder().bucket(bucketName).key(key).build()
        val requestBody = RequestBody.fromString(content)
        s3.putObject(request, requestBody)

        logger.info(s"Created the sharded file for edges in S3 at s3://$bucket/$objectKey")
      } catch {
        case e: Exception =>
          logger.error("An error occurred while writing to S3.", e)
          throw e
      }
    } else {
      // Use local file writing code
      val file = new File(filepath)
      val writer = new BufferedWriter(new FileWriter(file))

      try {
        logger.info("Creating shards for edges. Each shard will have a list of edges from the Original and Perturbed Graph")

        originalSubgraphs.foreach { s1 =>
          val s1Action = s1.edges().asScala.flatMap { edge =>
            val edgeValueOptional = s1.edgeValue(edge.nodeU(), edge.nodeV())
            if (edgeValueOptional.isPresent) {
              Some(edgeValueOptional.get())
            } else {
              None
            }.collect {
              case action: Action => action
            }.toSet
          }

          perturbedSubgraphs.foreach { s2 =>
            val s2Action = s2.edges().asScala.flatMap { edge =>
              val edgeValueOptional = s2.edgeValue(edge.nodeU(), edge.nodeV())
              if (edgeValueOptional.isPresent) {
                Some(edgeValueOptional.get())
              } else {
                None
              }.collect {
                case action: Action => action
              }.toSet
            }

            if (s1Action.nonEmpty && s2Action.nonEmpty) {
              val pair = s"[${s1Action.mkString(", ")}]; [${s2Action.mkString(", ")}]"
              writer.write(pair)
              writer.newLine()
            }
          }
        }
      } finally {
        writer.close()
        logger.info(s"Created the sharded file for edges at: $filepath")
      }
    }
  }
}
