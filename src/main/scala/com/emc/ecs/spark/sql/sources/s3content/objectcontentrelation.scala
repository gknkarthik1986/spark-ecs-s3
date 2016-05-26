package com.emc.ecs.spark.sql.sources.s3content
//blog.madhukaraphatak.com/extending-spark-api

import java.net.URI

import com.emc.`object`.s3.S3Config
import com.emc.`object`.s3.jersey.S3JerseyClient
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler
import org.apache.spark.TaskContext
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}

import com.emc.`object`.s3.bean.GetObjectResult
import com.emc.`object`.s3.request.{GetObjectRequest, QueryObjectsRequest}
/**
 * Created by conerj on 5/25/16.
 */


trait S3ClientWallet {
  def endpointUri: URI
  def credential: (String, String)
  def bucketName: String

  protected[this] lazy val s3Config = new S3Config(endpointUri)
    .withUseVHost(false)
    .withIdentity(credential._1).withSecretKey(credential._2)
  protected[this] lazy val s3Client = new S3JerseyClient(s3Config, new URLConnectionClientHandler())
}

class S3ClientWalletTraitor (val credential: (String,String),
                             val endpointUri: URI,
                             val bucketName: String) extends S3ClientWallet{

}

class S3ClientWalletImpl (endpt: String, cred: (String, String), bn: String){
  def endpointUri: URI = {URI.create(endpt)}
  def credential: (String, String) = cred
  def bucketName: String = bn

  def  s3Config: S3Config = { new S3Config(URI.create(endpt))
    .withUseVHost(false).withIdentity(cred._1)
    .withSecretKey(cred._2)}

    //protected[this] lazy val s3Client = new S3JerseyClient(s3Config, new URLConnectionClientHandler())
  var s3Client = new S3JerseyClient(s3Config, new URLConnectionClientHandler())

}




class ObjectContentRDD(prev:RDD[String], val credential: (String,String),
                       val endpointUri: URI,
                       val bucketName: String)
  extends RDD[String](prev)  with S3ClientWallet with Logging {

  override def getPartitions: Array[Partition] = firstParent[String].partitions

  override def compute(split: Partition, context: TaskContext):
  Iterator[String] = {
    firstParent[String].iterator(split, context).map(objKey => {
      log.info(s"Getting object content for: " + objKey)
      var gor = new GetObjectRequest(bucketName, objKey)
      val getObjResponse: GetObjectResult[String] = s3Client.getObject(gor,classOf[String])
      log.info(s"Got object content for: " + objKey)
      log.info(s"Content: " + getObjResponse.getObject())
      getObjResponse.getObject()
    })
  }



}