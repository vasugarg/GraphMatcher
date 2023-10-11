package com.lsc

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.*
import org.apache.hadoop.io.*
import org.apache.hadoop.util.*
import org.apache.hadoop.mapred.*
import org.slf4j.Logger
import MapReduceTasks.{MapReduceEdges, MapReduceNodes}

class JobScheduler(logger: Logger):
  private val conf: Config          = ConfigFactory.load("application.conf")
  private val nodeInputFile: String     = conf.getString("NGSimulator.ngsCompareLocal.shardsDirectory") + conf.getString("NGSimulator.ngsCompareLocal.nodeShardsfileName")
  private val edgeInputFile: String     = conf.getString("NGSimulator.ngsCompareLocal.shardsDirectory") + conf.getString("NGSimulator.ngsCompareLocal.edgeShardsfileName")
  private val outputDir: String         = conf.getString("NGSimulator.ngsCompareLocal.mapReduceOutputDir")
  private val task1Name: String         = "JobEdges"
  private val task2Name: String         = "JobEdgesFinal"
  private val task3Name: String         = "JobNodes"
  private val task4Name: String         = "JobNodesFinal"


  /** Task 1: Enqueues the Job which finds the similarity score of edges between the Original and
   * the Perturbed Graph
   */
  def schedlueJob1(): Unit =
    logger.info(s"Schedule job for generating similarity scores for edges ${task1Name}")
    val confEdges: JobConf = new JobConf(this.getClass)
    confEdges.setJobName("EdgeComparison")
    confEdges.set("mapreduce.job.maps", "1")
    confEdges.set("mapreduce.job.reduces", "1")
    confEdges.setOutputKeyClass(classOf[Text])
    confEdges.setOutputValueClass(classOf[Text])
    confEdges.setMapperClass(classOf[MapReduceEdges.Mapper1])
    confEdges.setReducerClass(classOf[MapReduceEdges.Reducer1])
    confEdges.setInputFormat(classOf[TextInputFormat])
    confEdges.setOutputFormat(classOf[TextOutputFormat[Text, Text]])

    FileInputFormat.addInputPath(confEdges, new Path(edgeInputFile))
    FileOutputFormat.setOutputPath(confEdges, new Path(outputDir + task1Name))
    val runningJob = JobClient.runJob(confEdges)

    if (runningJob.isSuccessful()) {
      logger.info(s"Schedule job for compiling similarity scores for edges ${task2Name}")
      val confEdges2: JobConf = new JobConf(this.getClass)
      confEdges2.setJobName("EdgeResult")
      confEdges2.set("mapreduce.job.maps", "1")
      confEdges2.set("mapreduce.job.reduces", "1")
      confEdges2.setOutputKeyClass(classOf[Text])
      confEdges2.setOutputValueClass(classOf[Text])
      confEdges2.setMapperClass(classOf[MapReduceEdges.Mapper2])
      confEdges2.setReducerClass(classOf[MapReduceEdges.Reducer2])
      confEdges2.setInputFormat(classOf[TextInputFormat])
      FileInputFormat.setInputPaths(confEdges2, new Path(outputDir + task1Name))
      FileOutputFormat.setOutputPath(confEdges2, new Path(outputDir + task2Name))

      if (JobClient.runJob(confEdges2).isSuccessful()) {
        logger.info("Both jobs completed successfully.")
      } else {
        logger.error("Job2 failed.")
      }
    } else {
      logger.error("Job1 failed.")
    }

  /** Task 2: Enqueues the Job which finds the similarity score of edges between the Original and
   * the Perturbed Graph
   */
  def schedlueJob2(): Unit =
    logger.info(s"Schedule job for generating similarity scores for nodes ${task3Name}")
    val confNodes: JobConf = new JobConf(this.getClass)
    confNodes.setJobName("NodeComparison")
    confNodes.set("mapreduce.job.maps", "1")
    confNodes.set("mapreduce.job.reduces", "1")
    confNodes.setOutputKeyClass(classOf[Text])
    confNodes.setOutputValueClass(classOf[Text])
    confNodes.setMapperClass(classOf[MapReduceNodes.Mapper1])
    confNodes.setReducerClass(classOf[MapReduceNodes.Reducer1])
    confNodes.setInputFormat(classOf[TextInputFormat])
    confNodes.setOutputFormat(classOf[TextOutputFormat[Text, Text]])

    FileInputFormat.addInputPath(confNodes, new Path(nodeInputFile))
    FileOutputFormat.setOutputPath(confNodes, new Path(outputDir + task3Name))
    val runningJob = JobClient.runJob(confNodes)

    if (runningJob.isSuccessful()) {
      logger.info(s"Schedule job for compiling similarity scores for nodes ${task4Name}")
      val confNodes2: JobConf = new JobConf(this.getClass)
      confNodes2.setJobName("NodeResult")
      confNodes2.set("mapreduce.job.maps", "1")
      confNodes2.set("mapreduce.job.reduces", "1")
      confNodes2.setOutputKeyClass(classOf[Text])
      confNodes2.setOutputValueClass(classOf[Text])
      confNodes2.setMapperClass(classOf[MapReduceNodes.Mapper2])
      confNodes2.setReducerClass(classOf[MapReduceNodes.Reducer2])
      confNodes2.setInputFormat(classOf[TextInputFormat])
      FileInputFormat.setInputPaths(confNodes2, new Path(outputDir + task3Name))
      FileOutputFormat.setOutputPath(confNodes2, new Path(outputDir + task4Name))

      if (JobClient.runJob(confNodes2).isSuccessful()) {
        logger.info("Both jobs completed successfully.")
      } else {
        logger.error("Job2 failed.")
      }
    } else {
      logger.error("Job1 failed.")
    }

end JobScheduler
