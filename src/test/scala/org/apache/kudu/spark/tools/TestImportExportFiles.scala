// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.kudu.spark.tools

import java.io.File

import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder
import org.apache.kudu.{Schema, Type}
import org.apache.kudu.client.CreateTableOptions
import org.apache.kudu.spark.kudu._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import org.spark_project.guava.collect.ImmutableList

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TestImportExportFiles  extends FunSuite with TestContext with  Matchers {

  private val TABLE_NAME: String = "TestImportExportFiles"
  private val TABLE_DATA_PATH: String = "src/test/resources/TestImportExportFiles.csv"

  test("Spark Import Export") {
    val schema: Schema = {
      val columns = ImmutableList.of(
        new ColumnSchemaBuilder("key", Type.STRING).key(true).build(),
        new ColumnSchemaBuilder("column1_i", Type.STRING).build(),
        new ColumnSchemaBuilder("column2_d", Type.STRING).nullable(true).build(),
        new ColumnSchemaBuilder("column3_s", Type.STRING).build(),
        new ColumnSchemaBuilder("column4_b", Type.STRING).build())
      new Schema(columns)
    }
    val tableOptions = new CreateTableOptions().setRangePartitionColumns(List("key").asJava)
      .setNumReplicas(1)
    kuduClient.createTable(TABLE_NAME, schema, tableOptions)

    val dataPath = new File(TABLE_DATA_PATH).getAbsolutePath

    ImportExportFiles.testMain(Array("--operation=import",
      "--format=csv",
      s"--master-addrs=${miniCluster.getMasterAddresses}",
      s"--path=$TABLE_DATA_PATH",
      s"--table-name=$TABLE_NAME",
      "--delimiter=,",
      "--header=true",
      "--inferschema=true"), ss)
    val rdd = kuduContext.kuduRDD(ss.sparkContext, TABLE_NAME, List("key"))
    assert(rdd.collect.length == 4)
    assertEquals(rdd.collect().mkString(","),"[1],[2],[3],[4]")
  }
}
