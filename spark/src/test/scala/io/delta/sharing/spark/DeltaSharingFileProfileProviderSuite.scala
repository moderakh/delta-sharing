package io.delta.sharing.spark

import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkFunSuite

class DeltaSharingFileProfileProviderSuite extends SparkFunSuite {

  def testProfile(profile: String, expected: DeltaSharingProfile): Unit = {
    val temp = Files.createTempFile("test", ".share").toFile
    try {
      FileUtils.writeStringToFile(temp, profile, "UTF-8")
      assert(new DeltaSharingFileProfileProvider(new Configuration, temp.getCanonicalPath)
        .getProfile == expected)
    } finally {
      temp.delete()
    }
  }

  test("parse") {
    testProfile(
      """{
        |  "version": 1,
        |  "endpoint": "foo",
        |  "bearerToken": "bar"
        |}
        |""".stripMargin,
      DeltaSharingProfile(version = Some(1), endpoint = "foo", bearerToken = "bar")
    )
  }

  test("version is missing") {
    val e = intercept[IllegalArgumentException] {
      testProfile(
        """{
          |  "endpoint": "foo",
          |  "bearerToken": "bar"
          |}
          |""".stripMargin,
        null
      )
    }
    assert(e.getMessage.contains("Cannot find the 'version' field in the profile file"))
  }

  test("version is not supported") {
    val e = intercept[IllegalArgumentException] {
      testProfile(
        """{
          |  "version": 100
          |}
          |""".stripMargin,
        null
      )
    }
    assert(e.getMessage.contains("The 'version' (100) in the profile is too new."))
  }

  test("endpoint is missing") {
    val e = intercept[IllegalArgumentException] {
      testProfile(
        """{
          |  "version": 1,
          |  "bearerToken": "bar"
          |}
          |""".stripMargin,
        null
      )
    }
    assert(e.getMessage.contains("Cannot find the 'endpoint' field in the profile file"))
  }

  test("bearerToken is missing") {
    val e = intercept[IllegalArgumentException] {
      testProfile(
        """{
          |  "version": 1,
          |  "endpoint": "foo"
          |}
          |""".stripMargin,
        null
      )
    }
    assert(e.getMessage.contains("Cannot find the 'bearerToken' field in the profile file"))
  }

  test("unknown field should be ignored") {
    testProfile(
      """{
        |  "version": 1,
        |  "endpoint": "foo",
        |  "bearerToken": "bar",
        |  "futureField": "xyz"
        |}
        |""".stripMargin,
      DeltaSharingProfile(version = Some(1), endpoint = "foo", bearerToken = "bar")
    )
  }
}
