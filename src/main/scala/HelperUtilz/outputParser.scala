package com.lsc
package HelperUtilz

import Utilz.CreateLogger
import org.slf4j.Logger
import scala.io.{BufferedSource, Source}
import java.io.FileNotFoundException
import org.apache.hadoop.fs.{FileSystem, Path, FSDataInputStream}
import org.apache.hadoop.conf.Configuration
import java.net.URI

class TextParser(filePath: String) {
  val logger: Logger = CreateLogger(this.getClass)

  def parse(): (Set[String], Set[String], Set[String], Set[String]) = {
    logger.info("Loading the file .........")

    if (filePath.startsWith("s3:")) {
      // Use S3-specific code for reading
      val hadoopConf = new Configuration()
      val s3Remote = FileSystem.get(new URI(filePath), hadoopConf)
      val lines: Option[BufferedSource] = None

      try {
        val inputStream: FSDataInputStream = s3Remote.open(new Path(filePath))
        val lines = Some(Source.fromInputStream(inputStream))
        logger.info("File Loaded")

        val (matchedPerturbed, minScorePerturbed, removed, matchedSet, modifiedSet): (Set[String], Set[String], Set[String], Set[String], Set[String]) =
          lines.foldLeft(
            (Set.empty[String], Set.empty[String], Set.empty[String], Set.empty[String], Set.empty[String])
          ) {
            case ((matchedP, minScoreP, removed, matchedN, modified), line) =>
              val parts = line.toString.split("\t")
              if (parts.length == 2) {
                val key = parts(0)
                val values = parts(1).split(",").toList

                key match {
                  case "matchedPerturbed" => (values.toSet, minScoreP, removed, matchedN, modified)
                  case "minScorePerturbed" => (matchedP, values.toSet, removed, matchedN, modified)
                  case "removed" => (matchedP, minScoreP, values.toSet, matchedN, modified)
                  case "matched" => (matchedP, minScoreP, removed, values.toSet, modified)
                  case "modified" => (matchedP, minScoreP, removed, matchedN, values.toSet)
                  case _ => (matchedP, minScoreP, removed, matchedN, modified)
                }
              } else {
                // Handle lines with incorrect format
                logger.warn(s"Skipping line with incorrect format: $line")
                (matchedP, minScoreP, removed, matchedN, modified)
              }
          }

        val addedSet = minScorePerturbed -- matchedPerturbed
        logger.info("Parsed the output file and generated the required Sets")
        (matchedSet, modifiedSet, removed, addedSet)
      } catch {
        case e: FileNotFoundException =>
          logger.error(s"File not found: $filePath")
          throw e // Re-throw the exception if needed
        case e: Exception =>
          logger.error("An error occurred while parsing the file.", e)
          throw e
      } finally {
        lines.foreach(_.close())
      }
    } else {
      // Use local file reading code
      var lines: Option[BufferedSource] = None

      try {
        lines = Some(Source.fromFile(filePath))
        logger.info("File Loaded")

        val (matchedPerturbed, minScorePerturbed, removed, matchedSet, modifiedSet) =
          lines.get.getLines().foldLeft(
            (Set.empty[String], Set.empty[String], Set.empty[String], Set.empty[String], Set.empty[String])
          ) {
            case ((matchedP, minScoreP, removed, matchedN, modified), line) =>
              val parts = line.split("\t")
              if (parts.length == 2) {
                val key = parts(0)
                val values = parts(1).split(",").toList

                key match {
                  case "matchedPerturbed" => (values.toSet, minScoreP, removed, matchedN, modified)
                  case "minScorePerturbed" => (matchedP, values.toSet, removed, matchedN, modified)
                  case "removed" => (matchedP, minScoreP, values.toSet, matchedN, modified)
                  case "matched" => (matchedP, minScoreP, removed, values.toSet, modified)
                  case "modified" => (matchedP, minScoreP, removed, matchedN, values.toSet)
                  case _ => (matchedP, minScoreP, removed, matchedN, modified)
                }
              } else {
                // Handle lines with incorrect format
                logger.warn(s"Skipping line with incorrect format: $line")
                (matchedP, minScoreP, removed, matchedN, modified)
              }
          }

        val addedSet = minScorePerturbed -- matchedPerturbed
        logger.info("Parsed the output file and generated the required Sets")
        (matchedSet, modifiedSet, removed, addedSet)
      } catch {
        case e: FileNotFoundException =>
          logger.error(s"File not found: $filePath")
          throw e // Re-throw the exception if needed
        case e: Exception =>
          logger.error("An error occurred while parsing the file.", e)
          throw e // Re-throw the exception if needed
      } finally {
        // Close the file if it was successfully opened
        lines.foreach(_.close())
      }
    }
  }
}
