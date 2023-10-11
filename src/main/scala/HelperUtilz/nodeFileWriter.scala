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

class nodeFileWriter(filepath: String, originalSubgraphs: List[MutableValueGraph[NodeObject, Action]],
                     perturbedSubgraphs: List[MutableValueGraph[NodeObject, Action]]) {
  val config: Config = ConfigFactory.load("application.conf")
  val logger: Logger = CreateLogger(this.getClass)

  def writer(): Unit = {
    if (filepath.startsWith("s3:")) {
      // Use S3-specific code for writing
      val s3UriParts = filepath.stripPrefix("s3://").split("/")
      val bucket = s3UriParts.head
      val awsRegion = Region.US_EAST_1 // Replace with your AWS region
      val s3 = S3Client.builder().region(awsRegion).build()
      val bucketName = "cs-441" // Replace with your S3 bucket name
      val key = "HW1/ngscompare/shards/nodeShards.txt"

      try {
        logger.info("Creating shards for nodes. Each shard will have a list of nodes from the Original and Perturbed Graph")

        val content = originalSubgraphs.flatMap { s1 =>
          perturbedSubgraphs.map { s2 =>
            s"${s1.nodes()}; ${s2.nodes()}\n"
          }
        }.mkString

        // Upload the content to the S3 bucket
        val request = PutObjectRequest.builder().bucket(bucketName).key(key).build()
        val requestBody = RequestBody.fromString(content)
        s3.putObject(request, requestBody)

        logger.info(s"Created the sharded file for nodes in S3 at s3://$bucket/$key")
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
        logger.info("Creating shards for nodes. Each shard will have a list of nodes from the Original and Perturbed Graph")

        originalSubgraphs.foreach { s1 =>
          perturbedSubgraphs.foreach { s2 =>
            val pair = s"${s1.nodes()}; ${s2.nodes()}"
            writer.write(pair)
            writer.newLine()
          }
        }
      } finally {
        writer.close()
        logger.info(s"Created the sharded file for nodes at: $filepath")
      }
    }
  }
}
